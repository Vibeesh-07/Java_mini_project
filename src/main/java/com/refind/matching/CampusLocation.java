package com.refind.matching;

import java.util.*;

public class CampusLocation {

    public static final String ZONE_LIBRARY = "LIBRARY_COMPLEX";
    public static final String ZONE_SCIENCE = "SCIENCE_BUILDING";
    public static final String ZONE_ENGINEERING = "ENGINEERING_BLOCK";
    public static final String ZONE_STUDENT_CENTER = "STUDENT_CENTER";
    public static final String ZONE_SPORTS = "SPORTS_ARENA";
    public static final String ZONE_RESIDENCE = "RESIDENCE_HALLS";
    public static final String ZONE_ADMIN = "ADMIN_BUILDING";
    public static final String ZONE_GENERAL = "CAMPUS_GENERAL";

    private static final Map<String, List<String>> ADJACENT_ZONES = new HashMap<>();

    static {
        ADJACENT_ZONES.put(ZONE_LIBRARY, Arrays.asList(ZONE_ADMIN, ZONE_STUDENT_CENTER));
        ADJACENT_ZONES.put(ZONE_SCIENCE, Arrays.asList(ZONE_ENGINEERING, ZONE_LIBRARY));
        ADJACENT_ZONES.put(ZONE_ENGINEERING, Arrays.asList(ZONE_SCIENCE, ZONE_STUDENT_CENTER));
        ADJACENT_ZONES.put(ZONE_STUDENT_CENTER, Arrays.asList(ZONE_LIBRARY, ZONE_SPORTS, ZONE_ENGINEERING));
        ADJACENT_ZONES.put(ZONE_SPORTS, Arrays.asList(ZONE_STUDENT_CENTER, ZONE_RESIDENCE));
        ADJACENT_ZONES.put(ZONE_RESIDENCE, Arrays.asList(ZONE_SPORTS, ZONE_STUDENT_CENTER));
        ADJACENT_ZONES.put(ZONE_ADMIN, Arrays.asList(ZONE_LIBRARY));
    }

    public static int calculateLocationScore(String loc1, String zone1, String loc2, String zone2, List<String> explanations) {
        if (loc1 == null || loc2 == null || loc1.isBlank() || loc2.isBlank()) {
            return 0;
        }

        String norm1 = loc1.trim().toLowerCase(Locale.ROOT);
        String norm2 = loc2.trim().toLowerCase(Locale.ROOT);

        // Exact location name match
        if (norm1.equals(norm2)) {
            explanations.add("Exact location match: '" + loc1.trim() + "' (+15 pts)");
            return 15;
        }

        // Substring location match (e.g., "Library 2nd Floor" vs "Main Library")
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            explanations.add("High location name overlap: '" + loc1.trim() + "' and '" + loc2.trim() + "' (+12 pts)");
            return 12;
        }

        // Same zone match
        if (zone1 != null && zone2 != null && !zone1.isBlank() && !zone2.isBlank() && zone1.equalsIgnoreCase(zone2)) {
            explanations.add("Same campus zone: " + zone1 + " (+10 pts)");
            return 10;
        }

        // Adjacent zones match
        if (zone1 != null && zone2 != null && isAdjacent(zone1.toUpperCase(Locale.ROOT), zone2.toUpperCase(Locale.ROOT))) {
            explanations.add("Adjacent campus zone: " + zone1 + " near " + zone2 + " (+5 pts)");
            return 5;
        }

        return 0;
    }

    private static boolean isAdjacent(String z1, String z2) {
        List<String> adj = ADJACENT_ZONES.get(z1);
        return adj != null && adj.contains(z2);
    }
}
