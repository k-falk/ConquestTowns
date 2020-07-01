package com.kfalk.conquesttowns.data;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.api.ConquestTownsAPI;
import com.kfalk.conquesttowns.database.InventoryManager;
import com.kfalk.conquesttowns.database.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.kitteh.tag.TagAPI;

import java.util.*;


public class WarInstance implements Listener, Runnable {

    List<Town> towns;

    //one hour
    private int counter = 60 * 60;
    private int secondsElapsedSinceDamage = 0;

    private Scoreboard board;
    private Objective objective;
    private BukkitTask task;

    /*
        War stops, after
        - 1 hour time
        - 5 minutes after some rewards have been stolen, give random stack!
        - 15 minutes after no combat
        - 10 minutes after a town has less than needed players (non reversible)
     */

    public WarInstance(Town town1, Town town2) {
        towns = new ArrayList<Town>(Arrays.asList(town1, town2));

        //set scoreboards!
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective("war" + town1.getTownName(), "dummy");

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GOLD + "   " + formatTime(counter) + "   ");

        objective.getScore(ChatColor.RED + town1.getTownName()).setScore(town1.getOnlinePlayers());
        objective.getScore(ChatColor.RED + town2.getTownName()).setScore(town2.getOnlinePlayers());

        //set board for all active players
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (town1.isMember(p.getUniqueId()) || town2.isMember(p.getUniqueId())) {
                p.setScoreboard(board);
            }
        }

        Bukkit.getPluginManager().registerEvents(this, ConquestTowns.plugin);

        //run every second
        task = Bukkit.getScheduler().runTaskTimer(ConquestTowns.plugin, this, 0, 20);
    }

    private boolean firstSteal = true;

    public void rewardsStolen(Town stolenFrom) {
        if (firstSteal) {

            if (counter > (60 * 5)) {
                counter = 60 * 5;
            }

            for (Town t : towns) {
                if (t.getTownName().equals(stolenFrom.getTownName())) {
                    //stolen from this town
                    stolenFrom.broadcastMessage(null, ChatColor.RED + "Your town chest has been raided! War will end in 5 minutes.");
                } else {
                    t.broadcastMessage(null, ChatColor.RED + "You have raided the enemies town chest! War will end in 5 minutes.");
                }
            }

            firstSteal = false;
        }
    }

    public void endWar() {
        //kill task
        task.cancel();

        //detach handler
        HandlerList.unregisterAll(this);

        //scan inventory for chest items!
        players:
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Town t : towns) {
                if (t.isMember(p.getUniqueId())) {
                    //scan for rewards
                    ItemStack reward = null;

                    for (ItemStack stack : p.getInventory().getContents()) {
                        if (stack != null && InventoryManager.isChestRewardItem(stack)) {
                            reward = stack;
                            break;
                        }
                    }

                    if (reward != null) {
                        //remove
                        p.getInventory().remove(reward);
                        p.updateInventory();

                        Town stolenFrom = TownManager.getTownByName(t.getWarringWith());
                        ConquestTownsAPI.addRewards(stolenFrom, TownMaterial.fromMaterial(reward.getType()), reward.getAmount());
                    }

                    continue players;
                }
            }
        }

        //clear boards
        players:
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Town t : towns) {
                if (t.isMember(p.getUniqueId())) {
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                    continue players;
                }
            }
        }

        //reset both towns to peace mode
        for (Town town : towns) {
            town.setPeace();
            town.broadcastMessage(null, ChatColor.GREEN + "War is over! Your town is now at peace.");
        }

        Bukkit.broadcastMessage(ChatColor.GREEN + "Towns " + towns.get(0).getTownName() + " and " + towns.get(1).getTownName() + " are no longer at war.");

        //towns at peace, now reset all name tags
        for (Town town : towns) {
            for (UUID uuid : town.getGroup().getMembers().keySet()) {
                Player p = Bukkit.getPlayer(uuid);

                if (p != null) {
                    //refresh for ALL
                    TagAPI.refreshPlayer(p);
                }
            }
        }
    }

    @EventHandler
    private void death(PlayerDeathEvent evt) {
        //scan for rewards, if player is killed return their drops to the chest they were stolen from
        ItemStack reward = null;

        Iterator<ItemStack> it = evt.getDrops().iterator();

        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack != null && InventoryManager.isChestRewardItem(stack)) {
                reward = stack;
                it.remove();
                break;
            }
        }

        if (reward != null) {
            //remove
            Town t = TownManager.getPlayerTown(evt.getEntity().getUniqueId());
            Town stolenFrom = TownManager.getTownByName(t.getWarringWith());
            ConquestTownsAPI.addRewards(stolenFrom, TownMaterial.fromMaterial(reward.getType()), reward.getAmount());
        }
    }

    @EventHandler
    private void damage(EntityDamageByEntityEvent evt) {
        if (evt.getEntity() instanceof Player && evt.getDamager() instanceof Player) {
            Player damaged = (Player) evt.getEntity(),
                    damager = (Player) evt.getDamager();

            Town damagedTown = TownManager.getPlayerTown(damaged.getUniqueId());

            boolean ignore = true;

            for (Town town : towns) {
                if (town.getTownName().equalsIgnoreCase(damagedTown.getTownName())) {
                    ignore = false;
                    break;
                }
            }

            if (ignore) {
                return;
            }

            if (damagedTown == null) {
                //they cant be in the same town
                return;
            } else {
                //now quicker to just check if the other player is a member of the town, as this is a simple hashmap lookup, quicker than iteration search
                if (damagedTown.isMember(damager.getUniqueId())) {
                    //stop hurting town members!
                    evt.setCancelled(true);
                } else {

                    //are they hitting a member from the town they are at war with
                    //lets just do existance check ofr both

                    int c = 0;

                    for (Town t : towns) {
                        if (t.isMember(damaged.getUniqueId()) || t.isMember(damager.getUniqueId())) {
                            c++;
                        }
                    }

                    if (c != 2) {
                        return;
                    } else {
                        //they are warring
                        secondsElapsedSinceDamage = 0;
                    }

                }
            }

        }
    }

    @Override
    public void run() {
        counter--;
        secondsElapsedSinceDamage++;

        objective.setDisplayName(ChatColor.GOLD + "   " + formatTime(counter) + "   ");

        if (secondsElapsedSinceDamage > (15 * 60)) {
            //stop war
            endWar();
            for (Town t : towns) {
                t.broadcastMessage(null, ChatColor.RED + "War has ended due to lack of combat.");
            }
        }
    }

    @EventHandler
    private void join(PlayerJoinEvent evt) {
        Player p = evt.getPlayer();
        for (Town t : towns) {
            if (t.isMember(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "Your town is currently at war with " + t.getWarringWith() + "!");
                p.setScoreboard(board);
                break;
            }
        }
    }

    @EventHandler
    private void quit(PlayerQuitEvent evt) {
        Player p = evt.getPlayer();
        for (Town t : towns) {
            if (t.isMember(p.getUniqueId())) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

                //scan for rewards, if they have any return it to the town chest
                ItemStack reward = null;

                for (ItemStack stack : p.getInventory().getContents()) {
                    if (stack != null && InventoryManager.isChestRewardItem(stack)) {
                        reward = stack;
                        break;
                    }
                }

                if (reward != null) {
                    //remove
                    p.getInventory().remove(reward);
                    p.updateInventory();

                    Town stolenFrom = TownManager.getTownByName(t.getWarringWith());
                    ConquestTownsAPI.addRewards(stolenFrom, TownMaterial.fromMaterial(reward.getType()), reward.getAmount());
                }
            }
        }
    }

    private String formatTime(int seconds) {
        //format 00:00

        if (seconds < 10) {
            return "00:0" + seconds;
        } else if (seconds < 60) {
            return "00:" + seconds;
        } else {
            int minutes = (int) Math.floor(((double) seconds) / 60D);
            int sLeft = seconds % 60;

            StringBuilder frmt = new StringBuilder();
            frmt.append(minutes < 10 ? "0" : "").append(minutes).append(":").append(sLeft < 10 ? "0" : "").append(sLeft);
            return frmt.toString();
        }
    }
}
