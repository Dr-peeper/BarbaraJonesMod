package com.barbarajones.v2.mobs;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KravelingEntity;
import com.barbarajones.v2.mobs.entity.KrispboneEntity;
import com.barbarajones.v2.mobs.entity.LoomweaverEntity;
import com.barbarajones.v2.mobs.entity.MascotEntity;
import com.barbarajones.v2.mobs.entity.SoggyEntity;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Binds each of the five Kraveling-family mobs to its attribute supplier.
 * {@code EntityAttributeCreationEvent} is a normal Forge event with no
 * "one subscriber only" restriction, so this lives entirely in its own class
 * rather than touching the shared {@code content.ModEntityAttributes} - every
 * mob here uses {@code Monster.createMonsterAttributes()} /
 * {@code PathfinderMob.createMobAttributes()}, never {@code createLivingAttributes()},
 * which is what actually supplies FOLLOW_RANGE (GroundPathNavigation reads it
 * in its constructor - the missing-attribute crash is instant, not deferred).
 *
 * <p>No {@code value = Dist...} on the annotation - this event fires on both
 * logical sides, and both need these suppliers registered.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobAttributes {

    private MobAttributes() { }

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ModMobEntities.KRAVELING.get(), KravelingEntity.createAttributes().build());
        event.put(ModMobEntities.KRISPBONE.get(), KrispboneEntity.createAttributes().build());
        event.put(ModMobEntities.LOOMWEAVER.get(), LoomweaverEntity.createAttributes().build());
        event.put(ModMobEntities.SOGGY.get(), SoggyEntity.createAttributes().build());
        event.put(ModMobEntities.KRAVAJO.get(), com.barbarajones.v2.mobs.entity.KravajoEntity.createAttributes().build());
        event.put(ModMobEntities.MASCOT.get(), MascotEntity.createAttributes().build());
        // KRAVE_SHARD (MISC, plain Projectile) is not a LivingEntity - no attributes needed.
    }
}
