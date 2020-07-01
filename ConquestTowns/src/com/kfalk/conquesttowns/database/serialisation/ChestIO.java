package com.kfalk.conquesttowns.database.serialisation;

import com.kfalk.conquesttowns.database.serialisation.cardboard.CardboardBox;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;


public class ChestIO {

    public static ItemStack[] loadChest(File f) {
        SyncObjectIO io = new SyncObjectIO(f);
        io.read();

        if(io.doesObjectExist("contents")) {
            return CardboardBox.toItemStack((CardboardBox[]) io.getObject("contents"));
        } else {
            //return empty stack
            return new ItemStack[36];
        }
    }

    public static void saveChest(ItemStack[] contents, File f) {
        CardboardBox[] inv = new CardboardBox[36];
        int c = 0;
        for (ItemStack is : contents) {
            if (is == null) {
                inv[c] = null;
            } else {
                inv[c] = new CardboardBox(is);
            }
            c++;
        }

        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SyncObjectIO io = new SyncObjectIO(f);
        io.add("contents", inv);
        io.write();
    }

}
