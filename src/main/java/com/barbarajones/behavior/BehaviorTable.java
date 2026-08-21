package com.barbarajones.behavior;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.barbarajones.behavior.KraveBehaviors.broadcast;
import static com.barbarajones.behavior.KraveBehaviors.effect;
import static com.barbarajones.behavior.KraveBehaviors.onEaten;
import static com.barbarajones.behavior.KraveBehaviors.onUse;
import static com.barbarajones.behavior.KraveBehaviors.particles;
import static com.barbarajones.behavior.KraveBehaviors.say;
import static com.barbarajones.behavior.KraveBehaviors.shout;
import static com.barbarajones.behavior.KraveBehaviors.sound;

/**
 * What each item actually does, keyed by its id.
 *
 * <p>One entry per item, in the order the item list is written, so this file and
 * the descriptions can be read side by side. If a tooltip promises something,
 * the promise is kept here or it is not kept at all.
 *
 * <p>Registered once on server start rather than in a static initialiser: these
 * lambdas capture nothing, but the sounds and effects they reference are
 * registry objects, and touching a registry object before the registries have
 * thawed is how this project has crashed before.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class BehaviorTable {

    private static boolean registered;

    private BehaviorTable() { }

    @SubscribeEvent
    public static void register(ServerStartedEvent event) {
        if (registered) {
            return;
        }
        registered = true;
        BehaviorTableTwo.register();

        // ---- drugs, food and other bad decisions ---------------------------

        // Speed and jump, then twenty seconds of sneezing snow everywhere.
        onUse("fake_cocaine", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 8, 4);
            effect(player, MobEffects.JUMP, 8, 4);
            effect(player, MobEffects.CONFUSION, 20, 0);
            shout(player, "it's snow. it's been snow this whole time.");
            particles(level, player, ParticleTypes.SNOWFLAKE, 60, 0.8D, 0.2D);
            sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F);
            return true;
        });

        // A high, then a coughing fit that roots you to the spot.
        onUse("bong", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 10, 4);
            effect(player, MobEffects.CONFUSION, 12, 0);
            effect(player, MobEffects.REGENERATION, 10, 0);
            say(player, "you are coughing so hard you cannot move.");
            sound(level, player, SoundEvents.PLAYER_HURT, 0.8F, 0.6F);
            particles(level, player, ParticleTypes.CAMPFIRE_COSY_SMOKE, 40, 0.6D, 0.02D);
            return true;
        });

        onUse("rolled_joint", (level, player, stack) -> {
            effect(player, MobEffects.REGENERATION, 30, 0);
            effect(player, MobEffects.CONFUSION, 8, 0);
            say(player, "hand rolled. artisanal. she'd want you to know that.");
            particles(level, player, ParticleTypes.CAMPFIRE_COSY_SMOKE, 20, 0.4D, 0.01D);
            return true;
        });

        // Strongest in the mod, so it is the only one that stacks everything.
        onUse("golden_joint", (level, player, stack) -> {
            effect(player, MobEffects.REGENERATION, 60, 1);
            effect(player, MobEffects.DAMAGE_BOOST, 60, 1);
            effect(player, MobEffects.MOVEMENT_SPEED, 60, 1);
            effect(player, MobEffects.CONFUSION, 25, 0);
            shout(player, "you are a different person for a few minutes.");
            particles(level, player, ParticleTypes.TOTEM_OF_UNDYING, 60, 0.7D, 0.3D);
            return true;
        });

        // Nothing but nausea and an announcement of your own foolishness.
        onUse("fake_weed", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 10, 0);
            shout(player, "THIS SHIT IS MID");
            broadcast(level, player, "somebody just smoked oregano.");
            return true;
        });

        // A micro-high and eight seconds of coughing that cancels what you were doing.
        onUse("burnt_grass", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 8, 2);
            effect(player, MobEffects.CONFUSION, 6, 0);
            say(player, "she hit it too hard and now everyone watched.");
            return true;
        });

        onUse("handful_of_grass", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 5, 0);
            say(player, "smoked straight out the ground. like an animal.");
            return true;
        });

        onUse("cigarette", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 12, 0);
            say(player, "if Barbara is anywhere nearby she already knows.");
            return true;
        });

        // Nearby players are told to be nice to you. Enforcement not included.
        onUse("cigar", (level, player, stack) -> {
            effect(player, MobEffects.DAMAGE_RESISTANCE, 45, 0);
            broadcast(level, player, "compliment this man's build. immediately.");
            return true;
        });

        // ---- the Pibb family ------------------------------------------------

        onUse("mr_pibb", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 20, 0);
            say(player, "Barbara is already walking toward you.");
            return true;
        });

        onUse("mr_pibb_xtra", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 40, 1);
            say(player, "XTRA. buying regular is a skill issue.");
            return true;
        });

        onUse("mr_pibb_two_liter", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 90, 1);
            effect(player, MobEffects.SATURATION, 5, 0);
            shout(player, "two full liters. straight from the bottle.");
            return true;
        });

        // Does nothing useful, on purpose, and tells you so.
        onUse("pibb_zero", (level, player, stack) -> {
            effect(player, MobEffects.WEAKNESS, 15, 0);
            shout(player, "why");
            return true;
        });

        // A genuinely random short effect, because the tooltip says "???".
        onUse("pibb_cocktail", (level, player, stack) -> {
            var pool = new net.minecraft.world.effect.MobEffect[] {
                MobEffects.MOVEMENT_SPEED, MobEffects.JUMP, MobEffects.CONFUSION,
                MobEffects.NIGHT_VISION, MobEffects.DIG_SPEED, MobEffects.WEAKNESS,
                MobEffects.DAMAGE_BOOST, MobEffects.INVISIBILITY };
            effect(player, pool[level.random.nextInt(pool.length)], 20, 0);
            say(player, "???");
            return true;
        });

        onUse("sweet_tea", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 15, 2);
            say(player, "the crash is coming. it is coming fast.");
            // The crash: rooted in place once the rush wears off.
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 300, 4, false, true, true));
            return true;
        });

        // Always blue. Any other colour is not on offer.
        onUse("gatorade", (level, player, stack) -> {
            effect(player, MobEffects.SATURATION, 4, 0);
            effect(player, MobEffects.MOVEMENT_SPEED, 25, 0);
            say(player, "it's the blue one. it's always the blue one.");
            return true;
        });

        // ---- food -----------------------------------------------------------

        // Restores hunger, then sets your mouth on fire about it.
        onEaten("apple_pie", (level, player, stack) -> {
            player.setSecondsOnFire(5);
            shout(player, "the inside was LAVA and you knew that");
            return false;
        });

        // The only food in the mod that never betrays you.
        onEaten("hash_browns", (level, player, stack) -> {
            effect(player, MobEffects.SATURATION, 3, 0);
            say(player, "the only thing in this whole breakfast that never let you down.");
            return false;
        });

        // Frozen middle, molten outside. Both, at once, every time.
        onEaten("microwave_burrito", (level, player, stack) -> {
            player.setSecondsOnFire(3);
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 6, 1);
            shout(player, "frozen in the middle. every time. EVERY TIME.");
            return false;
        });

        // Nine hours under the lamp: either it made you stronger or it did not.
        onEaten("gas_station_hot_dog", (level, player, stack) -> {
            if (level.random.nextBoolean()) {
                effect(player, MobEffects.DAMAGE_BOOST, 30, 1);
                shout(player, "nine hours under that lamp and it made you STRONGER");
            } else {
                effect(player, MobEffects.WITHER, 6, 0);
                shout(player, "nine hours under that lamp. this was the other outcome.");
            }
            return false;
        });

        // Glowing, undated, and a coin flip either way.
        onEaten("honey_bun", (level, player, stack) -> {
            if (level.random.nextBoolean()) {
                effect(player, MobEffects.DAMAGE_RESISTANCE, 10, 4);
                shout(player, "it was glowing for a REASON");
            } else {
                effect(player, MobEffects.POISON, 8, 2);
                shout(player, "it was glowing for a reason.");
            }
            return false;
        });

        onEaten("fries", (level, player, stack) -> {
            say(player, "cold before you left the parking lot.");
            return false;
        });

        onEaten("off_brand_pastries", (level, player, stack) -> {
            say(player, "the filling is just red. it tastes like a COLOR.");
            return false;
        });

        onEaten("toaster_pastries", (level, player, stack) -> {
            effect(player, MobEffects.SATURATION, 4, 0);
            effect(player, MobEffects.REGENERATION, 8, 0);
            shout(player, "mom came THROUGH. the frosting got sprinkles ON it.");
            return false;
        });

        // Five prompts and it still lets you.
        onEaten("sewer_water", (level, player, stack) -> {
            effect(player, MobEffects.POISON, 12, 1);
            effect(player, MobEffects.CONFUSION, 15, 0);
            shout(player, "you were asked five times.");
            return false;
        });

        // Eating it raw was a choice you made freely.
        onEaten("krave_batter", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 10, 0);
            shout(player, "you ate it raw. the game let you. that's on you.");
            return false;
        });

        onEaten("raw_krave_piece", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 8, 0);
            shout(player, "you were warned");
            return false;
        });

        onEaten("krave_dirt", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 6, 0);
            say(player, "it was dirt. chocolate flavoured, but dirt.");
            return false;
        });

        // Cold milk, bad effects gone, and a small lift.
        onEaten("krave_milk", (level, player, stack) -> {
            player.removeAllEffects();
            effect(player, MobEffects.REGENERATION, 6, 0);
            say(player, "everything cleared. even the good ones.");
            return false;
        });

        // ---- Barbara's things ------------------------------------------------

        // Cleans everything off you, including what you paid for.
        onUse("soap", (level, player, stack) -> {
            player.removeAllEffects();
            say(player, "you now smell like nothing at all.");
            sound(level, player, SoundEvents.BUCKET_EMPTY, 0.8F, 1.4F);
            return true;
        });

        // Nausea gone, everything nearby now afraid of you.
        onUse("toothbrush", (level, player, stack) -> {
            player.removeEffect(MobEffects.CONFUSION);
            say(player, "the bristles are gone. this is a stick.");
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(12.0D))) {
                if (e instanceof net.minecraft.world.entity.PathfinderMob mob) {
                    mob.getNavigation().moveTo(
                            mob.getX() + (mob.getX() - player.getX()),
                            mob.getY(),
                            mob.getZ() + (mob.getZ() - player.getZ()), 1.6D);
                }
            }
            broadcast(level, player, "everything within twelve blocks just left.");
            return true;
        });

        // Always wet. Drying yourself makes it worse.
        onUse("towel", (level, player, stack) -> {
            say(player, "you are somehow damper than before.");
            particles(level, player, ParticleTypes.FALLING_WATER, 25, 0.5D, 0.0D);
            return true;
        });

        onUse("barbara_plush", (level, player, stack) -> {
            effect(player, MobEffects.REGENERATION, 20, 0);
            broadcast(level, player, "the plush said \"ugh\" loud enough for everyone to hear.");
            return true;
        });

        onUse("ashtray", (level, player, stack) -> {
            say(player, "layers like the earth. nothing in here is worth having.");
            return true;
        });

        // ---- phones, screens and other technology -----------------------------

        // Invisible in normal chat, which is the entire point of it.
        onUse("flip_phone", (level, player, stack) -> {
            say(player, "[sent on the other phone] nobody saw that.");
            sound(level, player, SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.8F, 1.8F);
            return true;
        });

        // Two contacts and you never remember which is which.
        onUse("burner_phone", (level, player, stack) -> {
            if (level.random.nextBoolean()) {
                say(player, "you called Duhl Wol. he is on his way.");
            } else {
                say(player, "somebody picked up and started screaming about money.");
            }
            sound(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8F, 0.6F);
            return true;
        });

        // One wrong digit and you start over.
        onUse("rotary_phone", (level, player, stack) -> {
            if (level.random.nextInt(3) == 0) {
                shout(player, "you fumbled the LAST digit. start again.");
                sound(level, player, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.0F, 0.5F);
            } else {
                say(player, "ringing. slowly. one digit at a time.");
            }
            return true;
        });

        // A third of your clicks simply fail.
        onUse("computer_mouse", (level, player, stack) -> {
            if (level.random.nextInt(10) < 3) {
                say(player, "the click did not register. do it again.");
                sound(level, player, SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.5F);
            } else {
                say(player, "click.");
            }
            return true;
        });

        onUse("virus", (level, player, stack) -> {
            shout(player, "CONGRATULATIONS!!! YOU ARE THE 1,000,000th VISITOR!!!");
            effect(player, MobEffects.CONFUSION, 15, 0);
            broadcast(level, player, "their computer is advertising dick pills now.");
            return true;
        });

        onUse("usb_drive", (level, player, stack) -> {
            say(player, "47 blurry photos, 12 unpaid bills, and one text file.");
            say(player, "the text file just says \"it was oregano\".");
            return true;
        });

        onUse("laptop", (level, player, stack) -> {
            say(player, "one folder is named \"definitely not crimes\".");
            if (level.random.nextInt(4) == 0) {
                broadcast(level, player, "something just pinged The Manager.");
            }
            return true;
        });

        // Only works at one specific stupid angle.
        onUse("tv_antenna", (level, player, stack) -> {
            if (player.getXRot() > 20.0F || player.getXRot() < -20.0F) {
                say(player, "picture's back. do not move. do not breathe.");
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 6, 3);
            } else {
                say(player, "static. try a stupider angle.");
            }
            return true;
        });

        // Batteries at 4%: it takes several attempts and belief.
        onUse("remote_control", (level, player, stack) -> {
            if (level.random.nextInt(3) == 0) {
                say(player, "channel changed. do not question it.");
            } else {
                say(player, "nothing. hold it at an angle and press harder.");
            }
            return true;
        });

        // ---- money and paperwork ---------------------------------------------

        onUse("counterfeit_bill", (level, player, stack) -> {
            if (level.random.nextInt(10) < 4) {
                say(player, "they took it. walk. do not run. walk.");
            } else {
                shout(player, "the pen came back BLACK");
                broadcast(level, player, "somebody just got called a damn liar.");
            }
            return true;
        });

        onUse("dollars", (level, player, stack) -> {
            broadcast(level, player, "he is flashing a whole WAD right now.");
            effect(player, MobEffects.LUCK, 25, 0);
            return true;
        });

        onUse("scam_receipt", (level, player, stack) -> {
            say(player, "total: $0.00. and somebody kept it.");
            return true;
        });

        onUse("iou_note", (level, player, stack) -> {
            say(player, "the debt is quietly growing interest in the background.");
            return true;
        });

        onUse("debt_notice", (level, player, stack) -> {
            shout(player, "third FINAL notice. next one comes with a person attached.");
            return true;
        });

        onUse("pink_slip", (level, player, stack) -> {
            effect(player, MobEffects.WEAKNESS, 60, 0);
            shout(player, "you're fired. pack your desk.");
            return true;
        });

        onUse("severance_check", (level, player, stack) -> {
            effect(player, MobEffects.DAMAGE_RESISTANCE, 30, 0);
            shout(player, "they paid you to leave. take it and go.");
            return true;
        });

        // ---- tools that are jokes ---------------------------------------------

        // Burns your own fingers a quarter of the time.
        onUse("matchbook", (level, player, stack) -> {
            if (level.random.nextInt(4) == 0) {
                player.setSecondsOnFire(2);
                shout(player, "you burned your own hand and told nobody.");
            } else {
                say(player, "lit. first try. act normal.");
            }
            return true;
        });

        // Fifteen percent of the time it goes off in your hand.
        onUse("blowtorch", (level, player, stack) -> {
            if (level.random.nextInt(100) < 15) {
                player.setSecondsOnFire(6);
                shout(player, "it went off in your hand. only you are on fire.");
                particles(level, player, ParticleTypes.EXPLOSION, 6, 0.4D, 0.1D);
            } else {
                say(player, "tiny flame. no license. no supervision.");
            }
            return true;
        });

        onUse("lighter", (level, player, stack) -> {
            say(player, "sixteen blocks of range and it never runs out.");
            return true;
        });

        // Every flip hurts the people around you.
        onUse("daniels_zippo", (level, player, stack) -> {
            sound(level, player, SoundEvents.IRON_DOOR_OPEN, 1.4F, 2.0F);
            for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(10.0D))) {
                if (p != player) {
                    p.hurt(p.damageSources().generic(), 1.0F);
                    say(p, "he flipped it AGAIN.");
                }
            }
            return true;
        });

        // ---- the loud ones ---------------------------------------------------

        onUse("boombox", (level, player, stack) -> {
            broadcast(level, player, "you are all hearing his playlist now. consent was not sought.");
            sound(level, player, SoundEvents.NOTE_BLOCK_PLING.value(), 4.0F, 1.0F);
            return true;
        });

        onUse("microphone", (level, player, stack) -> {
            broadcast(level, player, "the mic only picked up the most embarrassing thing said here.");
            return true;
        });

        onUse("camera", (level, player, stack) -> {
            broadcast(level, player, "that got recorded. specifically that. not the good part.");
            sound(level, player, SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.6F);
            return true;
        });
    }
}
