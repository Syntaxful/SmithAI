package com.smithai.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CraftingHelper. Uses mocked Player and PlayerInventory so no
 * Bukkit server runtime is required.
 */
public class CraftingHelperTest {

    private final Material planks = MaterialCompat.get("OAK_PLANKS", "PLANKS", "WOOD");

    @Test
    public void testCraftSticksFromPlanks() {
        assertNotNull(planks, "planks material must exist in test classpath");
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);

        ItemStack[] contents = new ItemStack[] { new ItemStack(planks, 8) };
        when(inv.getContents()).thenReturn(contents);
        when(inv.containsAtLeast(any(ItemStack.class), anyInt())).thenAnswer(i -> {
            ItemStack req = i.getArgument(0);
            int amount = i.getArgument(1);
            for (ItemStack s : contents) {
                if (s != null && s.getType() == req.getType() && s.getAmount() >= amount) return true;
            }
            return false;
        });

        assertTrue(CraftingHelper.craft(player, "craft_stick"));
        assertEquals(6, contents[0].getAmount()); // 2 planks consumed
    }

    @Test
    public void testCraftFailsWithoutIngredients() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);

        ItemStack[] contents = new ItemStack[] { new ItemStack(Material.DIRT, 1) };
        when(inv.getContents()).thenReturn(contents);

        assertFalse(CraftingHelper.craft(player, "craft_stick"));
    }

    @Test
    public void testUnknownSkillReturnsFalse() {
        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getContents()).thenReturn(new ItemStack[0]);

        assertFalse(CraftingHelper.craft(player, "craft_xyz_nonsense"));
    }
}
