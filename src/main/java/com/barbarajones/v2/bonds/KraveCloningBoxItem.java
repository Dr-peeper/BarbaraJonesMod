package com.barbarajones.v2.bonds;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * CAYDEN BREEDING, or rather the reason he does not need one: this is what
 * happens when the same box of Krave has been refilled and handed to him one
 * too many times.
 *
 * <p>The joke is the mechanic. {@code CaydenCobb.getBreedOffspring()} returns
 * {@code null} on purpose - he cannot be bred the normal Minecraft way because
 * there is no version of "two Caydens make a third" that is funnier than "one
 * Cayden, fed enough Krave Cloning Boxes, produces a second Cayden by pure
 * caloric accident." Right-click any Cayden (canonical or already-bred - a
 * clone of a clone is funnier, not a bug) with a full box while he is well
 * past ordinary full ({@link #MIN_FED_TO_CLONE} boxes deep, comfortably short
 * of Krave Rage's 25 so this is a mid-game toy, not an endgame one) and the
 * box does not survive the encounter. Neither, structurally, does the idea
 * that there was only ever going to be one Cayden.
 *
 * <p>The clone ({@link BredCaydenCobb}) inherits a real fraction of his
 * progress rather than either nothing or everything - {@link #INHERIT_FED_FRACTION}
 * of his fed count and roughly half of his taught ascension forms, each
 * re-taught for real through the public {@code tryUnlock} so the transformation
 * spectacle fires exactly as it would if the player had bought it - because a
 * blank slate is a downgrade nobody would ever use this for, and a perfect
 * copy makes the box a strictly-better clone machine instead of a gag with a
 * real cost (a five-minute cooldown per parent, and a hard cap on how many
 * clones one owner can have running around at once).
 */
public class KraveCloningBoxItem extends Item {

    private static final int MIN_FED_TO_CLONE = 15;
    private static final int COOLDOWN_TICKS = 20 * 60 * 5;
    private static final int MAX_BRED_PER_OWNER = 8;
    private static final double INHERIT_FED_FRACTION = 0.4D;
    private static final double INHERIT_UNLOCK_FRACTION = 0.5D;
    private static final double SEARCH_RADIUS = 512.0D;

    private static final String[] PUNCHLINES = {
            "The box gives out somewhere around bowl four hundred. There are now two Caydens, and only one of them looks surprised.",
            "Something in the cardboard structurally fails. A second Cayden steps out of the wreckage, already reaching for a spoon.",
            "It turns out there is a Krave-to-Cayden ratio, and he just found it. Congratulations, you have two now.",
            "The box was rated for one (1) Cayden. This is now a known defect.",
            "He eats past the line printed on the inside of the box that nobody reads. A second him is the warning label."
    };

    public KraveCloningBoxItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CaydenCobb parent)) {
            return InteractionResult.PASS;
        }
        Level rawLevel = target.level();
        if (rawLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel level = (ServerLevel) rawLevel;

        if (parent.getKraveFed() < MIN_FED_TO_CLONE) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "Not fed enough yet - the box needs him considerably more overstuffed than this ("
                    + parent.getKraveFed() + "/" + MIN_FED_TO_CLONE + ")."));
            return InteractionResult.FAIL;
        }
        int cooldown = BondState.familyBoxCooldown(parent);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "This one is still digesting. Give it " + (cooldown / 20) + "s."));
            return InteractionResult.FAIL;
        }
        if (countBredFor(level, player) >= MAX_BRED_PER_OWNER) {
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "You already have " + MAX_BRED_PER_OWNER + " Caydens. The kitchen cannot support a ninth."));
            return InteractionResult.FAIL;
        }

        BredCaydenCobb child = BondsRegistry.BRED_CAYDEN.get().create(level);
        if (child == null) {
            return InteractionResult.FAIL;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 at = parent.position().add(Math.cos(angle) * 1.6D, 0.1D, Math.sin(angle) * 1.6D);
        child.moveTo(at.x, at.y, at.z, parent.getYRot(), 0.0F);
        child.tame(player);
        level.addFreshEntity(child);

        inheritProgress(parent, child);
        BondState.setFamilyBoxCooldown(parent, COOLDOWN_TICKS);

        level.playSound(null, parent.blockPosition(), SoundEvents.GENERIC_EXPLODE, parent.getSoundSource(), 0.7F, 1.7F);
        level.playSound(null, parent.blockPosition(), SoundEvents.VILLAGER_YES, parent.getSoundSource(), 1.2F, 0.8F);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.8D, at.z, 25, 0.4D, 0.5D, 0.4D, 0.05D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.2D, at.z, 12, 0.4D, 0.4D, 0.4D, 0.0D);

        String line = PUNCHLINES[level.random.nextInt(PUNCHLINES.length)];
        for (Player p : level.getEntitiesOfClass(Player.class, parent.getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD
                    + "KRAVE FAMILY BOX: " + ChatFormatting.RESET + ChatFormatting.LIGHT_PURPLE + line));
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                + "New Cayden inherited " + child.getKraveFed() + " fed and " + AscensionLadder.countUnlocked(child.getUnlockMask())
                + " taught form(s)."));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Fed count and Ki are transferred with the public, additive
     * {@code CaydenCobb} API only ({@code restoreKrave}, {@code addKi}).
     * Unlocked forms are re-taught one at a time through the public
     * {@code tryUnlock}, funding it with exactly the Ki those specific rungs
     * cost (via {@code AscensionLadder.rung(tier).kiCost()}, also public) so
     * the child ends up with zero Ki left over rather than a windfall.
     */
    private void inheritProgress(CaydenCobb parent, BredCaydenCobb child) {
        int inheritedFed = (int) Math.floor(parent.getKraveFed() * INHERIT_FED_FRACTION);
        boolean inheritedRage = inheritedFed >= CaydenCobb.RAGE_THRESHOLD && parent.isRageUnlocked();
        child.restoreKrave(inheritedFed, inheritedRage);

        int parentHighest = parent.highestUnlockedTier();
        int childCeiling = (int) Math.floor(parentHighest * INHERIT_UNLOCK_FRACTION);
        int cost = 0;
        for (int tier = AscensionLadder.SSJ; tier <= childCeiling; tier++) {
            if (AscensionLadder.unlocked(parent.getUnlockMask(), tier)) {
                cost += AscensionLadder.rung(tier).kiCost();
            }
        }
        if (cost > 0) {
            child.addKi(cost);
            for (int tier = AscensionLadder.SSJ; tier <= childCeiling; tier++) {
                if (AscensionLadder.unlocked(parent.getUnlockMask(), tier)) {
                    child.tryUnlock(tier, null);
                }
            }
        }
    }

    /** Best-effort cap on live clones per owner - only counts currently loaded ones, which is the population that actually costs tick time. */
    private int countBredFor(ServerLevel level, Player owner) {
        List<BredCaydenCobb> mine = level.getEntitiesOfClass(BredCaydenCobb.class,
                new AABB(owner.blockPosition()).inflate(SEARCH_RADIUS),
                e -> owner.getUUID().equals(e.getOwnerUUID()));
        return mine.size();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.LIGHT_PURPLE + "Right-click a very well-fed Cayden."));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Needs " + MIN_FED_TO_CLONE + "+ boxes eaten. Consumed on use."));
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "Not FDA approved. Not Krave-brand approved either."));
    }
}
