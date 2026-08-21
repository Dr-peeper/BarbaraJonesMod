package com.barbarajones.v2.airline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class PlayerFlightStats {
    private static final Map<String, PlayerFlightStats> STATS = new HashMap<>();

    private UUID playerId;
    private int totalFlights = 0;
    private int totalDistance = 0; // in blocks
    private int totalMiles = 0;
    private Set<String> visitedCities = new HashSet<>();
    private long totalFlightTime = 0; // in ticks
    private String favoriteAirline = "NONE";

    public PlayerFlightStats(UUID playerId) {
        this.playerId = playerId;
    }

    public static PlayerFlightStats getOrCreate(Player player) {
        return STATS.computeIfAbsent(player.getUUID().toString(), k -> new PlayerFlightStats(player.getUUID()));
    }

    public static PlayerFlightStats get(UUID playerId) {
        return STATS.get(playerId.toString());
    }

    public void recordFlight(FlightData flight) {
        totalFlights++;

        double distanceBlocks = Math.sqrt(
            Math.pow(flight.arrivalX - flight.departureX, 2) +
            Math.pow(flight.arrivalZ - flight.departureZ, 2)
        );
        totalDistance += distanceBlocks;
        totalMiles += (int)(distanceBlocks * 50); // 1 block = 50km

        visitedCities.add(flight.departureCity);
        visitedCities.add(flight.arrivalCity);

        totalFlightTime += flight.scheduledDuration;
    }

    public int getTotalFlights() {
        return totalFlights;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public int getTotalMiles() {
        return totalMiles;
    }

    public Set<String> getVisitedCities() {
        return new HashSet<>(visitedCities);
    }

    public long getTotalFlightTime() {
        return totalFlightTime;
    }

    public int getTotalFlightHours() {
        return (int)(totalFlightTime / 20 / 3600); // Convert ticks to hours (20 ticks = 1 second)
    }

    public int getTotalFlightMinutes() {
        return (int)((totalFlightTime / 20 / 60) % 60);
    }

    public void save(CompoundTag tag) {
        tag.putInt("TotalFlights", totalFlights);
        tag.putInt("TotalDistance", totalDistance);
        tag.putInt("TotalMiles", totalMiles);
        tag.putLong("TotalFlightTime", totalFlightTime);
        tag.putString("FavoriteAirline", favoriteAirline);

        CompoundTag citiesTag = new CompoundTag();
        int cityIndex = 0;
        for (String city : visitedCities) {
            citiesTag.putString("City" + cityIndex, city);
            cityIndex++;
        }
        tag.put("VisitedCities", citiesTag);
    }

    public static PlayerFlightStats load(UUID playerId, CompoundTag tag) {
        PlayerFlightStats stats = new PlayerFlightStats(playerId);
        stats.totalFlights = tag.getInt("TotalFlights");
        stats.totalDistance = tag.getInt("TotalDistance");
        stats.totalMiles = tag.getInt("TotalMiles");
        stats.totalFlightTime = tag.getLong("TotalFlightTime");
        stats.favoriteAirline = tag.getString("FavoriteAirline");

        CompoundTag citiesTag = tag.getCompound("VisitedCities");
        int cityIndex = 0;
        while (citiesTag.contains("City" + cityIndex)) {
            stats.visitedCities.add(citiesTag.getString("City" + cityIndex));
            cityIndex++;
        }

        return stats;
    }

    public String getFrequentFlyerStatus() {
        if (totalFlights < 5) return "Rookie Flyer";
        if (totalFlights < 25) return "Regular Traveler";
        if (totalFlights < 100) return "Frequent Flyer";
        if (totalFlights < 250) return "Gold Member";
        if (totalFlights < 500) return "Platinum Member";
        return "Elite Status";
    }
}
