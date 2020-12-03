package com.iridium.iridiumskyblock.managers;

import com.iridium.iridiumskyblock.*;
import com.iridium.iridiumskyblock.configs.Config;
import com.iridium.iridiumskyblock.configs.Schematics;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class IslandManager {

    public static Map<Integer, Island> cache = new HashMap<>();

    public static transient Integer id = 0;

    public static int length;
    public static int current;

    public static Direction direction;
    public static Location nextLocation;

    public static int nextID;

    public static World getWorld() {
        return Bukkit.getWorld(IridiumSkyblock.getConfiguration().worldName);
    }

    public static World getNetherWorld() {
        return Bukkit.getWorld(IridiumSkyblock.getConfiguration().netherWorldName);
    }

    public static void createIsland(Player player) {
        User user = User.getUser(player);
        if (user.isOnCooldown()) {
            //The user cannot create an island
            player.sendMessage(Utils.color(user.getCooldownTimeMessage()));
            return;
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, IridiumSkyblock.getConfiguration().regenCooldown);
        user.lastCreate = c.getTime();

        Location pos1 = nextLocation.clone().subtract((IridiumSkyblock.getUpgrades().sizeUpgrade.upgrades.get(1).size / 2.00), 0, (IridiumSkyblock.getUpgrades().sizeUpgrade.upgrades.get(1).size / 2.00));
        Location pos2 = nextLocation.clone().add((IridiumSkyblock.getUpgrades().sizeUpgrade.upgrades.get(1).size / 2.00), 0, (IridiumSkyblock.getUpgrades().sizeUpgrade.upgrades.get(1).size / 2.00));
        Location center = nextLocation.clone().add(0, 100, 0);
        Location home = nextLocation.clone();

        Location netherhome = home.clone();

        if (IridiumSkyblock.getConfiguration().netherIslands) {
            netherhome.setWorld(getNetherWorld());
        }
        Island island = new Island(player, pos1, pos2, center, home, netherhome, nextID);
        Bukkit.getScheduler().runTaskAsynchronously(IridiumSkyblock.getInstance(), () -> {
            try {
                Connection connection = IridiumSkyblock.getSqlManager().getConnection();
                PreparedStatement insert = connection.prepareStatement("INSERT INTO islands (id,json) VALUES (?,?);");
                insert.setInt(1, nextID);
                insert.setString(2, IridiumSkyblock.getPersist().getGson().toJson(island));
                insert.executeUpdate();
                insert.close();
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        cache.put(nextID, island);

        user.islandID = nextID;
        user.role = Role.Owner;

        if (IridiumSkyblock.getSchematics().schematics.size() == 1) {
            for (Schematics.FakeSchematic schematic : IridiumSkyblock.getSchematics().schematics) {
                island.setSchematic(schematic.name);
                island.setNetherschematic(schematic.netherisland);
                island.setHome(island.getHome().add(schematic.x, schematic.y, schematic.z));
                island.setNetherhome(island.getNetherhome().add(schematic.x, schematic.y, schematic.z));
            }
            island.pasteSchematic(player, false);
        } else {
            player.openInventory(island.getSchematicSelectGUI().getInventory());
        }

        switch (direction) {
            case NORTH:
                nextLocation.add(IridiumSkyblock.getConfiguration().distance, 0, 0);
                break;
            case EAST:
                nextLocation.add(0, 0, IridiumSkyblock.getConfiguration().distance);
                break;
            case SOUTH:
                nextLocation.subtract(IridiumSkyblock.getConfiguration().distance, 0, 0);
                break;
            case WEST:
                nextLocation.subtract(0, 0, IridiumSkyblock.getConfiguration().distance);
                break;
        }

        current++;

        if (current == length) {
            current = 0;
            direction = direction.next();
            if (direction == Direction.SOUTH || direction == Direction.NORTH) {
                length++;
            }
        }

        nextID++;

        IridiumSkyblock.getInstance().saveData(true);
    }

    public static int purgeIslands(int days, CommandSender sender) {
        List<Integer> ids = getLoadedIslands().stream().filter(island -> oldIsland(days, island)).map(Island::getId).collect(Collectors.toList());
        final ListIterator<Integer> islandIds = ids.listIterator();
        id = Bukkit.getScheduler().scheduleSyncRepeatingTask(IridiumSkyblock.getInstance(), new Runnable() {
            int amount = 0;

            @Override
            public void run() {
                if (islandIds.hasNext()) {
                    int i = islandIds.next();
                    Island island = getIslandViaId(i);
                    island.delete();
                    amount++;
                } else {
                    sender.sendMessage(Utils.color(IridiumSkyblock.getMessages().purgingFinished.replace("%amount%", String.valueOf(amount)).replace("%prefix%", IridiumSkyblock.getConfiguration().prefix)));
                    Bukkit.getScheduler().cancelTask(id);
                    id = 0;
                }
            }
        }, 0, 20 * 5);
        return ids.size();
    }

    private static boolean oldIsland(int days, Island island) {
        LocalDateTime now = LocalDateTime.now();
        for (OfflinePlayer player : island.getMembers().stream().map(s -> Bukkit.getOfflinePlayer(UUID.fromString(s))).collect(Collectors.toList())) {
            if (player == null) continue;
            LocalDateTime lastLogin = LocalDateTime.ofInstant(Instant.ofEpochMilli(player.getLastPlayed()), TimeZone.getDefault().toZoneId());
            Duration duration = Duration.between(lastLogin, now);
            if (duration.toDays() < days) {
                return false;
            }
        }
        return true;
    }

    public static void makeWorlds() {
        makeWorld(Environment.NORMAL, IridiumSkyblock.getConfiguration().worldName);
        if (IridiumSkyblock.getConfiguration().netherIslands)
            makeWorld(Environment.NETHER, IridiumSkyblock.getConfiguration().netherWorldName);
    }

    private static void makeWorld(Environment env, String name) {
        WorldCreator wc = new WorldCreator(name);
        wc.type(WorldType.FLAT);
        wc.generateStructures(false);
        wc.generator(new SkyblockGenerator());
        wc.environment(env);
        wc.createWorld();
    }

    public static List<Island> getLoadedIslands() {
        return new ArrayList<>(cache.values());
    }

    public static Island getIslandViaLocation(Location location) {
        if (location == null) return null;
        if (!isIslandWorld(location)) return null;

        final Chunk chunk = location.getChunk();

        final double x = location.getX();
        final double z = location.getZ();
        final Set<Integer> islandIds = ClaimManager.getIslands(chunk.getX(), chunk.getZ());

        for (int id : islandIds) {
            final Island island = getIslandViaId(id);
            if (island == null) continue;
            if (island.isInIsland(x, z)) return island;
        }

        for (Island island : getLoadedIslands()) {
            if (!island.isInIsland(x, z)) continue;
            ClaimManager.addClaim(chunk.getX(), chunk.getZ(), island.getId());
            return island;
        }

        return null;
    }

    public static Island getIslandViaId(int id) {

        if (cache.containsKey(id)) return cache.get(id);
        try {
            Connection connection = IridiumSkyblock.getSqlManager().getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM islands WHERE id =?;");
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                //There is a value
                Island island = IridiumSkyblock.getPersist().getGson().fromJson(resultSet.getString("json"), Island.class);
                cache.put(id, island);
                connection.close();

                island.init();
                if (island.getName().length() > IridiumSkyblock.getConfiguration().maxIslandName) {
                    island.setName(island.getName().substring(0, IridiumSkyblock.getConfiguration().maxIslandName));
                }
                if (island.getName().length() < IridiumSkyblock.getConfiguration().minIslandName) {
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(island.getOwner()));
                    island.setName(owner.getName());
                }

                return island;
            }
            cache.put(id, null);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static boolean isIslandWorld(Location location) {
        if (location == null) return false;
        return isIslandWorld(location.getWorld());
    }

    public static boolean isIslandWorld(World world) {
        if (world == null) return false;
        final String name = world.getName();
        return isIslandWorld(name);
    }

    public static boolean isIslandWorld(String name) {
        final Config config = IridiumSkyblock.getConfiguration();
        return (name.equals(config.worldName) || name.equals(config.netherWorldName));
    }

    public static void save(Island island, boolean async) {
        if (async)
            Bukkit.getScheduler().runTaskAsynchronously(IridiumSkyblock.getInstance(), () -> save(island, false));
        try {
            Connection connection = IridiumSkyblock.getSqlManager().getConnection();
            PreparedStatement insert = connection.prepareStatement("UPDATE islands SET json = ? WHERE id = ?;");
            insert.setString(1, IridiumSkyblock.getPersist().getGson().toJson(island));
            insert.setInt(2, island.getId());
            insert.executeUpdate();
            insert.close();
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void removeIsland(Island island) {
        final int id = island.getId();
        cache.remove(id);
        Bukkit.getScheduler().runTaskAsynchronously(IridiumSkyblock.getInstance(), () -> {
            try {
                Connection connection = IridiumSkyblock.getSqlManager().getConnection();
                PreparedStatement insert = connection.prepareStatement("DELETE FROM islands WHERE id=?;");
                insert.setInt(1, id);
                insert.executeUpdate();
                insert.close();
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
        ClaimManager.removeClaims(island.getId());
    }
}
