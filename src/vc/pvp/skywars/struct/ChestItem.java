/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package vc.pvp.skywars.struct;

import org.bukkit.inventory.ItemStack;

/**
 *
 * @author devan_000
 */
public class ChestItem {

    private final ItemStack item;
    private final int chance;

    public ChestItem(ItemStack item, int chance) {
        this.item = item;
        this.chance = chance;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getChance() {
        return chance;
    }

}
