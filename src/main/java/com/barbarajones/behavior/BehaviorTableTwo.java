package com.barbarajones.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.barbarajones.behavior.KraveBehaviors.broadcast;
import static com.barbarajones.behavior.KraveBehaviors.effect;
import static com.barbarajones.behavior.KraveBehaviors.onEaten;
import static com.barbarajones.behavior.KraveBehaviors.onUse;
import static com.barbarajones.behavior.KraveBehaviors.particles;
import static com.barbarajones.behavior.KraveBehaviors.say;
import static com.barbarajones.behavior.KraveBehaviors.shout;
import static com.barbarajones.behavior.KraveBehaviors.sound;

/**
 * The second half of the item behaviours.
 *
 * <p>Split from {@link BehaviorTable} purely for length - one file with every
 * entry becomes something nobody wants to scroll through, and the two register
 * from the same place.
 *
 * <p>A few descriptions promise things Minecraft cannot do: recording gameplay
 * with laugh tracks, replacing a player's skin, muting voice chat, granting
 * admin rights. Those are built as the nearest thing that lands the same joke,
 * and the comment on each one says what was swapped and why. Deleting a random
 * chunk was also skipped deliberately: it is buildable, and it would
 * permanently wreck a world for a punchline that lasts a second.
 */
final class BehaviorTableTwo {

    private BehaviorTableTwo() { }

    static void register() {

        // ---- the All-Red Fit -------------------------------------------------
        // Worn, not clicked, so these announce rather than activate. The set
        // bonus itself is checked on the armour, not here.
        onUse("red_hat", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 30, 0);
            broadcast(level, player, "the hat is backwards. he is 40% more confident about everything.");
            return true;
        });
        onUse("red_pants", (level, player, stack) -> {
            broadcast(level, player, "bro lost a bet.");
            return true;
        });
        onUse("red_shoes", (level, player, stack) -> {
            broadcast(level, player, "the fit is COMPLETE. nobody can say nothin.");
            effect(player, MobEffects.MOVEMENT_SPEED, 20, 1);
            return true;
        });
        onUse("red_shirt", (level, player, stack) -> {
            broadcast(level, player, "head to toe red. he looks like a fire hydrant that got a tax refund.");
            return true;
        });

        // ---- possessions that are jokes about you ----------------------------

        // Private box. Anyone else who opens it gets hit for it.
        onUse("nugget_box", (level, player, stack) -> {
            shout(player, "MINE");
            for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(4.0D))) {
                if (p != player) {
                    p.hurt(p.damageSources().generic(), 4.0F);
                    shout(p, "MINE");
                }
            }
            effect(player, MobEffects.SATURATION, 6, 1);
            return true;
        });

        onUse("donut_box", (level, player, stack) -> {
            effect(player, MobEffects.JUMP, 30, 3);
            effect(player, MobEffects.SATURATION, 5, 1);
            shout(player, "all twelve. you are bouncing now and that is your problem.");
            return true;
        });

        // Adopt whatever baby is in front of you. It never despawns again.
        onUse("adoption_papers", (level, player, stack) -> {
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(6.0D))) {
                if (e instanceof AgeableMob baby && baby.isBaby()) {
                    baby.setPersistenceRequired();
                    baby.setCustomName(net.minecraft.network.chat.Component.literal("dad?"));
                    baby.setCustomNameVisible(true);
                    shout(player, "it's legal now. it is legally yours.");
                    broadcast(level, player, "he just adopted a whole child in front of everybody.");
                    return true;
                }
            }
            say(player, "no baby nearby. the paperwork needs a child.");
            return false;
        });

        onUse("child_support_papers", (level, player, stack) -> {
            shout(player, "somebody is about to learn what RETROACTIVE means.");
            broadcast(level, player, "the papers have been served.");
            return true;
        });

        onUse("moms_belt", (level, player, stack) -> {
            sound(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 3.0F, 0.8F);
            broadcast(level, player, "that WHAP was heard from thirty blocks away.");
            return true;
        });

        onUse("five_hundred_dollars", (level, player, stack) -> {
            shout(player, "it was in the coffee can. she counts it every Sunday.");
            broadcast(level, player, "mom's rage meter just went to maximum.");
            return true;
        });

        onUse("moms_tv_remote", (level, player, stack) -> {
            broadcast(level, player, "she changed it mid-scene. nobody say a word.");
            return true;
        });

        onUse("confiscated_krave", (level, player, stack) -> {
            shout(player, "you took it off the high shelf. she is coming.");
            broadcast(level, player, "somebody just started a boss fight with no countdown.");
            return true;
        });

        // ---- Barbara's supply chain -------------------------------------------

        onUse("stash_jar", (level, player, stack) -> {
            shout(player, "this is her 401k. put it back.");
            return true;
        });

        onUse("pocket_scale", (level, player, stack) -> {
            say(player, "reads to the hundredth. she does not accept eyeballed amounts.");
            sound(level, player, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8F, 1.6F);
            return true;
        });

        onUse("rolling_paper", (level, player, stack) -> {
            say(player, "she is out. she is always out. she is going to ask you.");
            return true;
        });

        onUse("cigarette_pack", (level, player, stack) -> {
            say(player, "twenty. gone in a day and a half. she is \"cutting back\".");
            return true;
        });

        onUse("grass_seeds", (level, player, stack) -> {
            say(player, "the GOOD seeds. the regular stuff is going to feel like an insult now.");
            return true;
        });

        onUse("diced_grass", (level, player, stack) -> {
            say(player, "she did this with a knife. on the counter. where food goes.");
            return true;
        });

        // Effects arrive 45 seconds late, exactly as promised.
        onEaten("grass_brownie", (level, player, stack) -> {
            say(player, "you don't feel nothin. give it a minute.");
            // The wait IS the joke, so nothing happens now - the payload is
            // scheduled for 45 seconds later, by which point you have eaten more.
            DelayedEffects.schedule(player, 900, p -> {
                effect(p, MobEffects.CONFUSION, 40, 0);
                effect(p, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                shout(p, "there it is.");
            });
            return false;
        });

        // ---- cereal ------------------------------------------------------------

        onUse("krave_dust", (level, player, stack) -> {
            effect(player, MobEffects.MOVEMENT_SPEED, 3, 1);
            say(player, "three seconds of feeling cool. that's it. that's the whole thing.");
            particles(level, player, ParticleTypes.CRIT, 12, 0.3D, 0.05D);
            return true;
        });

        onUse("off_brand_krave", (level, player, stack) -> {
            shout(player, "KRAIV. not Krave. KRAIV.");
            broadcast(level, player, "Cayden saw that and something in him died.");
            return true;
        });

        onEaten("stale_krave", (level, player, stack) -> {
            say(player, "no crunch left. he ate it in silence like a soldier.");
            return false;
        });

        onEaten("cereal_bowl", (level, player, stack) -> {
            effect(player, MobEffects.SATURATION, 8, 2);
            broadcast(level, player, "oh my god");
            return false;
        });

        onEaten("chocolate_bar", (level, player, stack) -> {
            effect(player, MobEffects.NIGHT_VISION, 25, 0);
            effect(player, MobEffects.CONFUSION, 10, 0);
            shout(player, "you can hear a colour now.");
            particles(level, player, ParticleTypes.NOTE, 40, 0.8D, 0.4D);
            sound(level, player, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 1.2F);
            return false;
        });

        onEaten("chicken_sandwich", (level, player, stack) -> {
            say(player, "mid. objectively mid. you'll order it again next week.");
            return false;
        });

        // The dog conversation, one more time.
        onUse("chicken_nuggets", (level, player, stack) -> {
            shout(player, "WE ALREADY TALKED ABOUT THIS");
            say(player, "the food. NOT the dog. put the dog down.");
            return true;
        });

        onEaten("cocoa_substitute", (level, player, stack) -> {
            effect(player, MobEffects.HUNGER, 12, 0);
            say(player, "tastes like a hug from somebody who doesn't like you.");
            return false;
        });

        onEaten("milkshake", (level, player, stack) -> {
            broadcast(level, player, "every boy in the yard is walking this way.");
            effect(player, MobEffects.SATURATION, 4, 0);
            return false;
        });

        // ---- yard work ---------------------------------------------------------

        onUse("lawn_mower", (level, player, stack) -> {
            player.causeFoodExhaustion(4.0F);
            say(player, "no engine. just you, the blades and the sun.");
            sound(level, player, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 0.9F, 0.7F);
            return true;
        });

        onUse("weed_whacker", (level, player, stack) -> {
            sound(level, player, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.4F, 1.9F);
            broadcast(level, player, "that WHIRR has not stopped for a full minute.");
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(4.0D))) {
                if (e instanceof LivingEntity living && living != player) {
                    living.hurt(living.damageSources().generic(), 3.0F);
                }
            }
            return true;
        });

        onUse("hedge_trimmer", (level, player, stack) -> {
            say(player, "you did two feet of it and got bored. it'll look like that for six months.");
            return true;
        });

        onUse("rake", (level, player, stack) -> {
            player.hurt(player.damageSources().generic(), 3.0F);
            sound(level, player, SoundEvents.WOOD_BREAK, 1.2F, 0.8F);
            shout(player, "it came up and hit you in the face. cartoon physics. real damage.");
            return true;
        });

        onUse("watering_can", (level, player, stack) -> {
            particles(level, player, ParticleTypes.FALLING_WATER, 30, 1.2D, 0.0D);
            say(player, "they seem slightly less disappointed in you.");
            return true;
        });

        onUse("fertilizer_bag", (level, player, stack) -> {
            for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(10.0D))) {
                effect(p, MobEffects.CONFUSION, 8, 0);
            }
            broadcast(level, player, "he opened the bag. everybody within ten blocks regrets it.");
            return true;
        });

        // ---- media -------------------------------------------------------------

        // Cannot actually record video, so it plays back the bit instead.
        onUse("vhs_blank", (level, player, stack) -> {
            broadcast(level, player, "[recording] ...and then a laugh track. and a zoom. badly timed.");
            sound(level, player, SoundEvents.NOTE_BLOCK_BIT.value(), 1.0F, 0.7F);
            return true;
        });

        onUse("krave_video_tape", (level, player, stack) -> {
            broadcast(level, player, "the original tape. this is where all of it started.");
            return true;
        });

        // If Barbara can hear it, she goes and finds a television.
        onUse("vhs_barbara_interview", (level, player, stack) -> {
            broadcast(level, player, "THE tape is playing.");
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(32.0D))) {
                if (e instanceof com.barbarajones.entity.BarbaraJones barbara) {
                    barbara.getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), 1.4D);
                    broadcast(level, player, "Barbara dropped what she was doing.");
                }
            }
            return true;
        });

        onUse("record_flyrich", (level, player, stack) -> {
            broadcast(level, player, "people on other continents can hear your taste.");
            return true;
        });

        onUse("flyrich_poster", (level, player, stack) -> {
            effect(player, MobEffects.DAMAGE_BOOST, 6, 0);
            say(player, "motivated. briefly. it wears off the second you look away.");
            return true;
        });

        // ---- the crew's belongings ----------------------------------------------

        onUse("ski_mask", (level, player, stack) -> {
            effect(player, MobEffects.NIGHT_VISION, 60, 0);
            broadcast(level, player, "bro what are you about to do");
            return true;
        });

        onUse("plug_trophy", (level, player, stack) -> {
            effect(player, MobEffects.INVISIBILITY, 20, 0);
            say(player, "nobody knows what he looks like. now nobody knows what you look like.");
            return true;
        });

        onUse("sniper_scope", (level, player, stack) -> {
            effect(player, MobEffects.NIGHT_VISION, 30, 0);
            say(player, "he got a notification the second you looked through it.");
            return true;
        });

        onUse("managers_tie", (level, player, stack) -> {
            effect(player, MobEffects.WEAKNESS, 45, 0);
            say(player, "clip-on. of course it is. you deal less damage now, you look like you gave up.");
            return true;
        });

        // No voice chat exists, so he says it in chat and breaks something.
        onUse("managers_headset", (level, player, stack) -> {
            broadcast(level, player, "\"have you tried turning it off and on again\"");
            effect(player, MobEffects.MOVEMENT_SLOWDOWN, 8, 1);
            return true;
        });

        onUse("static_ip", (level, player, stack) -> {
            say(player, "you're already thinking about it. don't.");
            return true;
        });

        onUse("fiber_optic_coil", (level, player, stack) -> {
            broadcast(level, player, "somewhere, a manager started yelling about latency.");
            return true;
        });

        onUse("duhl_wol_trophy", (level, player, stack) -> {
            say(player, "he has not noticed yet. the meter is filling.");
            return true;
        });

        onUse("barbara_trophy", (level, player, stack) -> {
            broadcast(level, player, "employee of the month. a laminated sheet instead of a raise.");
            return true;
        });

        onUse("krave_monster_trophy", (level, player, stack) -> {
            effect(player, MobEffects.DAMAGE_BOOST, 120, 0);
            sound(level, player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.4F, 1.0F);
            broadcast(level, player, "all six forms. he is going to bring this up forever.");
            return true;
        });

        onUse("nugget_collar", (level, player, stack) -> {
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(24.0D))) {
                if (e instanceof LivingEntity dog && e.getType().toString().contains("nugget")) {
                    dog.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            MobEffects.GLOWING, 20 * 600, 0, false, false, true));
                }
            }
            say(player, "he glows now. you cannot lose the good boy again.");
            return true;
        });

        // ---- odds and ends -------------------------------------------------------

        onUse("rat_tail", (level, player, stack) -> {
            shout(player, "why the fuck do you have a rat tail");
            return true;
        });

        onUse("yellow_teeth", (level, player, stack) -> {
            broadcast(level, player, "villagers are inventing new languages to scream in.");
            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(10.0D))) {
                if (e instanceof net.minecraft.world.entity.PathfinderMob mob) {
                    mob.getNavigation().moveTo(
                            mob.getX() + (mob.getX() - player.getX()) * 2,
                            mob.getY(),
                            mob.getZ() + (mob.getZ() - player.getZ()) * 2, 1.8D);
                }
            }
            return true;
        });

        onUse("ash", (level, player, stack) -> {
            say(player, "something used to be here. nobody is discussing it.");
            return true;
        });

        // Buildable as chunk deletion, deliberately not built that way.
        onUse("minecraft_disc", (level, player, stack) -> {
            if (level.random.nextBoolean()) {
                effect(player, MobEffects.LUCK, 30, 0);
                shout(player, "free hat. no questions.");
            } else {
                effect(player, MobEffects.CONFUSION, 20, 0);
                shout(player, "something in the world is wrong now and you will find it later.");
            }
            return true;
        });

        onUse("chepina", (level, player, stack) -> {
            // Genuinely different every time, because the tooltip says so.
            switch (level.random.nextInt(6)) {
                case 0 -> effect(player, MobEffects.LEVITATION, 4, 0);
                case 1 -> effect(player, MobEffects.INVISIBILITY, 15, 0);
                case 2 -> effect(player, MobEffects.DIG_SPEED, 20, 2);
                case 3 -> player.setSecondsOnFire(3);
                case 4 -> effect(player, MobEffects.NIGHT_VISION, 30, 0);
                default -> effect(player, MobEffects.JUMP, 20, 3);
            }
            say(player, "Chepina. nobody knows what that was going to do. including the game.");
            return true;
        });

        onEaten("chepina_jug", (level, player, stack) -> {
            effect(player, MobEffects.SATURATION, 10, 4);
            effect(player, MobEffects.BLINDNESS, 15, 0);
            shout(player, "the whole jug. alone. in the dark. now literally.");
            return false;
        });

        onUse("boarding_pass", (level, player, stack) -> {
            if (level.random.nextInt(10) < 4) {
                shout(player, "they pulled you aside anyway.");
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, 10, 2);
            } else {
                say(player, "you're through. gate B47. it moved to C12.");
            }
            return true;
        });

        onUse("sewer_key", (level, player, stack) -> {
            effect(player, MobEffects.CONFUSION, 10, 0);
            say(player, "you really finna go down there. by yourself. right now.");
            return true;
        });

        onUse("recipe_book", (level, player, stack) -> {
            say(player, "every recipe starts with \"first, roll up\". even the pancakes.");
            return true;
        });

        onUse("krave_radio", (level, player, stack) -> {
            broadcast(level, player, "one station. it is cereal. there is no off button.");
            sound(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.4F);
            return true;
        });

        onUse("krave_shard", (level, player, stack) -> {
            say(player, "getting hit with breakfast is a new low and you know it.");
            return true;
        });

        onUse("krave_carton", (level, player, stack) -> {
            say(player, "it holds cereal. that is the entire feature set.");
            return true;
        });

        onUse("roasted_husk", (level, player, stack) -> {
            say(player, "step one of never walking to a jungle again.");
            return true;
        });

        onUse("housing_query", (level, player, stack) -> {
            say(player, "scanning... this is not a house. this is a crime.");
            return true;
        });
    }
}
