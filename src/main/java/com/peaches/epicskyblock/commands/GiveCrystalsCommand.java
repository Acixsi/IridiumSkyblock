package com.peaches.epicskyblock.commands;

import com.peaches.epicskyblock.EpicSkyblock;
import com.peaches.epicskyblock.Island;
import com.peaches.epicskyblock.User;
import com.peaches.epicskyblock.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GiveCrystalsCommand extends Command {

    public GiveCrystalsCommand() {
        super(Arrays.asList("givecrystals"), "Give a player Crystals", "EpicSkyblock.givecrystals", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("/is givecrystals <player> <amount>");
            return;
        }

        if (Bukkit.getPlayer(args[1]) != null) {
            Player player = Bukkit.getPlayer(args[1]);
            if (player != null) {
                Island island = User.getUser(player).getIsland();
                if (island != null) {
                    try {
                        int amount = Integer.parseInt(args[2]);
                        island.setCrystals(island.getCrystals() + amount);
                        sender.sendMessage(Utils.color(EpicSkyblock.getMessages().giveCrystals.replace("%crystals%", args[2]).replace("%player%", player.getName()).replace("%prefix%", EpicSkyblock.getConfiguration().prefix)));
                        player.sendMessage(Utils.color(EpicSkyblock.getMessages().givenCrystals.replace("%crystals%", args[2]).replace("%player%", player.getName()).replace("%prefix%", EpicSkyblock.getConfiguration().prefix)));
                    } catch (Exception e) {
                        sender.sendMessage(args[2] + "is not a number");
                    }
                } else {
                    sender.sendMessage(Utils.color(EpicSkyblock.getMessages().playerNoIsland.replace("%prefix%", EpicSkyblock.getConfiguration().prefix)));
                }
            } else {
                sender.sendMessage(Utils.color(EpicSkyblock.getMessages().playerOffline.replace("%prefix%", EpicSkyblock.getConfiguration().prefix)));
            }
        }
    }

    @Override
    public List<String> TabComplete(CommandSender cs, org.bukkit.command.Command cmd, String s, String[] args) {
        return null;
    }
}
