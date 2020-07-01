package com.kfalk.conquesttowns;

import com.kfalk.conquesttowns.commands.AdminCommands;
import com.kfalk.conquesttowns.commands.SettlementCommands;
import com.kfalk.conquesttowns.commands.TownCommands;
import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.database.GeneralConfig;
import com.kfalk.conquesttowns.database.SettlementManager;
import com.kfalk.conquesttowns.database.TownManager;
import com.kfalk.conquesttowns.listeners.PlayerListener;
import com.kfalk.conquesttowns.listeners.TagColouring;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


public class ConquestTowns extends JavaPlugin {

    public static final File root = new File("plugins" + File.separator + "ConquestTowns"),
            towns_root = new File(root + File.separator + "Towns");

    public static Plugin plugin;
    public static Logger logger;

    public static Economy economy = null;

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        root.mkdir();
        towns_root.mkdir();

        logger.info(setupEconomy() ? "Hooked into Vault economy successfully." : "Failed to hook into Vault economy!");

        GeneralConfig.init();

        SettlementManager.init();

        TownManager.init();

        getCommand("settlement").setExecutor(new SettlementCommands());
        getCommand("town").setExecutor(new TownCommands());
        getCommand("towna").setExecutor(new AdminCommands());

        //register listeners
        new PlayerListener(this);
        new TagColouring(this);

        BukkitRunnable msgs = new BukkitRunnable() {

            Map<UUID, Town> log = new HashMap<UUID, Town>();

            @Override
            public void run() {
                //run every second
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Town in = TownManager.getFromLocation(p.getLocation());

                    Town last = log.containsKey(p.getUniqueId()) ? log.get(p.getUniqueId()) : null;

                    if (last == null && in == null) {
                        continue;
                    } else if (last == null && in != null) {
                        //show entry
                        log.put(p.getUniqueId(), in);
                        p.sendMessage(ChatColor.GREEN + "You have entered " + in.getTownName());

                        if(in.isAtWar()) {
                            Town players = TownManager.getPlayerTown(p.getUniqueId());
                            if (players != null && in.getWarringWith().equalsIgnoreCase(players.getTownName())) {
                                //announce raider!
                                in.broadcastMessage(null, ChatColor.RED + p.getName() + " has entered your town!");
                            }
                        }
                    } else if (last != null && in == null) {
                        //show exit
                        log.remove(p.getUniqueId());
                        p.sendMessage(ChatColor.RED + "You have left " + last.getTownName());
                    }
                }
            }
        };
        Bukkit.getScheduler().runTaskTimer(this, msgs, 0, 20);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    public static void debug(String s) {
        if (GeneralConfig.debug) {
            logger.info("[DEBUG] " + s);
        }
    }
}
