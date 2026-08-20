package com.barbarajones.v2.internet.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.internet.InternetManagerBoss;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * CLIENT ONLY - reached solely from {@link InternetClientSetup}, which is
 * {@code Dist.CLIENT} guarded, so this never loads on a dedicated server.
 *
 * <p>Deliberately the simplest version of the hi-vis read: the plain
 * humanoid rig - {@code HumanoidModel.createMesh(...)} on its own has no
 * jacket/hat/sleeve overlay layer at all (that belongs to {@code PlayerModel}
 * alone, one level down the hierarchy), so the hi-vis colour comes entirely
 * from painting the base head/body/arm/leg faces, exactly the way any
 * reskinned zombie or villager works. The one piece of custom geometry is the
 * headset earpiece, on free UV that nothing else on a base humanoid mesh ever
 * touches - the same trick {@link com.barbarajones.boss.manager.ManagerModel}
 * uses for its collar and tie, at the same (0,32) origin, because a base
 * {@code HumanoidModel}'s own unwrap never reaches past y=32 no matter whose
 * texture file it is.
 *
 * <p>Full cinematic fidelity - the moustache, the tool belt, the cable reel -
 * belongs to {@link com.barbarajones.cinematic.actor.InternetManagerActor},
 * which plays the death cutscene. This is the in-combat model; it only needs
 * to read correctly at fight distance and sell the telegraphs.
 */
public class InternetManagerModel extends HumanoidModel<InternetManagerBoss> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "internet_manager"), "main");

    private final ModelPart earpiece;

    public InternetManagerModel(ModelPart root) {
        super(root);
        this.earpiece = root.getChild("head").getChild("earpiece");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        mesh.getRoot().getChild("head").addOrReplaceChild("earpiece",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.0F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(4.2F, -4.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(InternetManagerBoss entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float windup = entity.isWindingUp() ? entity.getWindupProgress() : 0.0F;
        int kind = entity.getWindupKind();

        switch (kind) {
            case InternetManagerBoss.WINDUP_BUFFER -> {
                // arms out to the sides, presenting the ring that is filling around him
                this.rightArm.zRot = -1.3F * windup;
                this.leftArm.zRot = 1.3F * windup;
                this.rightArm.xRot = -0.3F * windup;
                this.leftArm.xRot = -0.3F * windup;
                this.head.xRot -= 0.15F * Mth.sin(ageInTicks * 0.2F) * windup;
            }
            case InternetManagerBoss.WINDUP_THROTTLE -> {
                // both arms raised overhead, bringing the whole zone down
                this.rightArm.xRot = -3.0F * windup;
                this.leftArm.xRot = -3.0F * windup;
            }
            case InternetManagerBoss.WINDUP_LATENCY, InternetManagerBoss.WINDUP_PACKET_LOSS -> {
                // one hand to the ear, listening to the dispatcher before he acts
                this.rightArm.xRot = -2.0F * windup;
                this.rightArm.zRot = -0.5F * windup;
                this.head.yRot += 0.2F * windup;
            }
            default -> {
                // WINDUP_WHIP / none: a coiled, ready lean forward
                this.body.xRot = 0.12F * windup;
                this.rightArm.xRot -= 1.6F * windup;
                this.leftArm.xRot -= 0.6F * windup;
            }
        }

        // an impatient shift of weight even when he is doing nothing at all
        this.body.yRot = Mth.sin(ageInTicks * 0.05F) * 0.05F;

        // the earpiece answers to head movement with a beat of lag
        this.earpiece.xRot = this.head.xRot * 0.2F;
        this.earpiece.yRot = this.head.yRot * 0.2F;
    }
}
