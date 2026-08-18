package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;
import com.barbarajones.entity.ThePlug;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Binds each living entity to its attribute supplier. Without this every mob
 * has a null attribute map and constructing one throws immediately - which,
 * during the login handler, aborts placing the player and shows up to the
 * player as "Invalid player data".
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {

    private ModEntityAttributes() { }

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BARBARA.get(),       BarbaraJones.createAttributes().build());
        event.put(ModEntities.CAYDEN.get(),        CaydenCobb.createAttributes().build());
        event.put(ModEntities.KRAVE_MONSTER.get(), KraveMonster.createAttributes().build());
        event.put(ModEntities.NUGGET.get(),        Nugget.createAttributes().build());
        event.put(ModEntities.DANIEL.get(),        Daniel.createAttributes().build());
        event.put(ModEntities.MOM.get(),           MomCobb.createAttributes().build());
        event.put(ModEntities.PLUG.get(),          ThePlug.createAttributes().build());
    }
}
