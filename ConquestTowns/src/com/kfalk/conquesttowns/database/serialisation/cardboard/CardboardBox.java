package com.kfalk.conquesttowns.database.serialisation.cardboard;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A serializable ItemStack
 */
public class CardboardBox implements Serializable {
	private static final long serialVersionUID = 729890133797629668L;

	private int type, amount;
	private short damage;
	private byte data;

	private HashMap<CardboardEnchantment, Integer> enchants;
	
	boolean NULL = false;

	public CardboardBox(ItemStack item) {
		if(item == null){
			NULL = true;
			return;
		}
		this.type = item.getTypeId();
		this.amount = item.getAmount();
		this.damage = item.getDurability();
		this.data = item.getData().getData();

		HashMap<CardboardEnchantment, Integer> map = new HashMap<CardboardEnchantment, Integer>();

		Map<Enchantment, Integer> enchantments = item.getEnchantments();

		for (Enchantment enchantment : enchantments.keySet()) {
			map.put(new CardboardEnchantment(enchantment), enchantments.get(enchantment));
		}

		this.enchants = map;
	}

	public ItemStack unbox() {
		if(NULL){
			return null;
		}
		ItemStack item = new ItemStack(type, amount, damage, data);

		HashMap<Enchantment, Integer> map = new HashMap<Enchantment, Integer>();

		for (CardboardEnchantment cEnchantment : enchants.keySet()) {
			map.put(cEnchantment.unbox(), enchants.get(cEnchantment));
		}

		item.addUnsafeEnchantments(map);

		return item;
	}
	
	public static ItemStack[] toItemStack(CardboardBox[] b){
		ItemStack[] i = new ItemStack[b.length];
		for(int e = 0; e < b.length; e++){
			if(b[e] == null){
				i[e] = null;
			} else {
				i[e] = b[e].unbox();
			}
		}
		return i;
	}
}