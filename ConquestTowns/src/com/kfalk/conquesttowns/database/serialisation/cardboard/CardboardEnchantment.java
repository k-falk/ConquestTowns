package com.kfalk.conquesttowns.database.serialisation.cardboard;
import org.bukkit.enchantments.Enchantment;

import java.io.Serializable;

/**
* A serializable Enchantment
*/
public class CardboardEnchantment implements Serializable {
    private static final long serialVersionUID = 8973856768102665381L;
 
    private final int id;
 
    public CardboardEnchantment(Enchantment enchantment) {
        this.id = enchantment.getId();
    }
 
    public Enchantment unbox() {
        return Enchantment.getById(this.id);
    }
}