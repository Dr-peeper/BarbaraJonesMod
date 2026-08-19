package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.boss.manager.ManagerMinion;
import com.barbarajones.boss.manager.TheManager;
import com.barbarajones.boss.mom.MomCobbBoss;
import com.barbarajones.boss.mom.MomKraveStash;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.DuhlWol;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;
import com.barbarajones.entity.ThePlug;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Binds each living entity to its attribute supplier. Without this a mob has a
 * null attribute map and constructing one throws immediately - during the login
 * handler that aborts placing the player and surfaces as "Invalid player data",
 * and at spawn time it just silently fails to spawn.
 *
 * <p>Duhl Wol shipped missing from this list and was therefore impossible to
 * spawn by any means, so {@link #verifyEveryMobRegistered()} now cross-checks
 * the registrations against {@link #REQUIRED} and throws at startup. A mistake
 * here is otherwise invisible until someone tries to spawn that specific mob.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {

    /**
     * Every Mob-based entity in the mod. Add new ones here as well as in
     * {@link #createAttributes} - the check below exists to make a mismatch
     * loud instead of leaving an unspawnable mob behind.
     */
    private static final List<RegistryObject<? extends EntityType<?>>> REQUIRED = List.of(
            ModEntities.BARBARA, ModEntities.CAYDEN, ModEntities.KRAVE_MONSTER,
            ModEntities.NUGGET, ModEntities.DANIEL, ModEntities.MOM, ModEntities.PLUG,
            ModEntities.DUHL_WOL, ModEntities.KRAVE_MINION, ModEntities.KRAVE_HEALING_BOX,
            ModEntities.MOM_BOSS, ModEntities.MOM_STASH,
            ModEntities.THE_MANAGER, ModEntities.MANAGER_MINION);

    /** Populated as we register, then checked against REQUIRED. */
    private static final Set<EntityType<?>> REGISTERED = new HashSet<>();

    private ModEntityAttributes() { }

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        put(event, ModEntities.BARBARA.get(),            BarbaraJones.createAttributes().build());
        put(event, ModEntities.CAYDEN.get(),             CaydenCobb.createAttributes().build());
        put(event, ModEntities.KRAVE_MONSTER.get(),      KraveMonster.createAttributes().build());
        put(event, ModEntities.NUGGET.get(),             Nugget.createAttributes().build());
        put(event, ModEntities.DANIEL.get(),             Daniel.createAttributes().build());
        put(event, ModEntities.MOM.get(),                MomCobb.createAttributes().build());
        put(event, ModEntities.PLUG.get(),               ThePlug.createAttributes().build());
        put(event, ModEntities.DUHL_WOL.get(),           DuhlWol.createAttributes().build());
        put(event, ModEntities.KRAVE_MINION.get(),       KraveMinion.createAttributes().build());
        put(event, ModEntities.KRAVE_HEALING_BOX.get(),  KraveHealingBox.createAttributes().build());
        put(event, ModEntities.MOM_BOSS.get(),           MomCobbBoss.createAttributes().build());
        put(event, ModEntities.MOM_STASH.get(),          MomKraveStash.createAttributes().build());
        put(event, ModEntities.THE_MANAGER.get(),        TheManager.createAttributes().build());
        put(event, ModEntities.MANAGER_MINION.get(),     ManagerMinion.createAttributes().build());

        verifyEveryMobRegistered();
    }

    private static <T extends LivingEntity> void put(EntityAttributeCreationEvent event,
                                                     EntityType<T> type, AttributeSupplier attrs) {
        event.put(type, attrs);
        REGISTERED.add(type);
    }

    /** Throws at startup rather than leaving a mob that dies the moment it spawns. */
    private static void verifyEveryMobRegistered() {
        List<String> missing = new ArrayList<>();
        for (RegistryObject<? extends EntityType<?>> ro : REQUIRED) {
            if (!REGISTERED.contains(ro.get())) {
                missing.add(ro.getId().toString());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "No attributes registered for " + String.join(", ", missing)
                    + " - add them in ModEntityAttributes.createAttributes(), or they cannot spawn.");
        }
    }
}
