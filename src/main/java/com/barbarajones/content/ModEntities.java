package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.DuhlWol;
import com.barbarajones.entity.DuhlWolCar;
import com.barbarajones.entity.GiantKraveBox;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveLaser;
import com.barbarajones.entity.KraveMouthBeam;
import com.barbarajones.entity.KraveMeteor;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.KraveTornado;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;
import com.barbarajones.entity.SkyCinematic;
import com.barbarajones.entity.ThePlug;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    private ModEntities() { }

    public static final RegistryObject<EntityType<BarbaraJones>> BARBARA =
            ENTITIES.register("barbara_jones", () -> EntityType.Builder
                    .of(BarbaraJones::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("barbara_jones"));

    public static final RegistryObject<EntityType<CaydenCobb>> CAYDEN =
            ENTITIES.register("cayden_cobb", () -> EntityType.Builder
                    .of(CaydenCobb::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.4F).clientTrackingRange(10)
                    .build("cayden_cobb"));

    public static final RegistryObject<EntityType<KraveMonster>> KRAVE_MONSTER =
            ENTITIES.register("krave_monster", () -> EntityType.Builder
                    .of(KraveMonster::new, MobCategory.MONSTER)
                    // grown in step with the renderer's bigger scale so the
                    // hitbox doesn't look absurdly undersized against the model
                    .sized(1.5F, 3.0F).clientTrackingRange(16).fireImmune()
                    .build("krave_monster"));

    public static final RegistryObject<EntityType<Nugget>> NUGGET =
            ENTITIES.register("nugget", () -> EntityType.Builder
                    .<Nugget>of(Nugget::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F).clientTrackingRange(8)
                    .build("nugget"));

    public static final RegistryObject<EntityType<Daniel>> DANIEL =
            ENTITIES.register("daniel", () -> EntityType.Builder
                    .of(Daniel::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build("daniel"));

    public static final RegistryObject<EntityType<MomCobb>> MOM =
            ENTITIES.register("mom_cobb", () -> EntityType.Builder
                    .of(MomCobb::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.85F).clientTrackingRange(10)
                    .build("mom_cobb"));

    public static final RegistryObject<EntityType<ThePlug>> PLUG =
            ENTITIES.register("the_plug", () -> EntityType.Builder
                    .of(ThePlug::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F).clientTrackingRange(16)
                    .build("the_plug"));

    public static final RegistryObject<EntityType<DuhlWol>> DUHL_WOL =
            ENTITIES.register("duhl_wol", () -> EntityType.Builder
                    .of(DuhlWol::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F).clientTrackingRange(16)
                    .build("duhl_wol"));

    public static final RegistryObject<EntityType<DuhlWolCar>> DUHL_WOL_CAR =
            ENTITIES.register("duhl_wol_car", () -> EntityType.Builder
                    .<DuhlWolCar>of(DuhlWolCar::new, MobCategory.MISC)
                    .sized(1.8F, 1.6F).clientTrackingRange(24).updateInterval(3)
                    .build("duhl_wol_car"));

    // ---- cinematic / projectile entities (no spawn eggs) -------------------

    public static final RegistryObject<EntityType<KraveMeteor>> METEOR =
            ENTITIES.register("krave_meteor", () -> EntityType.Builder
                    .<KraveMeteor>of(KraveMeteor::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F).clientTrackingRange(16).updateInterval(1)
                    .build("krave_meteor"));

    public static final RegistryObject<EntityType<GiantKraveBox>> GIANT_BOX =
            ENTITIES.register("giant_krave_box", () -> EntityType.Builder
                    .<GiantKraveBox>of(GiantKraveBox::new, MobCategory.MISC)
                    .sized(3.0F, 4.0F).clientTrackingRange(16).updateInterval(2)
                    .build("giant_krave_box"));

    public static final RegistryObject<EntityType<KraveTornado>> TORNADO =
            ENTITIES.register("krave_tornado", () -> EntityType.Builder
                    .<KraveTornado>of(KraveTornado::new, MobCategory.MISC)
                    .sized(2.0F, 8.0F).clientTrackingRange(16).updateInterval(1)
                    .build("krave_tornado"));

    public static final RegistryObject<EntityType<KraveMinion>> KRAVE_MINION =
            ENTITIES.register("krave_minion", () -> EntityType.Builder
                    .of(KraveMinion::new, MobCategory.MISC)
                    .sized(0.5F, 1.2F).clientTrackingRange(12)
                    .build("krave_minion"));

    public static final RegistryObject<EntityType<KraveHealingBox>> KRAVE_HEALING_BOX =
            ENTITIES.register("krave_healing_box", () -> EntityType.Builder
                    .of(KraveHealingBox::new, MobCategory.MISC)
                    .sized(0.7F, 0.9F).clientTrackingRange(12)
                    .build("krave_healing_box"));

    public static final RegistryObject<EntityType<KraveLaser>> KRAVE_LASER =
            ENTITIES.register("krave_laser", () -> EntityType.Builder
                    .<KraveLaser>of(KraveLaser::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(16).updateInterval(1)
                    .build("krave_laser"));

    public static final RegistryObject<EntityType<KraveMouthBeam>> KRAVE_MOUTH_BEAM =
            ENTITIES.register("krave_mouth_beam", () -> EntityType.Builder
                    .<KraveMouthBeam>of(KraveMouthBeam::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(16).updateInterval(1)
                    .build("krave_mouth_beam"));

    public static final RegistryObject<EntityType<SkyCinematic>> SKY_CINEMATIC =
            ENTITIES.register("sky_cinematic", () -> EntityType.Builder
                    .<SkyCinematic>of(SkyCinematic::new, MobCategory.MISC)
                    .sized(2.0F, 4.0F).clientTrackingRange(20).updateInterval(2)
                    .build("sky_cinematic"));
}
