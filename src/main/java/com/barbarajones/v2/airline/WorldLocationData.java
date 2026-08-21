package com.barbarajones.v2.airline;

import java.util.*;

public class WorldLocationData {
    public static class Location {
        public String cityName;
        public String airportCode;
        public int x, y, z;
        public int runwayOrientation; // 0=North-South, 1=East-West

        public Location(String cityName, String airportCode, int x, int y, int z, int runwayOrientation) {
            this.cityName = cityName;
            this.airportCode = airportCode;
            this.x = x;
            this.y = y;
            this.z = z;
            this.runwayOrientation = runwayOrientation;
        }
    }

    private static final Map<String, Location> LOCATIONS = new HashMap<>();

    static {
        // Major world cities - scaled coordinates for Minecraft
        // 1 block ≈ 50km in real world
        // Using equirectangular projection for simplicity
        LOCATIONS.put("LAX", new Location("Los Angeles", "LAX", 0, 65, 0, 1));
        LOCATIONS.put("JFK", new Location("New York", "JFK", 5000, 65, 0, 0));
        LOCATIONS.put("LHR", new Location("London", "LHR", 10000, 65, 2000, 1));
        LOCATIONS.put("CDG", new Location("Paris", "CDG", 9500, 65, 1500, 0));
        LOCATIONS.put("SYD", new Location("Sydney", "SYD", 15000, 65, -8000, 1));
        LOCATIONS.put("NRT", new Location("Tokyo", "NRT", 14000, 65, -6000, 0));
        LOCATIONS.put("MEX", new Location("Mexico City", "MEX", -2000, 65, -3000, 1));
        LOCATIONS.put("GRU", new Location("São Paulo", "GRU", 6000, 65, -8000, 0));
        LOCATIONS.put("DUB", new Location("Dubai", "DXB", 11000, 65, -4000, 1));
        LOCATIONS.put("SIN", new Location("Singapore", "SIN", 12000, 65, -3000, 0));
        LOCATIONS.put("BKK", new Location("Bangkok", "BKK", 11500, 65, -2500, 1));
        LOCATIONS.put("HND", new Location("Tokyo Haneda", "HND", 14200, 65, -5800, 0));
        LOCATIONS.put("DEL", new Location("Delhi", "DEL", 10500, 65, -2000, 1));
        LOCATIONS.put("ORD", new Location("Chicago", "ORD", 3000, 65, 500, 0));
        LOCATIONS.put("DEN", new Location("Denver", "DEN", 2000, 65, -1000, 1));
        LOCATIONS.put("MIA", new Location("Miami", "MIA", 5500, 65, -1000, 0));
        LOCATIONS.put("SFO", new Location("San Francisco", "SFO", -500, 65, -1000, 1));
        LOCATIONS.put("SEA", new Location("Seattle", "SEA", -1500, 65, -500, 0));
        LOCATIONS.put("YVR", new Location("Vancouver", "YVR", -2000, 65, 500, 1));
        LOCATIONS.put("YYZ", new Location("Toronto", "YYZ", 4000, 65, 1500, 0));
        LOCATIONS.put("AUS", new Location("Austin", "AUS", 3500, 65, -2000, 1));
        LOCATIONS.put("DAL", new Location("Dallas", "DAL", 3800, 65, -1800, 0));
        LOCATIONS.put("LAD", new Location("Las Vegas", "LAS", 1000, 65, -1500, 1));
        LOCATIONS.put("FRA", new Location("Frankfurt", "FRA", 9800, 65, 1800, 0));
        LOCATIONS.put("AMS", new Location("Amsterdam", "AMS", 9600, 65, 1600, 1));
        LOCATIONS.put("ZRH", new Location("Zurich", "ZRH", 9700, 65, 1400, 0));
        LOCATIONS.put("MUC", new Location("Munich", "MUC", 9900, 65, 1200, 1));
        LOCATIONS.put("FCO", new Location("Rome", "FCO", 10000, 65, 800, 0));
        LOCATIONS.put("MAD", new Location("Madrid", "MAD", 9000, 65, 1000, 1));
        LOCATIONS.put("BCN", new Location("Barcelona", "BCN", 8900, 65, 900, 0));
        LOCATIONS.put("ICN", new Location("Seoul", "ICN", 14500, 65, -5000, 1));
        LOCATIONS.put("PVG", new Location("Shanghai", "PVG", 13500, 65, -4000, 0));
        LOCATIONS.put("HKG", new Location("Hong Kong", "HKG", 13000, 65, -2500, 1));
        LOCATIONS.put("KUL", new Location("Kuala Lumpur", "KUL", 12500, 65, -2000, 0));
        LOCATIONS.put("CGK", new Location("Jakarta", "CGK", 12000, 65, -1000, 1));
        LOCATIONS.put("BNE", new Location("Brisbane", "BNE", 14500, 65, -7500, 0));
        LOCATIONS.put("MEL", new Location("Melbourne", "MEL", 14800, 65, -8500, 1));
        LOCATIONS.put("AKL", new Location("Auckland", "AKL", 16000, 65, -9000, 0));
        LOCATIONS.put("CCU", new Location("Calcutta", "CCU", 11000, 65, -1500, 1));
        LOCATIONS.put("BOM", new Location("Mumbai", "BOM", 10800, 65, -1800, 0));
        LOCATIONS.put("TLV", new Location("Tel Aviv", "TLV", 10300, 65, -3500, 1));
        LOCATIONS.put("CAI", new Location("Cairo", "CAI", 10600, 65, -4000, 0));
        LOCATIONS.put("JNB", new Location("Johannesburg", "JNB", 11500, 65, -10000, 1));
        LOCATIONS.put("CPT", new Location("Cape Town", "CPT", 11000, 65, -11000, 0));
        LOCATIONS.put("LIM", new Location("Lima", "LIM", 2000, 65, -8500, 1));
        LOCATIONS.put("BOG", new Location("Bogotá", "BOG", 3000, 65, -5000, 0));
        LOCATIONS.put("VCP", new Location("Campinas", "VCP", 6200, 65, -8200, 1));
    }

    public static Location getLocation(String code) {
        return LOCATIONS.get(code);
    }

    public static Collection<Location> getAllLocations() {
        return LOCATIONS.values();
    }

    public static Location getNearestLocation(int x, int z, double maxDistance) {
        Location nearest = null;
        double minDist = maxDistance;

        for (Location loc : LOCATIONS.values()) {
            double dist = Math.sqrt(Math.pow(loc.x - x, 2) + Math.pow(loc.z - z, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = loc;
            }
        }

        return nearest;
    }

    public static double getDistanceBlocks(Location from, Location to) {
        return Math.sqrt(
            Math.pow(to.x - from.x, 2) +
            Math.pow(to.z - from.z, 2)
        );
    }

    public static double getDistanceKm(Location from, Location to) {
        return getDistanceBlocks(from, to) * 50.0; // 1 block = 50km
    }

    public static List<Location> getNearbyLocations(Location center, double radiusBlocks) {
        List<Location> nearby = new ArrayList<>();
        for (Location loc : LOCATIONS.values()) {
            if (!loc.equals(center)) {
                double dist = getDistanceBlocks(center, loc);
                if (dist <= radiusBlocks) {
                    nearby.add(loc);
                }
            }
        }
        return nearby;
    }

    public static String getLocationCodeByCity(String cityName) {
        for (Location loc : LOCATIONS.values()) {
            if (loc.cityName.equalsIgnoreCase(cityName)) {
                return loc.airportCode;
            }
        }
        return null;
    }

    public static int getFlightDurationTicks(Location from, Location to) {
        double distanceBlocks = getDistanceBlocks(from, to);
        double flightSpeedPerTick = 2.0; // blocks per tick
        int estimatedDuration = (int)(distanceBlocks / flightSpeedPerTick);
        return Math.min(6000, Math.max(600, estimatedDuration)); // 30sec to 5min
    }
}
