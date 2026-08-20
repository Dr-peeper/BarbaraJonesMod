package com.barbarajones.v2.village.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.village.KraveVillagerEntity;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Krave Villager rig - built from scratch, not from {@code HumanoidModel}.
 *
 * <p>The silhouette is the point. A vanilla villager reskin reads as a vanilla
 * villager no matter what you paint on it, so this one is shaped like the thing the
 * mod is about: a person wearing a cereal box. The head is a wide, flat-fronted
 * carton with a fold-out top flap; the body is a narrow apron; the arms are stubby;
 * the legs are chunky boots. At sixteen pixels away you can tell it is not a
 * villager, which is the whole test.
 *
 * <h2>Coordinates</h2>
 * Root sits at {@code y = 24} - model space runs downward from there, so every
 * cube above the feet has a negative Y. That is the same convention vanilla's
 * newer models (Frog, Allay, Camel) use, and it keeps the maths readable: the head
 * is at -31 because it is 31 pixels off the ground.
 *
 * <pre>
 *   feet   0
 *   legs   0  .. -11
 *   body  -11 .. -22
 *   head  -22 .. -31   (flap to -32)
 * </pre>
 * Thirty-one pixels is 1.94 blocks, which is what the entity type is sized to.
 *
 * <h2>Texture map (64x64)</h2>
 * <pre>
 *   head  9x9x6  at ( 0,  0)      flap  9x2x7 at (32, 0)
 *   body  8x11x5 at ( 0, 17)
 *   arm L 3x10x3 at (28, 17)      arm R 3x10x3 at (42, 17)
 *   leg L 4x11x4 at ( 0, 34)      leg R 4x11x4 at (18, 34)
 * </pre>
 */
public class KraveVillagerModel extends HierarchicalModel<KraveVillagerEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "krave_villager"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart flap;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;

    public KraveVillagerModel(ModelPart meshRoot) {
        // createBodyLayer nests everything under a "root" part, so the ModelPart
        // handed in here is the MESH root and "body" is its GRANDchild. Reading
        // body straight off it threw "Can.t find part body" and killed the game
        // at model bake, before the title screen.
        this.root = meshRoot.getChild("root");
        ModelPart root = this.root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.flap = this.head.getChild("flap");
        this.armLeft = this.body.getChild("arm_left");
        this.armRight = this.body.getChild("arm_right");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        PartDefinition root = parts.addOrReplaceChild("root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        // Body: a narrow apron, slightly deeper than a vanilla torso so the box
        // head does not look like it is balanced on a stick.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-4.0F, -11.0F, -2.5F, 8.0F, 11.0F, 5.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        // Head: the carton. Wider than it is deep, flat face, sits low on the
        // shoulders like something worn rather than something grown.
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.5F, -9.0F, -3.0F, 9.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));

        // The fold-out top flap. Animated open a crack, which is the one piece of
        // motion that makes the whole thing read as cardboard.
        head.addOrReplaceChild("flap",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.5F, -2.0F, -7.0F, 9.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, -9.0F, 3.0F));

        CubeListBuilder armLeft = CubeListBuilder.create().texOffs(28, 17)
                .addBox(-1.5F, -1.5F, -1.5F, 3.0F, 10.0F, 3.0F);
        CubeListBuilder armRight = CubeListBuilder.create().texOffs(42, 17)
                .addBox(-1.5F, -1.5F, -1.5F, 3.0F, 10.0F, 3.0F);
        body.addOrReplaceChild("arm_left", armLeft, PartPose.offset(4.75F, -9.5F, 0.0F));
        body.addOrReplaceChild("arm_right", armRight, PartPose.offset(-4.75F, -9.5F, 0.0F));

        CubeListBuilder legLeft = CubeListBuilder.create().texOffs(0, 34)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F);
        CubeListBuilder legRight = CubeListBuilder.create().texOffs(18, 34)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F);
        root.addOrReplaceChild("leg_left", legLeft, PartPose.offset(2.0F, -11.0F, 0.0F));
        root.addOrReplaceChild("leg_right", legRight, PartPose.offset(-2.0F, -11.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(KraveVillagerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        resetPose();

        if (entity.isSleeping()) {
            // The renderer already tips the whole entity onto the bed; all this has
            // to do is stop the head craning and tuck the limbs in, otherwise a
            // sleeping villager looks like it is standing up sideways.
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.armLeft.xRot = -0.35F;
            this.armRight.xRot = -0.35F;
            this.legLeft.xRot = -0.15F;
            this.legRight.xRot = -0.15F;
            this.flap.xRot = -0.25F;
            return;
        }

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);

        // Walk cycle. The 1.15 amplitude on stubby legs reads as a bustle rather
        // than a stride, which suits a village of people who are all late.
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.15F * limbSwingAmount;
        this.legLeft.xRot = swing;
        this.legRight.xRot = -swing;
        this.armLeft.xRot = -swing * 0.75F;
        this.armRight.xRot = swing * 0.75F;

        // Arms hang slightly out from the apron and sway a little at rest.
        float idle = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.armLeft.zRot = 0.08F + idle;
        this.armRight.zRot = -0.08F - idle;

        // The flap breathes open and shut. Cardboard, not chitin.
        this.flap.xRot = -0.18F + Mth.sin(ageInTicks * 0.09F) * 0.07F;

        // Working: the swing the use-building goal triggers.
        if (this.attackTime > 0.0F) {
            float attack = Mth.sin(this.attackTime * (float) Math.PI);
            this.armRight.xRot = -1.4F * attack;
            this.armLeft.xRot = -0.5F * attack;
        }

        // A villager stuffed with Krave stands a little prouder and a little wider.
        float fed = Math.min(1.0F, entity.getKraveFed() / 24.0F);
        this.body.xScale = 1.0F + fed * 0.12F;
        this.body.zScale = 1.0F + fed * 0.12F;
    }

    private void resetPose() {
        this.head.xRot = 0.0F;
        this.head.yRot = 0.0F;
        this.head.zRot = 0.0F;
        this.armLeft.xRot = 0.0F;
        this.armLeft.zRot = 0.0F;
        this.armRight.xRot = 0.0F;
        this.armRight.zRot = 0.0F;
        this.legLeft.xRot = 0.0F;
        this.legRight.xRot = 0.0F;
        this.body.xScale = 1.0F;
        this.body.zScale = 1.0F;
    }
}
