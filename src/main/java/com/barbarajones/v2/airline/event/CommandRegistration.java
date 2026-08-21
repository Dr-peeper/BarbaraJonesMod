package com.barbarajones.v2.airline.event;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.command.FlightCommand;
import com.barbarajones.v2.airline.command.BoardCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistration {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        FlightCommand.register(event.getDispatcher());
        BoardCommand.register(event.getDispatcher());
    }
}
