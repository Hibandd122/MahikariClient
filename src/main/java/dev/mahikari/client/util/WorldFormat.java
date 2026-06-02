package dev.mahikari.client.util;

import java.util.Map;

public final class WorldFormat {
    private static final Map<String, String> BIOME_ICONS = Map.ofEntries(new Map.Entry[]{Map.entry("badlands", "\udb80\udf60"), Map.entry("eroded_badlands", "\udb80\udf60"), Map.entry("wooded_badlands", "\udb80\udf60"), Map.entry("cherry_grove", "\udb80\udf61"), Map.entry("desert", "\udb80\udf62"), Map.entry("jungle", "\udb80\udf63"), Map.entry("bamboo_jungle", "\udb80\udf63"), Map.entry("sparse_jungle", "\udb80\udf63"), Map.entry("mushroom_fields", "\udb80\udf64"), Map.entry("plains", "\udb80\udf65"), Map.entry("sunflower_plains", "\udb80\udf65"), Map.entry("meadow", "\udb80\udf65"), Map.entry("savanna", "\udb80\udf66"), Map.entry("savanna_plateau", "\udb80\udf66"), Map.entry("windswept_savanna", "\udb80\udf66"), Map.entry("snowy_plains", "\udb80\udf67"), Map.entry("snowy_taiga", "\udb80\udf67"), Map.entry("snowy_slopes", "\udb80\udf67"), Map.entry("ice_spikes", "\udb80\udf67"), Map.entry("frozen_peaks", "\udb80\udf67"), Map.entry("grove", "\udb80\udf67"), Map.entry("swamp", "\udb80\udf68"), Map.entry("mangrove_swamp", "\udb80\udf68"), Map.entry("taiga", "\udb80\udf69"), Map.entry("old_growth_pine_taiga", "\udb80\udf69"), Map.entry("old_growth_spruce_taiga", "\udb80\udf69"), Map.entry("pale_garden", "\udb80\udf70"), Map.entry("forest", "\udb80\udf69"), Map.entry("birch_forest", "\udb80\udf69"), Map.entry("dark_forest", "\udb80\udf69"), Map.entry("flower_forest", "\udb80\udf69"), Map.entry("old_growth_birch_forest", "\udb80\udf69"), Map.entry("windswept_hills", "\udb80\udf69"), Map.entry("windswept_forest", "\udb80\udf69"), Map.entry("stony_peaks", "\udb80\udf69"), Map.entry("jagged_peaks", "\udb80\udf69")});

    private WorldFormat() {
    }

    public static String getBiomeIcon(String biome) {
        if (biome == null || biome.isEmpty()) {
            return "";
        }
        String icon = BIOME_ICONS.get(biome);
        return icon != null ? icon : "";
    }

    public static String formatWorld(String w) {
        if (w == null || w.isEmpty()) {
            return "?";
        }
        if (w.contains("overworld") || w.equals("world")) {
            return "Overworld";
        }
        if (w.contains("nether")) {
            return "Nether";
        }
        if (w.contains("the_end") || w.contains("end")) {
            return "The End";
        }
        return w;
    }

    public static boolean sameWorld(String a, String b) {
        if (a == null || a.isEmpty()) {
            return true;
        }
        if (b == null) {
            b = "";
        }
        return a.equals(b) || a.endsWith("/" + b) || b.endsWith("/" + a);
    }
}
