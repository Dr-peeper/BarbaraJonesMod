package com.barbarajones.apocalypse;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.GiantKraveBox;
import com.barbarajones.entity.KraveMeteor;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.KraveTornado;
import com.barbarajones.entity.SkyCinematic;
import com.barbarajones.net.ModNetwork;
import com.barbarajones.net.PacketApocalypse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The STAGED Krave Apocalypse. Every pet death raises the stage (cycling 1..10),
 * each matched to one uncanny face and one beat of video canon:
 *
 *   1 The Craving  2 The O's  3 The Spilled Pibb  4 Diced  5 The Torching
 *   6 Krave Rain   7 The Interview  8 Boxfall  9 The Broadcast
 *   10 KRAVE ARMAGEDDON
 *
 * The owner is damage-immune throughout and gets pinned at a sky vantage to
 * watch. Pets can't be hurt by the apocalypse itself - no death loops.
 */
public class KraveApocalypse {

    private static final List<KraveApocalypse> ACTIVE = new ArrayList<>();

    /**
     * ENDLESS MODE. Armed permanently on a player's eleventh pet death. From then
     * on every apocalypse re-arms itself the moment it finishes: the sky never
     * clears, the meteors never stop, the world simply keeps ending.
     */
    private static boolean endless = false;
    private static int endlessCooldown = 0;

    public static void setEndless(boolean on) {
        endless = on;
    }

    public static boolean isEndless() {
        return endless;
    }

    public static final String[] STAGE_NAME = {
        "The Craving", "The O's", "The Spilled Pibb", "Diced", "The Torching",
        "Krave Rain", "The Interview", "Boxfall", "The Broadcast", "KRAVE ARMAGEDDON"
    };

    private final ServerLevel level;
    private final Vec3 pos;
    @Nullable private final LivingEntity killer;
    @Nullable private final Player owner;
    private final boolean isCayden;
    private final int kraveFed;
    private final boolean kraveRage;
    private final int stage;

    private int t = 0;
    private int collected = 0;
    private final int wrathEnd;
    private final int totalDur;

    private Vec3 vantage;
    private boolean vantageSet;

    private KraveApocalypse(ServerLevel level, Vec3 pos, @Nullable LivingEntity killer,
                            @Nullable Player owner, boolean isCayden, int kraveFed,
                            boolean kraveRage, int stage) {
        this.level = level; this.pos = pos; this.killer = killer; this.owner = owner;
        this.isCayden = isCayden; this.kraveFed = kraveFed; this.kraveRage = kraveRage;
        this.stage = Math.max(1, Math.min(10, stage));

        if (this.stage >= 6) {
            this.wrathEnd = 200 + this.stage * 30;
            this.totalDur = this.wrathEnd + 78;
        } else {
            this.wrathEnd = 0;
            this.totalDur = 60 + this.stage * 26;
        }
    }

    public static void start(ServerLevel level, Vec3 pos, @Nullable LivingEntity killer,
                             @Nullable Player owner, boolean isCayden, int kraveFed,
                             boolean kraveRage, int stage) {
        if (isActiveNear(level, pos)) {
            return;   // one at a time: without this, the respawned pet dying loops forever
        }
        ACTIVE.add(new KraveApocalypse(level, pos, killer, owner, isCayden, kraveFed, kraveRage, stage));
    }

    /** True if an apocalypse is running near this position. */
    public static boolean isActiveNear(Level level, Vec3 pos) {
        for (KraveApocalypse a : ACTIVE) {
            if (a.level == level && a.pos.distanceToSqr(pos) < 300.0D * 300.0D) {
                return true;
            }
        }
        return false;
    }

    public static void tickAll() {
        if (endlessCooldown > 0) {
            endlessCooldown--;
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (KraveApocalypse a : new ArrayList<>(ACTIVE)) {
            boolean done;
            try {
                done = a.tick();
            } catch (Throwable err) {
                done = true;   // never let the cutscene wedge the server
            }
            if (done) {
                a.send(PacketApocalypse.PHASE_END, 0);   // always end cleanly
                ACTIVE.remove(a);
                if (endless) {
                    a.rearm();
                }
            }
        }
    }

    /** ENDLESS MODE: the moment one apocalypse ends, the next one begins. */
    private void rearm() {
        if (this.owner == null || !this.owner.isAlive() || endlessCooldown > 0) {
            return;
        }
        endlessCooldown = 60;   // one breath. that is all you get.
        Vec3 next = this.owner.position();
        ACTIVE.add(new KraveApocalypse(this.level, next, this.killer, this.owner,
                this.isCayden, this.kraveFed, this.kraveRage, 10));
        this.owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED
                + "It is not over. It is never over."));
    }

    private void send(int phase, int target) {
        ModNetwork.sendAround(this.level, this.pos, 260.0D,
                new PacketApocalypse(phase, this.stage, target, this.pos.x, this.pos.y, this.pos.z));
    }

    // ---- tick ---------------------------------------------------------------

    private boolean tick() {
        this.t++;
        if (this.t == 1) {
            onset();
        }

        if (this.stage >= 6) {
            tickWrath();
        } else {
            tickVignette();
        }

        if (this.owner != null && this.owner.isAlive()) {
            this.owner.fallDistance = 0.0F;
            this.owner.clearFire();
            if (this.t % 100 == 0) {
                this.owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, 4));
                this.owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 260));
            }
            pinAtVantage();
        }

        if (this.t >= this.totalDur) {
            respawnPet();
            return true;
        }
        return false;
    }

    /** The front-row seat: lift the owner to a fixed sky vantage and hold them. */
    private void pinAtVantage() {
        boolean window = this.stage >= 6
                ? (this.t >= 4 && this.t <= this.wrathEnd - 6)
                : (this.t >= 4 && this.t <= this.totalDur - 8);
        if (!window || this.stage == 1 || this.owner == null) {
            return;
        }
        if (!this.vantageSet) {
            Vec3 away = this.owner.position().subtract(this.pos);
            double len = Math.sqrt(away.x * away.x + away.z * away.z);
            if (len < 1.0D) {
                away = new Vec3(1.0D, 0.0D, 0.4D);
                len = 1.08D;
            }
            this.vantage = new Vec3(this.pos.x + away.x / len * 38.0D,
                    this.pos.y + 26.0D, this.pos.z + away.z / len * 38.0D);
            this.vantageSet = true;
            this.owner.teleportTo(this.vantage.x, this.vantage.y, this.vantage.z);
        }
        this.owner.setDeltaMovement(Vec3.ZERO);
        this.owner.hurtMarked = true;
        if (this.owner.position().distanceToSqr(this.vantage) > 9.0D) {
            this.owner.teleportTo(this.vantage.x, this.vantage.y, this.vantage.z);
        }
    }

    /** Stages 1-5: short escalating vignettes, each with its sky cinematic. */
    private void tickVignette() {
        if (this.stage == 2 && this.t == 8) {
            cinematic(SkyCinematic.O_BLOWER, this.pos.add(10, 22, 14), this.totalDur - 12);
        }
        if (this.stage >= 3 && this.t == 8) {
            cinematic(SkyCinematic.POURER, this.pos.add(-12, 26, 12), this.totalDur - 12);
        }
        if (this.stage >= 3 && this.t >= 20 && this.t < this.totalDur - 25 && this.t % 9 == 0) {
            drop(KraveMeteor.TYPE_PIBB, 2, 40.0D);
        }
        if (this.stage >= 4 && this.t % 6 == 0 && this.t < this.totalDur - 25) {
            eruptGrass(6, 34.0D);
        }
        if (this.stage >= 4 && this.t >= 24 && this.t < this.totalDur - 25 && this.t % 11 == 0) {
            drop(KraveMeteor.TYPE_KNIFE, 1 + this.level.random.nextInt(2), 46.0D);
        }
        if (this.stage >= 4 && this.t == 30) {
            cinematic(SkyCinematic.CLEAVER, this.pos.add(0, 55, 0), 200);
        }
        if (this.stage >= 5) {
            if (this.t == 15) {
                launchBarbaraSkyward();
            }
            if (this.t == 42) {
                cinematic(SkyCinematic.TORCHER, this.pos.add(6, 34, -8), this.totalDur - 50);
                this.level.playSound(null, BlockPos.containing(this.pos),
                        net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            if (this.t >= 55 && this.t < this.totalDur - 20 && this.t % 8 == 0) {
                drop(KraveMeteor.TYPE_GRASS, 2 + this.level.random.nextInt(2), 52.0D);
            }
        }
        if (this.stage >= 2 && this.t == this.totalDur - 12) {
            this.level.playSound(null, BlockPos.containing(this.pos), ModSounds.KRAVE_BOOM.get(),
                    SoundSource.HOSTILE, 2.0F, 1.1F);
            this.level.explode(null, this.pos.x, this.pos.y, this.pos.z, 2.0F + this.stage,
                    this.stage >= 4 ? ExplosionInteraction.MOB : ExplosionInteraction.NONE);
            send(PacketApocalypse.PHASE_BLAST, 0);
        }
    }

    /** Stages 6-10: the full wrath - gates, giants, rain of doom, finale. */
    private void tickWrath() {
        if (this.t == 20) {
            cinematic(SkyCinematic.O_BLOWER, this.pos.add(-22, 30, -16), this.wrathEnd - 60);
            cinematic(SkyCinematic.POURER, this.pos.add(22, 30, 16), this.wrathEnd - 60);
        }
        if (this.t == 40) {
            cinematic(SkyCinematic.CLEAVER, this.pos.add(0, 55, 0), 200);
        }
        if (this.stage >= 8 && this.t == 60) {
            cinematic(SkyCinematic.TORCHER, this.pos.add(0, 40, -26), this.wrathEnd - 90);
        }
        if (this.stage >= 7 && this.t == 30) {
            double ang = this.level.random.nextDouble() * Math.PI * 2.0D;
            SkyCinematic manager = cinematic(SkyCinematic.MANAGER,
                    this.pos.add(Math.cos(ang) * 75.0D, 0, Math.sin(ang) * 75.0D), this.wrathEnd + 40);
            if (manager != null) {
                manager.walkTo(this.pos.x, this.pos.z);
            }
            for (Player p : nearbyPlayers()) {
                p.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                        + "Daniel: \"I'm calling the manager. The INTERNET manager.\""));
            }
        }
        if (this.stage >= 9 && this.t % 11 == 0 && this.t <= this.wrathEnd - 45) {
            drop(KraveMeteor.TYPE_GATORADE, 2, 80.0D);
        }
        if (this.t >= 10 && this.t <= this.wrathEnd - 45 && this.t % 7 == 0) {
            wrathWave();
        }
        int boltEvery = this.stage >= 10 ? 25 : 40;
        if (this.stage >= 9 && this.t % boltEvery == 0 && this.t <= this.wrathEnd - 45) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level);
            if (bolt != null) {
                bolt.moveTo(this.pos.x + (this.level.random.nextDouble() - 0.5D) * 90.0D, this.pos.y,
                        this.pos.z + (this.level.random.nextDouble() - 0.5D) * 90.0D);
                this.level.addFreshEntity(bolt);
            }
        }
        if (this.stage >= 9 && this.t % 13 == 0 && this.t <= this.wrathEnd - 45) {
            drop(KraveMeteor.TYPE_GRASS, 2, 70.0D);
        }

        if (this.t == this.wrathEnd - 30) { dropTheBox(); }
        if (this.t == this.wrathEnd - 6)  { blast(); }
        if (this.t == this.wrathEnd - 2)  { startTornado(); }
        if (this.t >= this.wrathEnd && this.t <= this.wrathEnd + 16) { tornado(); }
        if (this.t == this.wrathEnd + 18) { apexThrow(); }
        if (this.t == this.wrathEnd + 42) { startMissile(); }
        if (this.t >= this.wrathEnd + 42 && this.t <= this.wrathEnd + 70) { missileTick(); }
    }

    private void wrathWave() {
        int mult = this.stage >= 10 ? 2 : 1;
        drop(KraveMeteor.TYPE_METEOR, (1 + this.level.random.nextInt(3)) * mult, 80.0D);
        if (this.stage >= 8 && this.level.random.nextBoolean()) {
            drop(KraveMeteor.TYPE_BOX, mult, 70.0D);
        }
        if (this.stage >= 9 && this.level.random.nextBoolean()) {
            drop(KraveMeteor.TYPE_KNIFE, 1, 70.0D);
        }
        if (this.stage >= 10) {
            drop(KraveMeteor.TYPE_GRASS, 2, 90.0D);
        }
    }

    // ---- beats --------------------------------------------------------------

    private void onset() {
        send(PacketApocalypse.PHASE_ONSET, 0);
        if (this.stage >= 6) {
            send(PacketApocalypse.PHASE_WRATH, 0);
        }

        var stageClip = new net.minecraft.sounds.SoundEvent[] {
            null, ModSounds.EVT_OG.get(), ModSounds.EVT_CHEPINA.get(), ModSounds.EVT_ROLL.get(),
            ModSounds.EVT_LIGHTER.get(), ModSounds.EVT_MUSIC.get(), ModSounds.EVT_MANAGER.get(),
            ModSounds.EVT_MCD.get(), ModSounds.EVT_DEMOCRAT.get(), ModSounds.EVT_HOUSE.get()
        };

        for (Player p : nearbyPlayers()) {
            if (stageClip[this.stage - 1] != null) {
                this.level.playSound(null, p.blockPosition(), stageClip[this.stage - 1],
                        SoundSource.MASTER, 1.3F, 1.0F);
            }
            this.level.playSound(null, p.blockPosition(), ModSounds.KRAVE_LAUGH.get(),
                    SoundSource.MASTER, 1.0F, this.stage <= 2 ? 1.1F : 0.8F);
            if (this.stage >= 2) {
                this.level.playSound(null, p.blockPosition(), ModSounds.KRAVE_VOICE.get(), SoundSource.MASTER, 1.0F, 1.0F);
            }
            if (this.stage >= 3) {
                this.level.playSound(null, p.blockPosition(), ModSounds.KRAVE_SCREECH.get(), SoundSource.MASTER, 0.9F, 1.0F);
            }
            if (this.stage >= 5) {
                this.level.playSound(null, p.blockPosition(), ModSounds.KRAVE_RUMBLE.get(), SoundSource.MASTER, 1.2F, 1.0F);
            }
            if (this.stage >= 6) {
                this.level.playSound(null, p.blockPosition(), ModSounds.KRAVE_SIREN.get(), SoundSource.MASTER, 1.4F, 1.0F);
            }
            p.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED + "" + ChatFormatting.BOLD
                    + "KRAVE DEATH " + roman(this.stage) + ChatFormatting.RESET + ChatFormatting.RED
                    + " - " + STAGE_NAME[this.stage - 1]));
            if (this.stage == 9) {
                p.sendSystemMessage(Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD
                        + "Barbara: \"YOU GUYS BE F#$%ING UP AMERICA!!\""));
            }
        }

        if (this.stage >= 3) {
            this.level.setWeatherParameters(0, this.totalDur + 60, true, true);
        }
        if (this.owner != null) {
            this.owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 4));
            this.owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400));
        }
    }

    private void dropTheBox() {
        GiantKraveBox box = ModEntities.GIANT_BOX.get().create(this.level);
        if (box != null) {
            box.setPos(this.pos.x + 0.5D, this.pos.y + 34.0D, this.pos.z + 0.5D);
            this.level.addFreshEntity(box);
        }
    }

    private void blast() {
        send(PacketApocalypse.PHASE_BLAST, 0);
        this.level.playSound(null, BlockPos.containing(this.pos), ModSounds.KRAVE_BOOM.get(),
                SoundSource.HOSTILE, 4.0F, 1.0F);
        this.level.explode(null, this.pos.x, this.pos.y, this.pos.z, 10.0F, ExplosionInteraction.MOB);
    }

    private void startTornado() {
        if (this.owner == null) {
            return;
        }
        KraveTornado tornado = ModEntities.TORNADO.get().create(this.level);
        if (tornado != null) {
            tornado.setPos(this.owner.getX(), this.owner.getY() - 1.5D, this.owner.getZ());
            this.level.addFreshEntity(tornado);
        }
        this.level.playSound(null, this.owner.blockPosition(), ModSounds.KRAVE_TORNADO.get(),
                SoundSource.MASTER, 1.4F, 1.0F);
    }

    private void tornado() {
        if (this.owner == null || !this.owner.isAlive()) {
            return;
        }
        double ang = this.t * 0.9D;
        this.owner.setDeltaMovement(Math.cos(ang) * 0.5D, 1.15D, Math.sin(ang) * 0.5D);
        this.owner.setYRot(this.owner.getYRot() + 45.0F);
        this.owner.hurtMarked = true;
    }

    private void apexThrow() {
        if (this.owner == null || !this.owner.isAlive()) {
            return;
        }
        this.level.playSound(null, this.owner.blockPosition(),
                net.minecraft.sounds.SoundEvents.IRON_GOLEM_ATTACK, SoundSource.MASTER, 1.4F, 0.6F);
        Vec3 target = (this.killer != null && this.killer.isAlive())
                ? this.killer.position() : this.pos;
        Vec3 d = target.subtract(this.owner.position());
        double len = d.length();
        if (len > 0.01D) {
            this.owner.setDeltaMovement(d.scale(2.4D / len).add(0.0D, 0.2D, 0.0D));
            this.owner.hurtMarked = true;
        }
        this.owner.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE
                + "Cayden hurls you at your killer!"));
    }

    private void startMissile() {
        if (this.owner != null) {
            send(PacketApocalypse.PHASE_MISSILE, this.owner.getId());
            this.owner.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                    + "Barbara turns you into a grass-seeking missile!"));
        }
    }

    private void missileTick() {
        if (this.owner == null || !this.owner.isAlive()) {
            return;
        }
        final int R = 24;
        BlockPos o = this.owner.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int gx = -R; gx <= R; gx++) {
            for (int gz = -R; gz <= R; gz++) {
                for (int gy = -6; gy <= 6; gy++) {
                    BlockPos p = o.offset(gx, gy, gz);
                    var state = this.level.getBlockState(p);
                    if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
                        double d = p.distSqr(o);
                        if (d < bestD) {
                            bestD = d;
                            best = p;
                        }
                    }
                }
            }
        }
        if (best == null) {
            return;
        }
        Vec3 to = Vec3.atCenterOf(best).subtract(this.owner.position());
        double len = to.length();
        if (len > 0.01D) {
            this.owner.setDeltaMovement(to.scale(1.6D / len));
            this.owner.hurtMarked = true;
        }
        if (bestD < 4.0D) {
            this.level.removeBlock(best, false);
            this.collected++;
        }
    }

    private void respawnPet() {
        if (this.collected > 0) {
            BarbaraJones barbara = nearestPetBarbara();
            if (barbara != null) {
                barbara.addGrassStash(this.collected * 40);
            } else if (this.owner != null) {
                this.owner.getInventory().add(
                        new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), Math.min(64, this.collected)));
            }
        }
        if (this.owner == null) {
            return;
        }

        Vec3 sp = this.owner.position().add(0.0D, 1.0D, 0.0D);
        this.level.playSound(null, BlockPos.containing(sp), ModSounds.KRAVE_SPAWN.get(),
                SoundSource.MASTER, 1.0F, 1.0F);

        if (this.isCayden) {
            CaydenCobb c = ModEntities.CAYDEN.get().create(this.level);
            if (c != null) {
                c.setPos(sp.x, sp.y, sp.z);
                c.tame(this.owner);
                this.level.addFreshEntity(c);
                c.restoreKrave(this.kraveFed, this.kraveRage);
                // 30s untouchable: he lands out of the sky into a burning crater,
                // and one fall death here restarts the entire apocalypse.
                c.grantGrace(CaydenCobb.GRACE_TICKS);
                if (this.owner != null) {
                    this.owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            net.minecraft.ChatFormatting.AQUA
                            + "Cayden is back, and untouchable for 30 seconds. Get him somewhere safe."));
                }
            }
        } else {
            BarbaraJones b = ModEntities.BARBARA.get().create(this.level);
            if (b != null) {
                b.setPos(sp.x, sp.y, sp.z);
                b.setPet(this.owner);
                this.level.addFreshEntity(b);
                b.grantGrace(BarbaraJones.GRACE_TICKS);
            }
        }

        // STAGE 10: the Krave Monster himself crawls out of the ruin.
        if (this.stage >= 10) {
            KraveMonster monster = ModEntities.KRAVE_MONSTER.get().create(this.level);
            if (monster != null) {
                monster.setPos(this.pos.x, this.pos.y + 1.0D, this.pos.z);
                this.level.addFreshEntity(monster);
                monster.setTarget(this.owner);
                this.owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE + ""
                        + ChatFormatting.BOLD + "HE HAS COME THROUGH THE GATES."));
            }
        }
    }

    // ---- helpers ------------------------------------------------------------

    @Nullable
    private SkyCinematic cinematic(byte kind, Vec3 at, int life) {
        SkyCinematic c = ModEntities.SKY_CINEMATIC.get().create(this.level);
        if (c == null) {
            return null;
        }
        c.kind(kind).lifespan(life);
        c.setPos(at.x, at.y, at.z);
        this.level.addFreshEntity(c);
        return c;
    }

    private void drop(byte kind, int count, double radius) {
        for (int i = 0; i < count; i++) {
            KraveMeteor m = ModEntities.METEOR.get().create(this.level);
            if (m == null) {
                continue;
            }
            m.kind(kind);
            m.setPos(this.pos.x + (this.level.random.nextDouble() - 0.5D) * 2.0D * radius,
                    this.pos.y + 65.0D + this.level.random.nextInt(24),
                    this.pos.z + (this.level.random.nextDouble() - 0.5D) * 2.0D * radius);
            m.aim((this.level.random.nextDouble() - 0.5D) * 0.7D,
                    (this.level.random.nextDouble() - 0.5D) * 0.7D);
            this.level.addFreshEntity(m);
        }
    }

    /** Stage 4: tall grass claws its way up out of the ground. */
    private void eruptGrass(int count, double radius) {
        for (int i = 0; i < count; i++) {
            int gx = (int) Math.floor(this.pos.x + (this.level.random.nextDouble() - 0.5D) * 2.0D * radius);
            int gz = (int) Math.floor(this.pos.z + (this.level.random.nextDouble() - 0.5D) * 2.0D * radius);
            for (int gy = (int) this.pos.y + 6; gy > (int) this.pos.y - 6; gy--) {
                BlockPos p = new BlockPos(gx, gy, gz);
                if (this.level.getBlockState(p).isAir()
                        && this.level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)) {
                    this.level.setBlockAndUpdate(p, Blocks.GRASS.defaultBlockState());
                    if (this.level.random.nextInt(4) == 0) {
                        this.level.playSound(null, p, net.minecraft.sounds.SoundEvents.GRASS_BREAK,
                                SoundSource.BLOCKS, 0.7F, 0.6F);
                    }
                    break;
                }
            }
        }
    }

    /** Stage 5 canon: Cayden sends Barbara into the sky to torch the stash. */
    private void launchBarbaraSkyward() {
        BarbaraJones barbara = nearestPetBarbara();
        if (barbara == null && this.owner != null) {
            barbara = ModEntities.BARBARA.get().create(this.level);
            if (barbara != null) {
                barbara.setPos(this.pos.x, this.pos.y, this.pos.z);
                barbara.setPet(this.owner);
                this.level.addFreshEntity(barbara);
            }
        }
        if (barbara != null) {
            barbara.setDeltaMovement(barbara.getDeltaMovement().x, 2.6D, barbara.getDeltaMovement().z);
            barbara.hurtMarked = true;
            this.level.playSound(null, barbara.blockPosition(),
                    net.minecraft.sounds.SoundEvents.IRON_GOLEM_ATTACK, SoundSource.MASTER, 1.4F, 0.5F);
        }
        if (this.owner != null) {
            this.owner.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                    + "Cayden hurls Barbara into the sky. She's torching the stash up there..."));
        }
    }

    private static String roman(int n) {
        String[] r = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        return r[Math.max(0, Math.min(9, n - 1))];
    }

    private List<Player> nearbyPlayers() {
        return this.level.getEntitiesOfClass(Player.class,
                new AABB(this.pos, this.pos).inflate(200.0D));
    }

    @Nullable
    private BarbaraJones nearestPetBarbara() {
        for (BarbaraJones b : this.level.getEntitiesOfClass(BarbaraJones.class,
                new AABB(this.pos, this.pos).inflate(64.0D, 32.0D, 64.0D))) {
            if (b.isPet()) {
                return b;
            }
        }
        return null;
    }
}
