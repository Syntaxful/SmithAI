package com.smithai.npc;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors and manages a Smith_AI NPC's inventory. When the NPC is a Player entity
 * (via Citizens), this operates on the real PlayerInventory. For non-Player NPCs
 * (e.g. villager fallback), items are stored in a small virtual inventory.
 */
public class NPCInventory {

    private final PlayerInventory inventory;
    private final List<ItemStack> virtualItems;
    private final boolean isReal;

    public NPCInventory(Player player) {
        this.inventory = player.getInventory();
        this.virtualItems = null;
        this.isReal = true;
    }

    public NPCInventory() {
        this.inventory = null;
        this.virtualItems = new ArrayList<>();
        this.isReal = false;
    }

    public boolean isRealInventory() {
        return isReal;
    }

    public void addItem(ItemStack stack) {
        if (isReal && inventory != null) {
            inventory.addItem(stack.clone());
        } else if (virtualItems != null) {
            virtualItems.add(stack.clone());
        }
    }

    public boolean contains(Material material) {
        if (isReal && inventory != null) {
            return inventory.contains(material);
        }
        if (virtualItems != null) {
            for (ItemStack stack : virtualItems) {
                if (stack != null && stack.getType() == material) return true;
            }
        }
        return false;
    }

    public int count(Material material) {
        int total = 0;
        if (isReal && inventory != null) {
            for (ItemStack stack : inventory.getContents()) {
                if (stack != null && stack.getType() == material) total += stack.getAmount();
            }
        } else if (virtualItems != null) {
            for (ItemStack stack : virtualItems) {
                if (stack != null && stack.getType() == material) total += stack.getAmount();
            }
        }
        return total;
    }

    public void remove(Material material, int amount) {
        if (amount <= 0) return;
        if (isReal && inventory != null) {
            ItemStack[] contents = inventory.getContents();
            for (int i = 0; i < contents.length && amount > 0; i++) {
                ItemStack stack = contents[i];
                if (stack != null && stack.getType() == material) {
                    if (stack.getAmount() <= amount) {
                        amount -= stack.getAmount();
                        inventory.setItem(i, null);
                    } else {
                        stack.setAmount(stack.getAmount() - amount);
                        inventory.setItem(i, stack);
                        amount = 0;
                    }
                }
            }
        } else if (virtualItems != null) {
            List<ItemStack> toRemove = new ArrayList<>();
            for (ItemStack stack : virtualItems) {
                if (stack != null && stack.getType() == material && amount > 0) {
                    if (stack.getAmount() <= amount) {
                        amount -= stack.getAmount();
                        toRemove.add(stack);
                    } else {
                        stack.setAmount(stack.getAmount() - amount);
                        amount = 0;
                    }
                }
            }
            virtualItems.removeAll(toRemove);
        }
    }

    public ItemStack[] getContents() {
        if (isReal && inventory != null) {
            return inventory.getContents();
        }
        if (virtualItems != null) {
            return virtualItems.toArray(new ItemStack[0]);
        }
        return new ItemStack[0];
    }

    public void setArmor(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        if (isReal && inventory != null) {
            inventory.setHelmet(helmet);
            inventory.setChestplate(chestplate);
            inventory.setLeggings(leggings);
            inventory.setBoots(boots);
        }
    }

    public void setMainHand(ItemStack item) {
        if (isReal && inventory != null) {
            inventory.setItemInMainHand(item);
        }
    }

    public void setOffHand(ItemStack item) {
        if (isReal && inventory != null) {
            inventory.setItemInOffHand(item);
        }
    }

    public ItemStack getHelmet() {
        return isReal && inventory != null ? inventory.getHelmet() : null;
    }

    public ItemStack getChestplate() {
        return isReal && inventory != null ? inventory.getChestplate() : null;
    }

    public ItemStack getLeggings() {
        return isReal && inventory != null ? inventory.getLeggings() : null;
    }

    public ItemStack getBoots() {
        return isReal && inventory != null ? inventory.getBoots() : null;
    }

    public ItemStack getItemInMainHand() {
        return isReal && inventory != null ? inventory.getItemInMainHand() : null;
    }

    public ItemStack getItemInOffHand() {
        return isReal && inventory != null ? inventory.getItemInOffHand() : null;
    }

    public void setHeldItemSlot(int slot) {
        if (isReal && inventory != null) {
            inventory.setHeldItemSlot(Math.max(0, Math.min(8, slot)));
        }
    }

    public int getHeldItemSlot() {
        return isReal && inventory != null ? inventory.getHeldItemSlot() : 0;
    }

    public int firstEmpty() {
        if (isReal && inventory != null) {
            return inventory.firstEmpty();
        }
        return -1;
    }

    public void setItem(int slot, ItemStack item) {
        if (isReal && inventory != null) {
            inventory.setItem(slot, item);
        }
    }

    public ItemStack getItem(int slot) {
        if (isReal && inventory != null) {
            return inventory.getItem(slot);
        }
        return null;
    }
}
