package com.kfalk.conquesttowns.data;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;


public class PageGen {

    private Map<Integer, List<String>> pages = new HashMap<Integer, List<String>>();

    private static String title = ChatColor.DARK_GREEN + "ConquestTowns //" + ChatColor.DARK_GRAY + "---" + ChatColor.YELLOW + " ConquestTowns Help " + ChatColor.DARK_GRAY + "---";
    private int maxPage;

    public PageGen(List<String> lines, int numPerPage) {
        pages.clear();

        int count = 1;

        List<String> localCopy = new ArrayList<String>(lines);
        Collections.sort(localCopy);

        List<String> pageData = new ArrayList<String>();

        for (String s : localCopy) {
            pageData.add(s);

            if (pageData.size() >= numPerPage) {
                pages.put(count, new ArrayList<String>(pageData));
                count++;
                pageData.clear();
            }
        }

        if (!pageData.isEmpty()) {
            pages.put(count, pageData);
        }

        maxPage = count;
    }

    public void sendPage(Player p, int page) {
        if (page > maxPage || page < 1) {
            p.sendMessage(ChatColor.RED + "Invalid page number. Valid pages[1-" + maxPage + "]");
        } else {
            p.sendMessage(title);
            List<String> l = pages.get(page);
            p.sendMessage(l.toArray(new String[l.size()]));
            p.sendMessage(ChatColor.GRAY + "Page [" + page + "/" + maxPage + "]");
        }
    }

}
