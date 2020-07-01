package com.kfalk.conquesttowns.database;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.data.RankedGroup;
import com.kfalk.conquesttowns.data.PlayerInvite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class SettlementManager {

    private static final File settlementsRoot = new File(ConquestTowns.root + File.separator + "Settlements"), settlementsData = new File(settlementsRoot +
            File.separator + "settlements.dat");

    public static List<RankedGroup> settlements = new ArrayList<RankedGroup>();

    private static List<PlayerInvite> invites = new ArrayList<PlayerInvite>();

    public static void init() {
        settlementsRoot.mkdir();
        if (!settlementsData.exists()) {
            try {
                settlementsData.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
            Format:

            <UUID of Owner>
                - UUID of members
         */

        FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);

        int loaded = 0;

        for (String owner : io.getKeys(false)) {

            UUID ownerUUID = UUID.fromString(owner);
            Set<UUID> members = new HashSet<UUID>();
            members.add(ownerUUID);

            for (String s : io.getStringList(owner)) {
                members.add(UUID.fromString(s));
            }

            RankedGroup group = new RankedGroup(ownerUUID, members);
            settlements.add(group);

            loaded++;
        }

        ConquestTowns.logger.info("Loaded [" + loaded + "] settlements.");
    }

    private static boolean inSettlement(UUID u) {
        for (RankedGroup group : settlements) {
            if (group.isMember(u)) {
                return true;
            }
        }
        return false;
    }

    private static RankedGroup getSettlement(UUID u) {
        for (RankedGroup group : settlements) {
            if (group.isMember(u)) {
                return group;
            }
        }
        return null;
    }

    public static void createSettlement(Player p) {
        if (!inSettlement(p.getUniqueId())) {
        	if(TownManager.getPlayerTown(p.getUniqueId())== null){
            settlements.add(new RankedGroup(p.getUniqueId(), new ArrayList<UUID>(Arrays.asList(p.getUniqueId()))));

            FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);
            io.set(p.getUniqueId().toString(), new String[]{});
            try {
                io.save(settlementsData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            p.sendMessage(ChatColor.GREEN + "Settlement created.");
        	}else{
                p.sendMessage(ChatColor.RED + "You already belong to a town.");
        	}
        }
        else {
        
            p.sendMessage(ChatColor.RED + "You already belong to another settlement.");
        }
    }

    public static void disbandSettlement(Player p, boolean quiet) {
        if (!inSettlement(p.getUniqueId())) {
            if (!quiet) {
                p.sendMessage(ChatColor.RED + "You do not belong to a settlement.");
            }
        } else {
            Iterator<RankedGroup> it = settlements.iterator();

            while (it.hasNext()) {
                RankedGroup group = it.next();

                if (group.isMember(p.getUniqueId())) {
                    if (group.getOwner().equals(p.getUniqueId())) {
                        //remove!
                        it.remove();

                        FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);
                        io.set(p.getUniqueId().toString(), null);
                        try {
                            io.save(settlementsData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!quiet) {
                            p.sendMessage(ChatColor.GREEN + "Settlement disbanded successfully.");
                        }
                    } else {
                        if (!quiet) {
                            p.sendMessage(ChatColor.RED + "You must be the owner of a settlement to disband it.");
                        }
                    }
                    return;
                }
            }

        }
    }

    public static void leaveOwnSettlement(Player p) {
        RankedGroup group = getSettlement(p.getUniqueId());

        if (group != null) {

            if (group.getOwner().equals(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "As the owner of a settlement you cannot leave, but only disband it.");
                return;
            }

            group.removeMember(p.getUniqueId());
            FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);
            io.set(p.getUniqueId().toString(), group.membersToStringArray(false));
            try {
                io.save(settlementsData);
            } catch (IOException e) {
                e.printStackTrace();
            }

            p.sendMessage(ChatColor.GREEN + "You left the settlement successfully.");

        } else {
            p.sendMessage(ChatColor.RED + "You do not belong to a settlement.");
        }
    }

    public static void removeMember(Player p, String member) {
        RankedGroup group = getSettlement(p.getUniqueId());

        if (group != null) {

            if (group.getOwner().equals(p.getUniqueId())) {

                OfflinePlayer on = Bukkit.getPlayer(member);

                UUID target = null;

                if (on != null) {
                    //this player is online
                    target = on.getUniqueId();
                } else {
                    //player is offline
                    on = Bukkit.getOfflinePlayer(member);

                    if (on != null) {
                        target = on.getUniqueId();
                    } else {
                        p.sendMessage(ChatColor.RED + "No player by this name was found.");
                        return;
                    }
                }

                if (target.equals(p.getUniqueId())) {
                    p.sendMessage(ChatColor.RED + "You cannot remove yourself from the settlement.");
                    p.sendMessage(ChatColor.RED + "/settlement disband " + ChatColor.WHITE + "to disband your settlement.");
                    return;
                }

                if (group.isMember(target)) {
                    group.removeMember(target);

                    FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);
                    io.set(p.getUniqueId().toString(), group.membersToStringArray(false));
                    try {
                        io.save(settlementsData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    p.sendMessage(ChatColor.GREEN + "Member removed from settlement successfully.");
                } else {
                    p.sendMessage(ChatColor.RED + "That player is not a member of your settlement.");
                }
            } else {
                p.sendMessage(ChatColor.RED + "Only the owner of a settlement may remove members.");
            }

        } else {
            p.sendMessage(ChatColor.RED + "You must own a settlement to do this.");
        }
    }

    public static void inviteToSettlement(Player p, String targetName) {
        Player other = Bukkit.getPlayer(targetName);

        RankedGroup group = getSettlement(p.getUniqueId());

        if (group != null) {

            if (group.getOwner().equals(p.getUniqueId())) {

                if (other == null) {
                    p.sendMessage(ChatColor.RED + "That player is not online.");
                    return;
                }

                UUID target = other.getUniqueId();

                if (group.isMember(target)) {
                    p.sendMessage(ChatColor.RED + "That player is already a member in your settlement.");
                    return;
                }

                for (PlayerInvite inv : invites) {
                    if (inv.getTarget().equals(target) && inv.getGroup().getOwner().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You have already sent an invitation to this player.");
                        return;
                    }
                }

                invites.add(new PlayerInvite(target, group, 120));

                p.sendMessage(ChatColor.GREEN + "Invitation sent successfully.");

                other.sendMessage(ChatColor.GREEN + "You have been invited to join " + p.getName() + "'s settlement.");
                other.sendMessage(ChatColor.GREEN + "/settlement join " + p.getName() + ChatColor.WHITE + " to join.");
                other.sendMessage(ChatColor.RED + "This invitation expires in 2 minutes.");
            } else {
                p.sendMessage(ChatColor.RED + "Only the owner of a settlement may invite players.");
            }

        } else {
            p.sendMessage(ChatColor.RED + "You must own a settlement to do this.");
        }
    }

    public static void acceptSettlementInvite(Player p, String name) {
        Iterator<PlayerInvite> it = invites.iterator();

        Player owner = Bukkit.getPlayer(name);
        UUID target = owner == null ? Bukkit.getOfflinePlayer(name).getUniqueId() : owner.getUniqueId();

        //are they in a settlement?
        for (RankedGroup group : settlements) {
            if (group.isMember(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "You must leave your current settlement before joining another.");
                return;
            }
        }

        while (it.hasNext()) {
            PlayerInvite inv = it.next();

            if (inv.getTarget().equals(p.getUniqueId()) && inv.getGroup().getOwner().equals(target)) {
                //could be a match
                if (inv.hasExpired()) {
                    p.sendMessage(ChatColor.RED + "This invitation has expired.");
                } else {
                    //add to settlement
                    inv.getGroup().addMember(p.getUniqueId());

                    FileConfiguration io = YamlConfiguration.loadConfiguration(settlementsData);
                    io.set(p.getUniqueId().toString(), inv.getGroup().membersToStringArray(false));
                    try {
                        io.save(settlementsData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    p.sendMessage(ChatColor.GREEN + "You have joined the settlement successfully.");
                }
                it.remove();
                return;
            }
        }

        p.sendMessage(ChatColor.RED + "This player has not invited you to their settlement.");
    }

    public static void showInfo(Player p) {
        RankedGroup group = getSettlement(p.getUniqueId());
        if (group == null) {
            p.sendMessage(ChatColor.RED + "You do not belong to a settlement.");
        } else {
            p.sendMessage(ChatColor.YELLOW + "Owner: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(group.getOwner()).getName());

            int members = group.getMembers().size() - 1;
            if (members > 0) {
                StringBuilder show = new StringBuilder();
                show.append(ChatColor.YELLOW).append("Members (").append(members).append("): ");
                for (UUID u : group.getMembers().keySet()) {
                    if (!u.equals(group.getOwner())) {
                        show.append(ChatColor.WHITE).append(Bukkit.getOfflinePlayer(u).getName()).append(ChatColor.YELLOW).append(", ");
                    }
                }
                show.setLength(show.length() - 2);
                p.sendMessage(show.toString());
            } else {
                p.sendMessage(ChatColor.YELLOW + "Members (0):");
            }
        }
    }

}
