package com.barbarajones.v2.build.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureGeometry;
import com.barbarajones.v2.build.item.KraveSchematicItem;
import com.barbarajones.v2.build.item.SchematicInput;
import com.barbarajones.v2.build.net.BuildNetwork;
import com.barbarajones.v2.build.net.PacketRotateSchematic;
import com.barbarajones.v2.build.place.KraveStructure;
import com.barbarajones.v2.build.place.PlacementCheck;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * The ghost: a translucent footprint of the building, drawn where it would
 * land, green where it fits and red where it does not.
 *
 * <p>Entirely client-side and entirely local. The client has the same
 * {@link StructureDef} objects the server does and its own copy of the
 * surrounding blocks, so it runs {@link KraveStructure#check} itself and needs
 * no packets at all. That is why the ghost tracks the crosshair with no lag -
 * and why it agrees with the server, because it is literally the same
 * validation code.
 *
 * <p>What is drawn:
 * <ul>
 *   <li>one translucent tile per footprint column, at the height that column
 *       will end up - green for flat ground, amber where the engine will shave
 *       a hill down, blue where it will pack a hollow up, red for anything
 *       blocked, too steep, or hanging over nothing;</li>
 *   <li>a wireframe of the building's real volume, so the player can see how
 *       tall the thing is before committing;</li>
 *   <li>a bar of bright cyan along the front edge - the side the door is on -
 *       which is what makes the rotate key legible.</li>
 * </ul>
 *
 * <p>Depth testing is off. A preview that hides behind the hill you are about
 * to flatten is not a preview.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GhostPreviewRenderer {

    /** Never re-validate more often than this, however fast the crosshair moves. */
    private static final int MIN_RECHECK_TICKS = 2;
    /** Re-validate at least this often even if nothing appears to have changed - the world moves too. */
    private static final int MAX_RECHECK_TICKS = 10;

    @Nullable
    private static PlacementCheck cached;
    @Nullable
    private static BlockPos cachedAnchor;
    @Nullable
    private static Rotation cachedRotation;
    @Nullable
    private static ResourceLocation cachedStructure;
    private static int sinceRecheck;

    private GhostPreviewRenderer() { }

    // =====================================================================
    // Tracking
    // =====================================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        SchematicInput.tickClientCooldown();

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.screen != null) {
            clear();
            return;
        }

        ItemStack stack = armedStack(player);
        // Drain the key queue whether or not a schematic is in hand, otherwise a
        // press made while holding something else fires later out of nowhere.
        while (BuildKeys.ROTATE.consumeClick()) {
            if (stack != null) {
                BuildNetwork.sendToServer(new PacketRotateSchematic(1));
            }
        }
        if (stack == null) {
            clear();
            return;
        }

        StructureDef def = KraveSchematicItem.structure(stack);
        if (def == null) {
            clear();
            return;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }

        BlockPos anchor = KraveStructure.anchorFor(blockHit);
        Rotation rotation = KraveSchematicItem.rotationFor(player, stack);
        sinceRecheck++;

        boolean moved = !anchor.equals(cachedAnchor) || rotation != cachedRotation
                || !def.id().equals(cachedStructure);
        boolean stale = sinceRecheck >= MAX_RECHECK_TICKS;
        if (cached != null && !stale && (!moved || sinceRecheck < MIN_RECHECK_TICKS)) {
            return;
        }

        cached = KraveStructure.check(level, anchor, rotation, def);
        cachedAnchor = anchor;
        cachedRotation = rotation;
        cachedStructure = def.id();
        sinceRecheck = 0;
    }

    @Nullable
    private static ItemStack armedStack(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof KraveSchematicItem && KraveSchematicItem.armed(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static void clear() {
        cached = null;
        cachedAnchor = null;
        cachedRotation = null;
        cachedStructure = null;
        sinceRecheck = MAX_RECHECK_TICKS;
    }

    // =====================================================================
    // Drawing
    // =====================================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        PlacementCheck check = cached;
        if (check == null || check.spanX() <= 0 || check.spanZ() <= 0) {
            return;
        }
        StructureDef def = check.def();
        if (def == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        drawTiles(check, matrix);
        drawOutline(check, def, matrix);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void drawTiles(PlacementCheck check, Matrix4f matrix) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int originX = check.origin().getX();
        int originZ = check.origin().getZ();
        Direction front = StructureGeometry.front(check.rotation());

        for (int dz = 0; dz < check.spanZ(); dz++) {
            for (int dx = 0; dx < check.spanX(); dx++) {
                float[] colour = colourFor(check.status(dx, dz));
                double x = originX + dx;
                double z = originZ + dz;
                double y = check.previewY(dx, dz) + 0.02;
                quad(buffer, matrix, x + 0.03, y, z + 0.03, x + 0.97, z + 0.97,
                        colour[0], colour[1], colour[2], colour[3]);

                // A bright lip along the front edge so the facing is unmistakable.
                if (isFrontEdge(check, dx, dz, front)) {
                    quad(buffer, matrix, x + 0.03, y + 0.01, z + 0.03, x + 0.97, z + 0.97,
                            0.25F, 0.95F, 1.0F, 0.35F);
                }
            }
        }
        tesselator.end();
    }

    private static boolean isFrontEdge(PlacementCheck check, int dx, int dz, Direction front) {
        switch (front) {
            case SOUTH:
                return dz == check.spanZ() - 1;
            case NORTH:
                return dz == 0;
            case EAST:
                return dx == check.spanX() - 1;
            case WEST:
                return dx == 0;
            default:
                return false;
        }
    }

    private static void drawOutline(PlacementCheck check, StructureDef def, Matrix4f matrix) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.lineWidth(2.5F);
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float r = check.ok() ? 0.35F : 1.0F;
        float g = check.ok() ? 1.0F : 0.25F;
        float b = check.ok() ? 0.45F : 0.22F;

        double x0 = check.origin().getX();
        double z0 = check.origin().getZ();
        double x1 = x0 + check.spanX();
        double z1 = z0 + check.spanZ();
        double y0 = check.baseY() + def.localBounds().minY();
        double y1 = check.baseY() + def.localBounds().maxY() + 1.0;

        box(buffer, matrix, x0, y0, z0, x1, y1, z1, r, g, b, 0.85F);
        // The floor plane, drawn separately, so the build height reads at a glance.
        double floor = check.baseY();
        line(buffer, matrix, x0, floor, z0, x1, floor, z0, r, g, b, 0.55F);
        line(buffer, matrix, x0, floor, z1, x1, floor, z1, r, g, b, 0.55F);
        line(buffer, matrix, x0, floor, z0, x0, floor, z1, r, g, b, 0.55F);
        line(buffer, matrix, x1, floor, z0, x1, floor, z1, r, g, b, 0.55F);

        tesselator.end();
        RenderSystem.lineWidth(1.0F);
    }

    private static float[] colourFor(PlacementCheck.ColumnStatus status) {
        switch (status) {
            case OK:
                return new float[] { 0.30F, 0.95F, 0.40F, 0.30F };
            case CUT:
                return new float[] { 0.95F, 0.78F, 0.25F, 0.32F };
            case FILL:
                return new float[] { 0.30F, 0.70F, 0.95F, 0.32F };
            case BLOCKED:
            case TOO_STEEP:
            case NO_GROUND:
            default:
                return new float[] { 0.95F, 0.20F, 0.18F, 0.40F };
        }
    }

    // ---- primitives --------------------------------------------------------

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             double x0, double y, double z0, double x1, double z1,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x0, (float) y, (float) z0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x0, (float) y, (float) z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x1, (float) y, (float) z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x1, (float) y, (float) z0).color(r, g, b, a).endVertex();
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x0, (float) y0, (float) z0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a).endVertex();
    }

    private static void box(BufferBuilder buffer, Matrix4f matrix,
                            double x0, double y0, double z0, double x1, double y1, double z1,
                            float r, float g, float b, float a) {
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);

        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);

        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }
}
