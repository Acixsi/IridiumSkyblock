package com.peaches.epicskyblock.listeners;

import com.peaches.epicskyblock.EpicSkyblock;
import com.peaches.epicskyblock.Island;
import com.peaches.epicskyblock.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class onPlayerJoinLeave implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(EpicSkyblock.getInstance(), () -> {
                Island island = EpicSkyblock.getIslandManager().getIslandViaLocation(e.getPlayer().getLocation());
                if (island != null) {
                    island.sendBorder(e.getPlayer());
                }
            }, 1);
        } catch (Exception ex) {
            EpicSkyblock.getInstance().sendErrorMessage(ex);
        }
    }
}
