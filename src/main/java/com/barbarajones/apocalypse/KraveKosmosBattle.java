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
            return true;
        }
        if (!this.cayden.isAlive() || !this.cayden.isSuperSaiyan()) {
            this.boss.setBossFightActive(false);
            announce(ChatFormatting.RED, "", "Cayden powers down. The fight is over - for now.");
            return true;
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

    private int countNearby(Class<? extends net.minecraft.world.entity.Entity> type) {
        return this.level.getEntitiesOfClass(type, this.boss.getBoundingBox().inflate(80.0D)).size();
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
