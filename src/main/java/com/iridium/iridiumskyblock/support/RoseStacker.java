package com.iridium.iridiumskyblock.support;

import com.iridium.iridiumskyblock.IridiumSkyblock;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import org.bukkit.block.CreatureSpawner;

public class RoseStacker implements SpawnerSupport{

    public RoseStacker() {
        IridiumSkyblock.getInstance().getLogger().info("RoseStacker support loaded");
    }

    public int getSpawnerAmount(CreatureSpawner spawner) {
        StackedSpawner stackedSpawner = RoseStackerAPI.getInstance().getStackedSpawner(spawner.getBlock());
        if(stackedSpawner==null)return 1;
        return stackedSpawner.getStackSize();
    }
}
