package com.barbarajones.v2.airline.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class BoardingPassItem extends Item {

    public BoardingPassItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String flightNumber = tag.getString("FlightNumber");
            String seatNumber = tag.getString("SeatNumber");
            String departureCity = tag.getString("DepartureCity");
            String arrivalCity = tag.getString("ArrivalCity");
            long departureTime = tag.getLong("DepartureTime");

            tooltip.add(Component.literal("§6Flight: §r" + flightNumber));
            tooltip.add(Component.literal("§6Seat: §r" + seatNumber));
            tooltip.add(Component.literal("§6Route: §r" + departureCity + " → " + arrivalCity));
            if (departureTime > 0) {
                tooltip.add(Component.literal("§6Departure: §rWorld tick " + departureTime));
            }
        }
    }

    public static ItemStack createBoardingPass(String flightNumber, String seatNumber,
                                                String departureCity, String arrivalCity,
                                                long departureTime) {
        // The registered instance, not a fresh one. `new BoardingPassItem(...)` builds
        // an item the game has never heard of: it has no registry name, so the stack
        // serialises to air the moment it is saved and shows as "air" in any tooltip.
        ItemStack stack = new ItemStack(com.barbarajones.content.ModItems.BOARDING_PASS.get());

        CompoundTag tag = new CompoundTag();
        tag.putString("FlightNumber", flightNumber);
        tag.putString("SeatNumber", seatNumber);
        tag.putString("DepartureCity", departureCity);
        tag.putString("ArrivalCity", arrivalCity);
        tag.putLong("DepartureTime", departureTime);

        stack.setTag(tag);
        stack.setHoverName(Component.literal("§6Boarding Pass - " + flightNumber));

        return stack;
    }

    public static String getFlightNumber(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString("FlightNumber") : "";
    }

    public static String getSeatNumber(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString("SeatNumber") : "";
    }

    public static String getDepartureCity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString("DepartureCity") : "";
    }

    public static String getArrivalCity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString("ArrivalCity") : "";
    }

    public static long getDepartureTime(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getLong("DepartureTime") : 0;
    }
}
