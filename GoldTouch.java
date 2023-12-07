// GoldTouch - Silk Touch for gold tools
// Copyright © 2023 - Obsidianlad
//
// This file is part of GoldTouch.
//
// GoldTouch is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// GoldTouch is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with GoldTouch. If not, see <https://www.gnu.org/licenses/>.

package lad.obsidian.goldtouch;

import java.io.File;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.config.ConfigurationNode;

/**
 * GoldTouch Plugin
 *
 * Listens for BlockBreakEvents, checks if gold tools were used, and applies Silk Touch if so.
 */
public class GoldTouch extends JavaPlugin implements Listener
{
    private String name = "[GoldTouch] ";
    private Set<Integer> ignore = new HashSet<Integer>();
    private List<Material> tools = Arrays.asList(
        Material.GOLD_AXE,
        Material.GOLD_HOE,
        Material.GOLD_SPADE,
        Material.GOLD_SWORD,
        Material.GOLD_PICKAXE
    );

    /**
     * {@inheritDoc}
     *
     * Creates config with hardcoded defaults if it doesn't exist.
     * Loads blocks to ignore from config.
     */
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);

        File datafolder = this.getDataFolder();
        Configuration config = getConfiguration();

        if (!datafolder.exists())
        {
            getServer().getLogger().warning(this.name + "Datafolder did not exist. It does now.");

            datafolder.mkdir();
        }

        if (config.getKeys().size() == 0)
        {
            getServer().getLogger().warning(this.name + "Empty config. Creating new from defaults.");

            String[] ignore = {
                // Left this one out as a funny easter egg:
                //"REDSTONE_TORCH_OFF",
                "BEDROCK",
                "PISTON_BASE",
                "PISTON_STICKY_BASE",
                "PISTON_EXTENSION",
                "PISTON_MOVING_PIECE",
                "MOB_SPAWNER",
                "LOCKED_CHEST",
                "DIODE_BLOCK_ON",
                "WOODEN_DOOR",
                "IRON_DOOR_BLOCK",
                "CAKE_BLOCK",
                "CROPS",
                "BURNING_FURNACE",
                // The remaining ones aren't really needed, but added just in case.
                "AIR",
                "WATER",
                "STATIONARY_WATER",
                "LAVA",
                "STATIONARY_LAVA",
                "FIRE",
                "PORTAL",
                // One could also add blocks that don't change when broken, for example:
                // dirt, cobble, logs, planks, slabs, chests, furnaces, signs, etc.
                // In theory, doing so might speed up performance.
            };

            config.setProperty("ignore", ignore);

            if (!config.save())
            {
                getServer().getLogger().warning(this.name + "Unable to save config. Ignoring.");

                // Manually add defaults, in case saving fails.

                for(String material : ignore)
                {
                    this.ignore.add(Material.matchMaterial(material).getId());
                }
            }

            config.load();
        }

        for(String material : config.getStringList("ignore", new ArrayList<String>()))
        {
            this.ignore.add(Material.matchMaterial(material).getId());
        }

        getServer().getLogger().info(this.name + "Plugin enabled");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisable()
    {
        this.ignore.clear();

        getServer().getLogger().info(this.name + "Plugin disabled");
    }

    /**
     * Function called when a BlockBreakEvent occurs.
     * Checks if player is holding a gold tool, then
     * checks if the block should be ignored or not.
     * Breaks the block, damages the tool durability,
     * and destroys the tool if necessary. Finally,
     * it naturally drops a block of the same type.
     *
     * @param event BlockBreakEvent that occurred.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        ItemStack tool = player.getItemInHand();

        if (this.tools.contains(tool.getType()))
        {
            Block block = event.getBlock();
            Material material = block.getType();

            if (this.ignore.contains((Integer) material.getId()))
            {
                return;
            }

            event.setCancelled(true);
            block.setType(Material.AIR);

            tool.setDurability((short) (tool.getDurability() + 1));

            if (tool.getDurability() > tool.getType().getMaxDurability())
            {
                player.setItemInHand(null);
            }

            Location location = block.getLocation();
            World world = player.getWorld();
            world.dropItemNaturally(location, new ItemStack(material, 1));
        }
    }
}
