package com.peaches.epicskyblock;

import com.peaches.epicskyblock.commands.CommandManager;
import com.peaches.epicskyblock.configs.Config;
import com.peaches.epicskyblock.configs.Messages;
import com.peaches.epicskyblock.configs.Missions;
import com.peaches.epicskyblock.configs.OreGen;
import com.peaches.epicskyblock.listeners.*;
import com.peaches.epicskyblock.placeholders.ClipPlaceholderAPIManager;
import com.peaches.epicskyblock.serializer.Persist;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpicSkyblock extends JavaPlugin {

    private SentryClient sentry;

    private static EpicSkyblock instance;

    private static Config configuration;
    private static Missions missions;
    private static OreGen oreGen;
    private static Messages messages;
    private static Persist persist;

    private static IslandManager islandManager;

    private static CommandManager commandManager;

    private ClipPlaceholderAPIManager clipPlaceholderAPIManager;
    /*
    TODO
    Permissions/Ranks: Owner Moderator Trusted Member
    Way to edit warps
     */

    @Override
    public void onEnable() {
        try {
            sentry = SentryClientFactory.sentryClient("https://88cbd35ae467457bbc87a56d81169389@sentry.prodigysupport.team/5" + "?timeout=15000" + "&async=true" + "&stacktrace.app.packages=com.peaches.epicskyblock");

            instance = this;

            super.onEnable();
            getDataFolder().mkdir();
            loadSchematic();

            persist = new Persist();

            commandManager = new CommandManager("island");
            commandManager.registerCommands();

            loadConfigs();
            saveConfigs();

            registerListeners(new onBlockBreak(), new onBlockPlace(), new onClick(), new onBlockFromTo(), new onPlayerMove(), new onInventoryClick(), new onSpawnerSpawn(), new onEntityDeath(), new onPlayerJoinLeave(), new onBlockGrow(), new onPlayerTalk(), new onEntityDamage(), new onEntityDamageByEntity(), new onPlayerExpChange(), new onPlayerFish(), new onEntityExplode());

            new Metrics(this);

            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> getPersist().save(islandManager), 0, 20);

            setupPlaceholderAPI();

            startCounting();

            getLogger().info("-------------------------------");
            getLogger().info("");
            getLogger().info(getDescription().getName() + " Enabled!");
            getLogger().info("");
            getLogger().info("-------------------------------");
        } catch (Exception e) {
            sendErrorMessage(e);
        }
    }

    public void startCounting() {
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> {
            try {
                if (LocalDateTime.now().getDayOfWeek().equals(DayOfWeek.MONDAY) && LocalDateTime.now().getHour() == 0 && LocalDateTime.now().getMinute() == 0 && LocalDateTime.now().getSecond() == 0) {
                    for (Island island : getIslandManager().islands.values()) {
                        island.treasureHunter = 0;
                        island.competitor = 0;
                        island.miner = 0;
                        island.farmer = 0;
                        island.hunter = 0;
                        island.fisherman = 0;
                        island.builder = 0;
                    }
                }
            } catch (Exception e) {
                sendErrorMessage(e);
            }
        }, 20, 20);
    }

    public void sendErrorMessage(Exception e) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw, true);
                e.printStackTrace(pw);
                String error = sw.getBuffer().toString();
                final EventBuilder eventBuilder = new EventBuilder();
                eventBuilder.withMessage(error);
                eventBuilder.withLevel(Event.Level.ERROR);
                eventBuilder.withTag("ip", InetAddress.getLocalHost().getHostAddress());
                eventBuilder.withTag("plugin_version", getDescription().getVersion());

                sentry.sendEvent(eventBuilder.build());

                getLogger().info(error);

            } catch (Exception exception) {
            }
        });
    }

    private void registerListeners(Listener... listener) {
        for (Listener l : listener) {
            Bukkit.getPluginManager().registerEvents(l, this);
        }
    }

    private void setupPlaceholderAPI() {
        Plugin clip = getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (clip != null && clip.isEnabled()) {
            this.clipPlaceholderAPIManager = new ClipPlaceholderAPIManager();
            if (this.clipPlaceholderAPIManager.register()) {
                getLogger().info("Successfully registered placeholders with PlaceholderAPI.");
            }
        }
    }

    public void loadSchematic() {
        File schematicFolder = new File(getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdir();
        }

        if (!new File(schematicFolder, "island.schematic").exists()) {
            if (getResource("schematics/island.schematic") != null) {
                saveResource("schematics/island.schematic", false);
            }
        }
    }

    public void loadConfigs() {
        configuration = persist.getFile(Config.class).exists() ? persist.load(Config.class) : new Config();
        missions = persist.getFile(Missions.class).exists() ? persist.load(Missions.class) : new Missions();
        islandManager = persist.getFile(IslandManager.class).exists() ? persist.load(IslandManager.class) : new IslandManager();
        messages = persist.getFile(Messages.class).exists() ? persist.load(Messages.class) : new Messages();
        oreGen = persist.getFile(OreGen.class).exists() ? persist.load(OreGen.class) : new OreGen();

        for (Island island : islandManager.islands.values()) {
            island.init();
        }
    }

    public void saveConfigs() {
        if (configuration != null) persist.save(configuration);
        if (missions != null) persist.save(missions);
        if (islandManager != null) persist.save(islandManager);
        if (messages != null) persist.save(messages);
        if (oreGen != null) persist.save(oreGen);
    }

    @Override
    public void onDisable() {
        try {
            super.onDisable();

            saveConfigs();

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.closeInventory();
            }

            getLogger().info("-------------------------------");
            getLogger().info("");
            getLogger().info(getDescription().getName() + " Disabled!");
            getLogger().info("");
            getLogger().info("-------------------------------");
        } catch (Exception e) {
            sendErrorMessage(e);
        }
    }

    public static EpicSkyblock getInstance() {
        return instance;
    }

    public static IslandManager getIslandManager() {
        return islandManager;
    }

    public static Config getConfiguration() {
        return configuration;
    }

    public static Missions getMissions() {
        return missions;
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    public static Messages getMessages() {
        return messages;
    }

    public static OreGen getOreGen() {
        if (oreGen == null) {
            oreGen = new OreGen();
            EpicSkyblock.getPersist().getFile(oreGen).delete();
            EpicSkyblock.getInstance().saveConfigs();
        }
        return oreGen;
    }

    public static Persist getPersist() {
        return persist;
    }
}
