package com.peaches.epicskyblock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public static Biome getNextBiome(Biome biome) {
        boolean next = false;
        for (Biome b : Biome.values()) {
            if (next) {
                return b;
            }
            if (b.equals(biome)) {
                next = true;
            }
        }
        return Biome.values()[0];
    }

    public static Biome getPreviousBiome(Biome biome) {
        int id = -1;
        for (int i = 0; i < Biome.values().length; i++) {
            if (Biome.values()[i].equals(biome)) {
                if (i != 0) {
                    return Biome.values()[i - 1];
                }
            }
        }
        return Biome.values()[Biome.values().length - 1];
    }

    public static String unColour(String string) {
        return string.replace(ChatColor.AQUA + "", "&b");
    }

    public static ItemStack makeItem(Material material, int amount, int type, String name) {
        ItemStack item = new ItemStack(material, amount, (short) type);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(m);
        return item;
    }

    public static ItemStack makeItem(Material material, int amount, int type, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount, (short) type);
        ItemMeta m = item.getItemMeta();
        m.setLore(Utils.color(lore));
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(m);
        return item;
    }

    public static ItemStack makeItemHidden(Material material, int amount, int type, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount, (short) type);
        ItemMeta m = item.getItemMeta();
        m.setLore(Utils.color(lore));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(m);
        return item;
    }

    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static List<String> color(List<String> strings) {
        return strings.stream().map(Utils::color).collect(Collectors.toList());
    }

    public static boolean isBlockValuable(Block b) {
        return EpicSkyblock.getConfiguration().blockvalue.containsKey(b.getType()) || b.getState() instanceof CreatureSpawner;
    }

    public static List<Island> getTopIslands() {
        List<Island> islands = new ArrayList<>(EpicSkyblock.getIslandManager().islands.values());
        islands.sort(Comparator.comparingInt(Island::getValue));
        Collections.reverse(islands);
        return islands;
    }


    public static boolean isSafe(Location loc, Island island) {
        return (island.isInIsland(loc) && loc.getBlock().getType().equals(Material.AIR)
                && (!loc.clone().add(0, -1, 0).getBlock().getType().equals(Material.AIR) && !loc.clone().add(0, -1, 0).getBlock().isLiquid()));
    }

    public static int getIslandRank(Island island) {
        int i = 1;
        for (Island is : getTopIslands()) {
            if (is.equals(island)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    public static Location getNewHome(Island island, Location loc) {
        Block b;
        b = EpicSkyblock.getIslandManager().getWorld().getHighestBlockAt(loc);
        if (isSafe(b.getLocation(), island)) {
            return b.getLocation().add(0.5, 1, 0.5);
        }

        for (double X = island.getPos1().getX(); X <= island.getPos2().getX(); X++) {
            for (double Z = island.getPos1().getZ(); Z <= island.getPos2().getZ(); Z++) {
                b = EpicSkyblock.getIslandManager().getWorld().getHighestBlockAt((int) X, (int) Z);
                if (isSafe(b.getLocation(), island)) {
                    return b.getLocation().add(0.5, 1, 0.5);
                }
            }
        }
        return null;
    }
}
