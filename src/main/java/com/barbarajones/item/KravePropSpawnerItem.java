package com.barbarajones.item;

import com.barbarajones.content.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Spawner for the prop entities that cannot have a spawn egg.
 *
 * A spawn egg has to be built around an {@code EntityType<? extends Mob>}, and
 * the car, the meteors, the tornado, the beams, the sky actors, and the Krave
 * Healing Box (rebuilt End-Crystal-style - a plain Entity, no AI, never a Mob)
 * are all plain Entities. This item stands in for the eggs they cannot have:
 * sneak-right-click in the air to cycle the selection, right-click a block to
 * place the selected prop.
 */
public class KravePropSpawnerItem extends Item {

    private record Prop(String label, Supplier<EntityType<? extends Entity>> type) { }

    private static final List<Prop> PROPS = List.of(
            new Prop("Duhl Wol's Car",  () -> ModEntities.DUHL_WOL_CAR.get()),
            new Prop("Krave Meteor",    () -> ModEntities.METEOR.get()),
            new Prop("Giant Krave Box", () -> ModEntities.GIANT_BOX.get()),
            new Prop("Krave Tornado",   () -> ModEntities.TORNADO.get()),
            new Prop("Krave Laser",     () -> ModEntities.KRAVE_LASER.get()),
            new Prop("Krave Mouth Beam", () -> ModEntities.KRAVE_MOUTH_BEAM.get()),
            new Prop("Sky Cinematic",   () -> ModEntities.SKY_CINEMATIC.get()),
            new Prop("Krave Healing Box", () -> ModEntities.KRAVE_HEALING_BOX.get()));

    public KravePropSpawnerItem(Properties props) {
        super(props);
    }

    private static int index(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Prop") % PROPS.size();
    }

    /** Sneak-right-click in the air cycles which prop is selected. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            int next = (index(stack) + 1) % PROPS.size();
            stack.getOrCreateTag().putInt("Prop", next);
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE
                    + "Selected: " + ChatFormatting.WHITE + PROPS.get(next).label()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Right-click a block to drop the selected prop on top of it. */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return InteractionResult.PASS;   // sneaking is the cycle gesture
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Prop prop = PROPS.get(index(ctx.getItemInHand()));
        Entity spawned = prop.type().get().create(level);
        if (spawned == null) {
            return InteractionResult.FAIL;
        }
        Vec3 at = Vec3.atBottomCenterOf(ctx.getClickedPos().above());
        spawned.moveTo(at.x, at.y, at.z, ctx.getHorizontalDirection().toYRot(), 0.0F);
        level.addFreshEntity(spawned);

        if (player != null) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "Spawned " + prop.label() + "."));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.LIGHT_PURPLE + "Selected: "
                + ChatFormatting.WHITE + PROPS.get(index(stack)).label()));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Right-click a block: place it"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Sneak + right-click: next prop"));
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY
                + "For the props that cannot have a spawn egg."));
    }
}
