package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.Config;
import com.barbarajones.content.ModSounds;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The Dread director - ambient, client-side horror that runs during NORMAL play,
 * not the apocalypse. It borrows the tricks the best horror films and games lean
 * on: dread beats jump-scares, the wait is worse than the hit, and the room being
 * subtly wrong is scarier than a monster in your face.
 *
 * <p>An "unease" meter rises when you're in genuinely tense conditions - dark,
 * night, underground, alone, or something from this mod standing too close (worse
 * if it's <em>behind</em> you) - and decays in daylight and company. Unease drives,
 * with long unpredictable cooldowns so it never settles into a rhythm:
 * <ul>
 *   <li>a <b>heartbeat</b> that quickens with unease;</li>
 *   <li>distant <b>whispers</b> placed <em>behind</em> your head - "did you hear that?";</li>
 *   <li>a <b>subliminal</b> single-flash of an uncanny face - barely there, "did I
 *       just see that?";</li>
 *   <li>creeping <b>paranoia messages</b>;</li>
 *   <li>and, rarely, at the edge of panic, the <b>lights go out</b> for a beat.</li>
 * </ul>
 *
 * <p>It stays comical because every voice and face is pure Krave nonsense. It is
 * cosmetic only - no gameplay effect - and stands fully down during the
 * apocalypse (which has its own show) and whenever {@link Config#ENABLE_DREAD} is off.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class DreadClient {

    private static final ResourceLocation[] FACES = new ResourceLocation[10];
    static {
        for (int i = 0; i < 10; i++) {
            FACES[i] = new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/face_" + (i + 1) + ".png");
        }
    }

    private static final String[] MESSAGES = {
        "You hear the O's forming somewhere behind you.",
        "Barbara: \"...I can smell the grass on you, sugar.\"",
        "Something just moved the ashtray.",
        "Cayden: \"don't look at the box.\"",
        "Did Cayden always stand that close?",
        "The cereal is watching.",
        "Barbara: \"why's it gone so quiet, sugar?\"",
        "You feel a craving you did not have a second ago.",
        "KRAVE.",
        "Somewhere, a box of cereal opens by itself.",
        "You could swear the walls just breathed.",
        "Cayden: \"...i heard mom.\"",
        "Turn around. No - slower.",
        "It knows you are almost out of Krave."
    };

    /** How many ticks after joining a level before dread is allowed to build at all -
     *  lets chunk lighting settle so a fresh spawn doesn't misread as pitch dark. */
    private static final int JOIN_GRACE_TICKS = 100;

    private static float unease = 0.0F;
    private static int stingerCd = 0, subliminalCd = 0, messageCd = 0, blackoutCd = 0;
    private static int heartbeatTimer = 0;
    private static int subliminalTimer = 0, subliminalFace = 0;
    private static int blackoutTimer = 0;
    private static ResourceKey<Level> lastLevel = null;
    private static int joinGrace = 0;
    private static float cachedTarget = 0.0F;
    private static int watcherRefresh = 0;

    private DreadClient() { }

    // ---- driver -------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            tick();
        } catch (Throwable ignored) {
            // atmosphere must never crash the game
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        decayTimers();

        if (!enabled() || mc.player == null || mc.level == null || mc.isPaused()) {
            lastLevel = null;
            unease += (0.0F - unease) * 0.05F;
            if (unease < 0.001F) {
                unease = 0.0F;
            }
            return;
        }

        // A fresh world/dimension join resets dread and holds off building any
        // back up until chunk lighting has had a chance to settle - otherwise
        // unlit spawn chunks read as pitch dark and the vignette can slam in
        // immediately, which is exactly backwards for a "slow build" effect.
        ResourceKey<Level> here = mc.level.dimension();
        if (!here.equals(lastLevel)) {
            lastLevel = here;
            unease = 0.0F;
            joinGrace = JOIN_GRACE_TICKS;
        }
        if (joinGrace > 0) {
            joinGrace--;
            return;
        }

        if (ApocalypseClient.isActive()) {
            unease += (0.15F - unease) * 0.05F;   // keep a low simmer under the show
            return;
        }

        // The Krave Kosmos is dark, solo, and always near a mod entity by
        // design - every ambient-dread heuristic below reads it as maximum
        // tension permanently, which is backwards for a "slow build" effect
        // and just looks like a stuck/broken vignette. It has its own
        // tension already (boss bar, health alarm, KraveHitClient's
        // hit-by-the-boss flash) so ambient Dread stands down here entirely,
        // same as it already does during the apocalypse.
        if (here.equals(com.barbarajones.dimension.KraveDimensions.KRAVE_KOSMOS)) {
            unease += (0.0F - unease) * 0.05F;
            if (unease < 0.001F) {
                unease = 0.0F;
            }
            return;
        }

        // The nearby-entity scan in computeTarget() is the expensive part;
        // the target only feeds a slow lerp anyway, so refreshing it a few
        // times a second instead of every tick is free smoothing, not a
        // behavior change.
        if (--watcherRefresh <= 0) {
            watcherRefresh = 4;
            cachedTarget = computeTarget(mc.player);
        }
        unease = Mth.clamp(unease + (cachedTarget - unease) * 0.02F, 0.0F, 1.0F);
        rollEvents(mc, mc.player);
    }

    private static void decayTimers() {
        if (stingerCd > 0) { stingerCd--; }
        if (subliminalCd > 0) { subliminalCd--; }
        if (messageCd > 0) { messageCd--; }
        if (blackoutCd > 0) { blackoutCd--; }
        if (subliminalTimer > 0) { subliminalTimer--; }
        if (blackoutTimer > 0) { blackoutTimer--; }
    }

    /** How wrong the room feels right now, 0..1. */
    private static float computeTarget(Player p) {
        var lvl = p.level();
        BlockPos pos = p.blockPosition();

        float t = (15 - lvl.getMaxLocalRawBrightness(pos)) / 15.0F * 0.5F;   // darkness
        long day = lvl.getDayTime() % 24000L;
        if (day > 13000L && day < 23000L) {
            t += 0.18F;                                                       // night
        }
        if (p.getY() < 50.0D && lvl.getBrightness(LightLayer.SKY, pos) < 4) {
            t += 0.22F;                                                       // buried
        }
        if (lvl.players().size() <= 1) {
            t += 0.12F;                                                       // alone
        }

        Entity watcher = nearestModEntity(p, 20.0D);
        if (watcher != null) {
            t += 0.22F;
            Vec3 look = p.getViewVector(1.0F);
            Vec3 to = watcher.position().subtract(p.position());
            if (to.lengthSqr() > 0.01D && look.dot(to.normalize()) < -0.1D) {
                t += 0.2F;                                                    // it's BEHIND you
            }
        }
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    private static Entity nearestModEntity(Player p, double r) {
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : p.level().getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(r))) {
            if (e == p) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (key == null || !key.getNamespace().equals(BarbaraJonesMod.MODID)) {
                continue;
            }
            double d = e.distanceToSqr(p);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private static void rollEvents(Minecraft mc, Player p) {
        RandomSource rnd = p.level().random;

        // heartbeat - quickens with unease
        if (unease > 0.45F) {
            if (--heartbeatTimer <= 0) {
                heartbeatTimer = Math.max(8, (int) Mth.lerp(unease, 34.0F, 12.0F));
                playAt(p, ModSounds.KRAVE_RUMBLE.get(), 0.1F + unease * 0.22F, 0.42F);
            }
        }
        // a whisper, placed behind your head
        if (unease > 0.35F && stingerCd <= 0 && rnd.nextFloat() < unease * 0.004F) {
            playBehind(p, whisper(rnd), 0.18F + unease * 0.22F, 0.6F + rnd.nextFloat() * 0.3F);
            stingerCd = 200 + rnd.nextInt(400);
        }
        // subliminal face flash + a short screech
        if (unease > 0.55F && subliminalCd <= 0 && rnd.nextFloat() < unease * 0.0016F) {
            subliminalTimer = 3;
            subliminalFace = rnd.nextInt(10);
            playBehind(p, ModSounds.KRAVE_SCREECH.get(), 0.22F, 1.35F);
            subliminalCd = 600 + rnd.nextInt(900);
        }
        // a paranoia line
        if (unease > 0.45F && messageCd <= 0 && rnd.nextFloat() < unease * 0.0018F) {
            ChatFormatting col = rnd.nextBoolean() ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY;
            p.displayClientMessage(
                    Component.literal(col + MESSAGES[rnd.nextInt(MESSAGES.length)]), rnd.nextBoolean());
            messageCd = 500 + rnd.nextInt(800);
        }
        // the lights go out for a beat
        if (unease > 0.8F && blackoutCd <= 0 && rnd.nextFloat() < 0.0016F) {
            blackoutTimer = 10;
            playAt(p, ModSounds.KRAVE_BOOM.get(), 0.5F, 0.4F);
            blackoutCd = 1200 + rnd.nextInt(1400);
        }
    }

    private static SoundEvent whisper(RandomSource rnd) {
        return switch (rnd.nextInt(7)) {
            case 0 -> ModSounds.KRAVE_VOICE.get();
            case 1 -> ModSounds.KRAVE_LAUGH.get();
            case 2 -> ModSounds.KRAVE_SCREECH.get();
            case 3 -> ModSounds.BARBARA_IDLE.get();
            case 4 -> ModSounds.CAYDEN_IDLE.get();
            case 5 -> ModSounds.EVT_NOTREADY.get();
            default -> ModSounds.KRAVE_RUMBLE.get();
        };
    }

    private static void playAt(Player p, SoundEvent s, float vol, float pitch) {
        p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), s, SoundSource.MASTER, vol, pitch, false);
    }

    private static void playBehind(Player p, SoundEvent s, float vol, float pitch) {
        RandomSource rnd = p.level().random;
        Vec3 look = p.getViewVector(1.0F);
        double dist = 4.0D + rnd.nextDouble() * 4.0D;
        Vec3 base = p.position().subtract(look.scale(dist));
        double sx = base.x + (rnd.nextDouble() - 0.5D) * 3.0D;
        double sy = base.y + 0.5D + rnd.nextDouble() * 1.5D;
        double sz = base.z + (rnd.nextDouble() - 0.5D) * 3.0D;
        p.level().playLocalSound(sx, sy, sz, s, SoundSource.HOSTILE, vol, pitch, false);
    }

    // ---- overlay ------------------------------------------------------------

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (subliminalTimer <= 0 && blackoutTimer <= 0) {
            return;
        }
        if (ApocalypseClient.isActive()) {
            return;
        }
        try {
            render(event.getGuiGraphics());
        } catch (Throwable ignored) {
            // atmosphere must never crash the game
        }
    }

    private static void render(GuiGraphics gfx) {
        int w = gfx.guiWidth(), h = gfx.guiHeight();
        // The closing screen-edge vignette has been removed entirely (reported
        // as still showing up unwantedly, repeatedly, even in the Overworld
        // where it's "working as intended" per the ambient-horror design) -
        // heartbeat/whispers/messages/subliminal-face/blackout (all audio or
        // brief, not a persistent darkening frame) are the atmosphere now.
        // `unease` still drives all of those via rollEvents(), just no
        // longer has a visual effect of its own.
        if (subliminalTimer > 0) {
            drawFace(gfx, FACES[subliminalFace], w, h, 0.16F * (subliminalTimer / 3.0F));
        }
        if (blackoutTimer > 0) {
            int alpha = (int) (Mth.clamp(blackoutTimer / 10.0F, 0.0F, 1.0F) * 235);
            gfx.fill(0, 0, w, h, alpha << 24);
        }
    }

    private static void drawFace(GuiGraphics gfx, ResourceLocation face, int w, int h, float alpha) {
        RenderSystem.enableBlend();
        gfx.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F));
        gfx.blit(face, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static boolean enabled() {
        try {
            return Config.ENABLE_DREAD.get();
        } catch (Throwable t) {
            return true;   // config not loaded yet - default on
        }
    }
}
