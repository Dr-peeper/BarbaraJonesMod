package com.barbarajones.apocalypse;

import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Super Saiyan showdown. Cayden and Krave Monster fight each other
 * directly (see KraveMonster.hurt()'s damage gating and CaydenCobb's
 * SsjFlyAttackGoal) - this controller's job is everything AROUND that fight:
 * spawning Krave Minions near the player (who Cayden ignores and who ignore
 * him back) and Krave Healing Boxes near the boss, so the player has a real
 * job during the fight instead of watching a cutscene. Modeled directly on
 * KraveApocalypse's staged-controller pattern.
 */
public final class KraveKosmosBattle {

    private static final List<KraveKosmosBattle> ACTIVE = new ArrayList<>();

    /** Ticks between Cayden's meteor barrages. */
    private static final int METEOR_INTERVAL = 90;
    private static final int MINION_INTERVAL = 200;
    private static final int BOX_INTERVAL = 500;
    private static final int MAX_MINIONS = 6;
    private static final int MAX_BOXES = 3;

    private final ServerLevel level;
    private final KraveMonster boss;
    private final CaydenCobb cayden;

    private int t;
    private int minionTimer = 100;
    private int boxTimer = 200;
    private int meteorTimer = 60;

    private KraveKosmosBattle(ServerLevel level, KraveMonster boss, CaydenCobb cayden) {
        this.level = level;
        this.boss = boss;
        this.cayden = cayden;
    }

    /** Starts the fight if this boss doesn't already have one running. */
    public static void start(ServerLevel level, KraveMonster boss, CaydenCobb cayden) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss == boss) {
                return;
            }
        }
        boss.setBossFightActive(true);
        ACTIVE.add(new KraveKosmosBattle(level, boss, cayden));

        for (Player p : level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(64.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "Clear the minions. Destroy the boxes before they heal him. Cayden has the rest."));
        }
    }

    public static boolean isActive(KraveMonster boss) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss == boss) {
                return true;
            }
        }
        return false;
    }

    public static void tickAll() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (KraveKosmosBattle b : new ArrayList<>(ACTIVE)) {
            if (b.tick()) {
                ACTIVE.remove(b);
            }
        }
    }

    /** @return true when this fight is over and should be removed. */
    private boolean tick() {
        this.t++;

        if (!this.boss.isAlive()) {
            announce(ChatFormatting.GREEN, "" + ChatFormatting.BOLD, "IT STOPS NOW.");
            // The whole point of the ascension was this fight. It is over.
            if (this.cayden.isAlive()) {
                this.cayden.powerDown();
            }
            return true;
        }
        if (!this.cayden.isAlive() || !this.cayden.isSuperSaiyan()) {
            this.boss.setBossFightActive(false);
            announce(ChatFormatting.RED, "", "Cayden powers down. The fight is over - for now.");
            return true;
        }

        catchTheFallen();
        // The apocalypse arsenal, pointed at someone who deserves it. This is
        // the one place he can throw it without dying for it.
        if (--this.meteorTimer <= 0) {
            this.meteorTimer = METEOR_INTERVAL;
            meteorBarrage();
        }

        if (--this.minionTimer <= 0) {
            this.minionTimer = MINION_INTERVAL;
            if (countNearby(KraveMinion.class) < MAX_MINIONS) {
                spawnMinion();
            }
        }
        if (--this.boxTimer <= 0) {
            this.boxTimer = BOX_INTERVAL;
            if (countNearby(KraveHealingBox.class) < MAX_BOXES) {
                spawnHealingBox();
            }
        }
        return false;
    }


    /** How far below the boss counts as having left the world. */
    private static final double VOID_LINE = 24.0D;

    /**
     * Puts anyone who fell out of the arena back into it.
     *
     * <p>His attacks demolish the ground now, and the Kosmos is floating islands
     * over open void: between a crater and a knockback the fight can throw you
     * off the edge of the world with no way back and no body to recover. The
     * demolition already refuses to dig below the slab it started on, so the
     * arena itself cannot be destroyed out from under the fight - this covers
     * the other way out, which is being knocked over the side of it.
     *
     * <p>Only while the fight is running, and only in the fight, so it never
     * becomes a general no-fall-damage rule that quietly breaks the rest of the
     * dimension.
     */
    private void catchTheFallen() {
        double line = this.boss.getY() - VOID_LINE;
        for (Player p : this.level.getEntitiesOfClass(Player.class,
                this.boss.getBoundingBox().inflate(160.0D))) {
            if (p.getY() > line || p.isSpectator() || p.isCreative()) {
                continue;
            }
            Vec3 back = this.boss.position().add(
                    (this.level.random.nextDouble() - 0.5D) * 12.0D,
                    6.0D,
                    (this.level.random.nextDouble() - 0.5D) * 12.0D);
            p.teleportTo(back.x, back.y, back.z);
            p.setDeltaMovement(0.0D, 0.0D, 0.0D);
            p.fallDistance = 0.0F;
            p.hurtMarked = true;
            p.sendSystemMessage(Component.literal(
                    ChatFormatting.DARK_RED + "The Kosmos is not done with you."));
        }
    }

    private int countNearby(Class<? extends net.minecraft.world.entity.Entity> type) {
        return this.level.getEntitiesOfClass(type, this.boss.getBoundingBox().inflate(80.0D)).size();
    }

    /** Rains Cayden's meteors down on the boss - and only on the boss' side. */
    private void meteorBarrage() {
        Vec3 at = this.boss.position();
        int count = 3 + this.level.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            com.barbarajones.entity.KraveMeteor m = ModEntities.METEOR.get().create(this.level);
            if (m == null) {
                continue;
            }
            double ox = (this.level.random.nextDouble() - 0.5D) * 10.0D;
            double oz = (this.level.random.nextDouble() - 0.5D) * 10.0D;
            m.saiyanStrike(this.cayden);
            m.setPos(at.x + ox, at.y + 42.0D + i * 3.0D, at.z + oz);
            m.aim(-ox * 0.05D, -oz * 0.05D);
            this.level.addFreshEntity(m);
        }
        this.cayden.playSound(com.barbarajones.content.ModSounds.KRAVE_ROAR.get(), 1.4F, 1.2F);
    }

    private void spawnMinion() {
        Player owner = this.cayden.getOwner() instanceof Player p ? p : null;
        Vec3 near = owner != null ? owner.position() : this.boss.position();
        double ang = this.level.random.nextDouble() * Math.PI * 2.0D;
        KraveMinion minion = ModEntities.KRAVE_MINION.get().create(this.level);
        if (minion == null) {
            return;
        }
        minion.setPos(near.x + Math.cos(ang) * 10.0D, near.y, near.z + Math.sin(ang) * 10.0D);
        this.level.addFreshEntity(minion);
    }

    private void spawnHealingBox() {
        double ang = this.level.random.nextDouble() * Math.PI * 2.0D;
        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(this.level);
        if (box == null) {
            return;
        }
        Vec3 pos = this.boss.position();
        box.setPos(pos.x + Math.cos(ang) * 8.0D, pos.y, pos.z + Math.sin(ang) * 8.0D);
        box.setHealTarget(this.boss);
        this.level.addFreshEntity(box);
    }

    private void announce(ChatFormatting color, String extra, String message) {
        for (Player p : this.level.getEntitiesOfClass(Player.class, this.boss.getBoundingBox().inflate(96.0D))) {
            p.sendSystemMessage(Component.literal(color + extra + message));
        }
    }
}
