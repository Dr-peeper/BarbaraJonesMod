package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.boss.manager.FilingCabinet;
import com.barbarajones.boss.manager.ManagerMinion;
import com.barbarajones.boss.manager.TerminationNotice;
import com.barbarajones.boss.manager.TheManager;
import com.barbarajones.boss.mom.MomCobbBoss;
import com.barbarajones.boss.mom.MomKraveStash;
import com.barbarajones.boss.mom.ThrownHousehold;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.DuhlWol;
import com.barbarajones.entity.DuhlWolCar;
import com.barbarajones.entity.GiantKraveBox;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveLaser;
import com.barbarajones.entity.KraveLeviathan;
import com.barbarajones.entity.KraveMouthBeam;
import com.barbarajones.entity.KraveMeteor;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.KraveTornado;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;
import com.barbarajones.entity.SkyCinematic;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.v2.airline.entity.PlaneEntity;
import com.barbarajones.v2.airline.entity.PilotEntity;
import com.barbarajones.v2.airline.entity.FlightAttendantEntity;
import com.barbarajones.v2.airline.entity.GateAgentEntity;
import com.barbarajones.v2.airline.entity.SecurityOfficerEntity;
import com.barbarajones.v2.airline.entity.GroundCrewEntity;
import com.barbarajones.v2.airline.entity.AirTrafficControllerEntity;

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
                    // Sized against the model's own geometry at the renderer's
                    // 1.8x scale (root-to-horn-tip / root-to-paw-bottom span,
                    // see KraveMonsterModel), not just eyeballed to "look big
                    // enough" - width already matched closely, height was the
                    // real gap (his head was clearing the old 3.0 box).
                    .sized(1.5F, 3.3F).clientTrackingRange(16).fireImmune()
                    .build("krave_monster"));

    public static final RegistryObject<EntityType<KraveLeviathan>> KRAVE_LEVIATHAN =
            ENTITIES.register("krave_leviathan", () -> EntityType.Builder
                    .<KraveLeviathan>of(KraveLeviathan::new, MobCategory.MISC)
                    // The hitbox stays small on purpose - nothing ever collides
                    // with this thing, and a many-block box would only make
                    // chunk tracking/entity culling more expensive for no
                    // benefit. KraveLeviathanRenderer scales the model itself
                    // up to den-size, the same trick KRAVE_MONSTER uses.
                    // Must comfortably exceed KraveLeviathan's own orbit
                    // radius (300-480) - a value smaller than the orbit
                    // itself meant the server essentially never sent this
                    // entity to anyone, which read as "it never spawns."
                    .sized(2.0F, 2.0F).clientTrackingRange(320).updateInterval(1)
                    .build("krave_leviathan"));

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

    // ---- airline entities -------------------------------------------------------

    public static final RegistryObject<EntityType<PlaneEntity>> PLANE =
            ENTITIES.register("plane", () -> EntityType.Builder
                    .<PlaneEntity>of(PlaneEntity::new, MobCategory.MISC)
                    .sized(12.0F, 3.5F).clientTrackingRange(64).updateInterval(1)
                    .build("plane"));

    public static final RegistryObject<EntityType<PilotEntity>> PILOT =
            ENTITIES.register("pilot", () -> EntityType.Builder
                    .of(PilotEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("pilot"));

    public static final RegistryObject<EntityType<FlightAttendantEntity>> FLIGHT_ATTENDANT =
            ENTITIES.register("flight_attendant", () -> EntityType.Builder
                    .of(FlightAttendantEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("flight_attendant"));

    public static final RegistryObject<EntityType<GateAgentEntity>> GATE_AGENT =
            ENTITIES.register("gate_agent", () -> EntityType.Builder
                    .of(GateAgentEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("gate_agent"));

    public static final RegistryObject<EntityType<SecurityOfficerEntity>> SECURITY_OFFICER =
            ENTITIES.register("security_officer", () -> EntityType.Builder
                    .of(SecurityOfficerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("security_officer"));

    public static final RegistryObject<EntityType<GroundCrewEntity>> GROUND_CREW =
            ENTITIES.register("ground_crew", () -> EntityType.Builder
                    .of(GroundCrewEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("ground_crew"));

    public static final RegistryObject<EntityType<AirTrafficControllerEntity>> AIR_TRAFFIC_CONTROLLER =
            ENTITIES.register("air_traffic_controller", () -> EntityType.Builder
                    .of(AirTrafficControllerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build("air_traffic_controller"));

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
                    .sized(1.3F, 1.8F).clientTrackingRange(12)
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

    public static final RegistryObject<EntityType<MomCobbBoss>> MOM_BOSS =
            ENTITIES.register("mom_cobb_boss", () -> EntityType.Builder
                    .of(MomCobbBoss::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F).clientTrackingRange(16)
                    .build("mom_cobb_boss"));
    public static final RegistryObject<EntityType<MomKraveStash>> MOM_STASH =
            ENTITIES.register("mom_krave_stash", () -> EntityType.Builder
                    .of(MomKraveStash::new, MobCategory.MISC)
                    .sized(0.8F, 1.0F).clientTrackingRange(12)
                    .build("mom_krave_stash"));
    public static final RegistryObject<EntityType<ThrownHousehold>> MOM_THROWN =
            ENTITIES.register("mom_thrown_object", () -> EntityType.Builder
                    .<ThrownHousehold>of(ThrownHousehold::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(16).updateInterval(1)
                    .build("mom_thrown_object"));
    public static final RegistryObject<EntityType<TheManager>> THE_MANAGER =
            ENTITIES.register("the_manager", () -> EntityType.Builder
                    .of(TheManager::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.3F).clientTrackingRange(24).fireImmune()
                    .build("the_manager"));
    public static final RegistryObject<EntityType<ManagerMinion>> MANAGER_MINION =
            ENTITIES.register("manager_minion", () -> EntityType.Builder
                    .of(ManagerMinion::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F).clientTrackingRange(16)
                    .build("manager_minion"));
    public static final RegistryObject<EntityType<TerminationNotice>> TERMINATION_NOTICE =
            ENTITIES.register("termination_notice", () -> EntityType.Builder
                    .<TerminationNotice>of(TerminationNotice::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(20).updateInterval(1)
                    .build("termination_notice"));
    public static final RegistryObject<EntityType<FilingCabinet>> FILING_CABINET =
            ENTITIES.register("filing_cabinet", () -> EntityType.Builder
                    .<FilingCabinet>of(FilingCabinet::new, MobCategory.MISC)
                    .sized(1.0F, 1.5F).clientTrackingRange(16).updateInterval(3)
                    .build("filing_cabinet"));

    // ---- Barbara's kit ------------------------------------------------------
    public static final RegistryObject<EntityType<com.barbarajones.entity.SmokeRing>> SMOKE_RING =
            ENTITIES.register("smoke_ring", () -> EntityType.Builder
                    .<com.barbarajones.entity.SmokeRing>of(com.barbarajones.entity.SmokeRing::new, MobCategory.MISC)
                    .sized(1.2F, 1.2F).clientTrackingRange(16).updateInterval(1)
                    .build("smoke_ring"));
    public static final RegistryObject<EntityType<com.barbarajones.entity.EmberCherry>> EMBER_CHERRY =
            ENTITIES.register("ember_cherry", () -> EntityType.Builder
                    .<com.barbarajones.entity.EmberCherry>of(com.barbarajones.entity.EmberCherry::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(16).updateInterval(1)
                    .build("ember_cherry"));
}
