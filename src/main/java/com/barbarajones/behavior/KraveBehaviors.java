package com.barbarajones.behavior;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Gives the mod's items the behaviour their tooltips promise.
 *
 * <p>Hooked from the event bus rather than by overriding {@code Item.use} on 238
 * classes. Almost every item in this mod is a plain {@code new Item(props())} -
 * giving each one a subclass to hold a few lines of behaviour would be 238 new
 * files, 238 registration changes, and a very good chance of a typo breaking
 * registration. Intercepting the right-click instead means an item's behaviour
 * is one entry in {@link BehaviorTable} keyed by its id, and adding one later
 * touches exactly one line.
 *
 * <p>Two hooks, because items are used two different ways:
 * <ul>
 *   <li>{@code RightClickItem} - for everything you activate by holding it.</li>
 *   <li>{@code UseItemFinish} - for food, which has already been swallowed by
 *       the time the effect should land. Hooking the right-click on food would
 *       fire the effect before the eating animation even starts.</li>
 * </ul>
 *
 * <p>Everything runs server-side only. An effect applied on the client is a
 * visual lie that vanishes on the next sync, and it is a genuinely confusing
 * bug to chase.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class KraveBehaviors {

    /** What an item does. Returns true if it should go on cooldown. */
    @FunctionalInterface
    public interface Behavior {
        boolean run(ServerLevel level, ServerPlayer player, ItemStack stack);
    }

    private static final Map<String, Behavior> USE = new HashMap<>();
    private static final Map<String, Behavior> EATEN = new HashMap<>();

    /** Default cooldown so a held right-click cannot machine-gun an effect. */
    private static final int COOLDOWN = 40;

    private KraveBehaviors() { }

    static void onUse(String id, Behavior behavior) {
        USE.put(id, behavior);
    }

    static void onEaten(String id, Behavior behavior) {
        EATEN.put(id, behavior);
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String id = idOf(event.getItemStack());
        Behavior behavior = id == null ? null : USE.get(id);
        if (behavior == null) {
            return;
        }
        if (behavior.run(level, player, event.getItemStack())) {
            player.getCooldowns().addCooldown(event.getItemStack().getItem(), COOLDOWN);
        }
        // Only swallow the click for items that have no vanilla right-click of
        // their own. Cancelling it on armour stops it equipping and on a
        // BlockItem stops it placing, which would break the item to give it a
        // joke - the joke is not worth the item.
        if (!(event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem)
                && !(event.getItemStack().getItem() instanceof net.minecraft.world.item.ArmorItem)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void finishedEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        String id = idOf(event.getItem());
        Behavior behavior = id == null ? null : EATEN.get(id);
        if (behavior != null) {
            behavior.run(level, player, event.getItem());
        }
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && BarbaraJonesMod.MODID.equals(id.getNamespace()) ? id.getPath() : null;
    }

    // ---- helpers the table is written in -----------------------------------

    /** Duration is in SECONDS here; the table is easier to read that way. */
    public static void effect(Player player, MobEffect effect, int seconds, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier, false, true, true));
    }

    public static void say(Player player, String text) {
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + text));
    }

    public static void shout(Player player, String text) {
        player.sendSystemMessage(Component.literal(
                ChatFormatting.GOLD + "" + ChatFormatting.BOLD + text));
    }

    /** Everyone nearby hears about it. Half these items are jokes at your expense. */
    public static void broadcast(ServerLevel level, Player near, String text) {
        for (Player p : level.getEntitiesOfClass(Player.class, near.getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GRAY + text));
        }
    }

    public static void sound(ServerLevel level, Player at, SoundEvent event, float volume, float pitch) {
        level.playSound(null, at.blockPosition(), event, SoundSource.PLAYERS, volume, pitch);
    }

    public static void particles(ServerLevel level, Player at, ParticleOptions type,
                                 int count, double spread, double speed) {
        level.sendParticles(type, at.getX(), at.getY() + at.getBbHeight() * 0.6D, at.getZ(),
                count, spread, spread, spread, speed);
    }
}
