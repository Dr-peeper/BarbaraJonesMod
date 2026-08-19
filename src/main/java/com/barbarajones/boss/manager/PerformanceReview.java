package com.barbarajones.boss.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * One player's personnel file during The Manager's fight: how many times they
 * have been Written Up, which task is currently on the clock, and how far
 * through it they are.
 *
 * <p>The "Written Up" debuff is deliberately built from vanilla effects with a
 * scaling amplifier rather than a registered custom MobEffect. A custom effect
 * would need a new registry (a shared file this subsystem must not touch) and a
 * lang key, and it would buy nothing the action bar readout below doesn't
 * already give the player.
 */
public class PerformanceReview {

    /** File this many write-ups and the Manager terminates you with cause. */
    public static final int TERMINATION_THRESHOLD = 5;

    private static final int REAPPLY_INTERVAL = 20;
    private static final double LAP_DISTANCE = 20.0D;
    private static final int REQUIRED_JUMPS = 3;
    private static final double OFFICE_RANGE_SQR = 4.0D * 4.0D;
    private static final double EYE_CONTACT_DOT = 0.93D;

    private int writeUps;

    @Nullable
    private ReviewTask task;
    private int deadline;

    // per-task progress
    private int hold;
    private int jumps;
    private boolean wasOnGround = true;
    private double travelled;
    private double lastX;
    private double lastZ;
    private boolean haveLastPos;

    /** Ticks until the Manager picks this player for another review. */
    private int nextReviewDelay;

    public int getWriteUps() {
        return this.writeUps;
    }

    public void setWriteUps(int value) {
        this.writeUps = Math.max(0, value);
    }

    public boolean hasTask() {
        return this.task != null;
    }

    public int getNextReviewDelay() {
        return this.nextReviewDelay;
    }

    public void setNextReviewDelay(int ticks) {
        this.nextReviewDelay = ticks;
    }

    public void addWriteUp() {
        this.writeUps++;
    }

    public void clearWriteUp() {
        this.writeUps = Math.max(0, this.writeUps - 1);
    }

    public void clearAll() {
        this.writeUps = 0;
        this.task = null;
        this.deadline = 0;
    }

    /** Hands the player a fresh order and resets every progress counter. */
    public void assign(ReviewTask newTask) {
        this.task = newTask;
        this.deadline = newTask.deadlineTicks();
        this.hold = 0;
        this.jumps = 0;
        this.travelled = 0.0D;
        this.haveLastPos = false;
        this.wasOnGround = true;
    }

    public void abandonTask() {
        this.task = null;
        this.deadline = 0;
    }

    /** What happened to the active task on this tick. */
    public enum Outcome { NONE, PASSED, FAILED }

    /**
     * Advances the active task by one tick and keeps the Written Up debuff
     * topped up. Returns whether the task just finished one way or the other.
     */
    public Outcome tick(ServerPlayer player, TheManager manager) {
        if (this.nextReviewDelay > 0) {
            this.nextReviewDelay--;
        }
        reapplyDebuff(player);

        if (this.task == null) {
            if (this.writeUps > 0 && player.tickCount % 10 == 0) {
                showStatus(player, "no task assigned - stand by");
            }
            return Outcome.NONE;
        }

        boolean done = advance(player, manager);
        this.deadline--;

        if (done) {
            this.task = null;
            this.deadline = 0;
            return Outcome.PASSED;
        }
        if (this.deadline <= 0) {
            this.task = null;
            return Outcome.FAILED;
        }
        if (player.tickCount % 4 == 0) {
            showStatus(player, this.task.order() + "  [" + progressLabel() + "]  "
                    + (this.deadline / 20) + "s left");
        }
        return Outcome.NONE;
    }

    private boolean advance(ServerPlayer player, TheManager manager) {
        return switch (this.task) {
            case SIT_DOWN -> holdWhile(player.isCrouching());
            case HANDS_EMPTY -> holdWhile(player.getMainHandItem().isEmpty()
                    && player.getOffhandItem().isEmpty());
            case STEP_INTO_MY_OFFICE -> holdWhile(player.distanceToSqr(manager) < OFFICE_RANGE_SQR);
            case EYE_CONTACT -> {
                Vec3 toBoss = manager.getEyePosition().subtract(player.getEyePosition());
                yield toBoss.lengthSqr() < 1.0E-4D
                        || player.getLookAngle().dot(toBoss.normalize()) > EYE_CONTACT_DOT
                        ? holdWhile(true) : holdWhile(false);
            }
            case TAKE_A_LAP -> {
                if (this.haveLastPos) {
                    double dx = player.getX() - this.lastX;
                    double dz = player.getZ() - this.lastZ;
                    this.travelled += Math.sqrt(dx * dx + dz * dz);
                }
                this.lastX = player.getX();
                this.lastZ = player.getZ();
                this.haveLastPos = true;
                yield this.travelled >= LAP_DISTANCE;
            }
            case JUMP_TO_IT -> {
                // count the leaving-the-ground edge, not the airborne state, or a
                // single hop off a ledge would fill the whole quota
                boolean onGround = player.onGround();
                if (this.wasOnGround && !onGround && player.getDeltaMovement().y > 0.0D) {
                    this.jumps++;
                }
                this.wasOnGround = onGround;
                yield this.jumps >= REQUIRED_JUMPS;
            }
        };
    }

    private boolean holdWhile(boolean compliant) {
        this.hold = compliant ? this.hold + 1 : 0;
        return this.task != null && this.hold >= this.task.holdTicks();
    }

    private String progressLabel() {
        if (this.task == null) {
            return "";
        }
        return switch (this.task) {
            case TAKE_A_LAP -> (int) this.travelled + "/" + (int) LAP_DISTANCE + " blocks";
            case JUMP_TO_IT -> this.jumps + "/" + REQUIRED_JUMPS + " jumps";
            default -> String.format("%.1f/%.1fs",
                    this.hold / 20.0F, this.task.holdTicks() / 20.0F);
        };
    }

    private void showStatus(ServerPlayer player, String detail) {
        ChatFormatting colour = this.writeUps >= TERMINATION_THRESHOLD - 1
                ? ChatFormatting.DARK_RED : ChatFormatting.RED;
        player.displayClientMessage(Component
                .literal("WRITTEN UP x" + this.writeUps + "  -  " + detail)
                .withStyle(colour), true);
    }

    /**
     * Refreshed on a short duration so the debuff simply lapses a second after
     * the boss dies - no cleanup pass needed, and no way to end the fight still
     * crippled because something skipped a removal call.
     */
    private void reapplyDebuff(ServerPlayer player) {
        if (this.writeUps <= 0 || player.tickCount % REAPPLY_INTERVAL != 0) {
            return;
        }
        int stacks = this.writeUps;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40,
                Math.min(stacks - 1, 3), false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40,
                Math.min(stacks - 1, 2), false, false, true));
        if (stacks >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40,
                    stacks - 3, false, false, true));
        }
        if (stacks >= 4) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60,
                    0, false, false, true));
        }
    }
}
