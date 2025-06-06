package com.magmaguy.elitemobs.adventurersguild;

import com.magmaguy.elitemobs.config.*;
import com.magmaguy.elitemobs.config.menus.premade.GuildRankMenuConfig;
import com.magmaguy.elitemobs.economy.EconomyHandler;
import com.magmaguy.elitemobs.quests.playercooldowns.PlayerQuestCooldowns;
import com.magmaguy.elitemobs.utils.CustomModelAdder;
import com.magmaguy.magmacore.util.ChatColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GuildRankMenuHandler implements Listener {

    private static final HashSet<Inventory> inventories = new HashSet<>();
    private static final ArrayList<Integer> rankSlots = new ArrayList<>(new ArrayList<>(List.of(
            4, 11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42)));
    private static final int prestigeRankSlot = 49;

    private static void selectUnlockedRank(Player player, int guildRank) {
        GuildRank.setActiveGuildRank(player, guildRank);
    }

    private static void selectRankToUnlock(Player player, int guildRank) {
        double price = tierPriceCalculator(guildRank, GuildRank.getGuildPrestigeRank(player));
        if (EconomyHandler.checkCurrency(player.getUniqueId()) < price) {
            player.sendMessage(GuildRankMenuConfig.getNotEnoughCurrencyMessage()
                    .replace("$neededAmount", tierPriceCalculator(guildRank, GuildRank.getGuildPrestigeRank(player)) + "")
                    .replace("$currentAmount", EconomyHandler.checkCurrency(player.getUniqueId()) + "")
                    .replace("$currencyName", EconomySettingsConfig.getCurrencyName()));
            return;
        }
        EconomyHandler.subtractCurrency(player.getUniqueId(), price);
        GuildRank.setActiveGuildRank(player, guildRank);
        GuildRank.setMaxGuildRank(player, guildRank);
        player.sendMessage(GuildRankMenuConfig.getUnlockMessage()
                .replace("$rankName", GuildRank.getRankName(GuildRank.getGuildPrestigeRank(player), guildRank))
                .replace("$price", tierPriceCalculator(guildRank, GuildRank.getGuildPrestigeRank(player)) + "")
                .replace("$currencyName", EconomySettingsConfig.getCurrencyName()));

        Bukkit.broadcastMessage(GuildRankMenuConfig.getBroadcastMessage()
                .replace("$player", player.getDisplayName())
                .replace("$rankName", GuildRank.getRankName(GuildRank.getGuildPrestigeRank(player), guildRank)));
        if (!AdventurersGuildConfig.getOnRankUpCommand().isEmpty())
            for (String command : AdventurersGuildConfig.getOnRankUpCommand())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("$player", player.getName())
                                .replace("$prestigeRank", GuildRank.getActiveGuildRank(player) + "")
                                .replace("$activeRank", GuildRank.getGuildPrestigeRank(player) + ""));
        player.playSound(player.getLocation(), SoundsConfig.guildRankUpSound, 1, 1);
    }

    private static void selectPrestigeUnlock(Player player) {
        if (GuildRank.getGuildPrestigeRank(player) >= 10) return;
        if (GuildRank.getActiveGuildRank(player) != 10 + GuildRank.getGuildPrestigeRank(player))
            return;
        if (EconomyHandler.checkCurrency(player.getUniqueId()) < tierPriceCalculator(GuildRank.getGuildPrestigeRank(player) + 12, GuildRank.getGuildPrestigeRank(player))) {
            player.sendMessage(GuildRankMenuConfig.getNotEnoughCurrencyMessage()
                    .replace("$neededAmount", tierPriceCalculator(GuildRank.getGuildPrestigeRank(player) + 12, GuildRank.getGuildPrestigeRank(player)) + "")
                    .replace("$currentAmount", EconomyHandler.checkCurrency(player.getUniqueId()) + "")
                    .replace("$currencyName", EconomySettingsConfig.getCurrencyName()));
            return;
        }
        EconomyHandler.setCurrency(player.getUniqueId(), 0);
        GuildRank.setMaxGuildRank(player, 1);
        GuildRank.setActiveGuildRank(player, 1);
        GuildRank.setGuildPrestigeRank(player, GuildRank.getGuildPrestigeRank(player) + 1);
        PlayerQuestCooldowns.resetPlayerQuests(player);

        for (Player iteratedPlayer : Bukkit.getOnlinePlayers()) {
            iteratedPlayer.sendTitle(
                    ChatColorConverter.convert(AdventurersGuildConfig.getPrestigeUnlockMessageTitle().replace("$player", player.getDisplayName())
                            .replace("$tier", GuildRank.getGuildPrestigeRank(player) + "")),
                    ChatColorConverter.convert(AdventurersGuildConfig.getPrestigeUnlockMessageSubtitle().replace("$player", player.getDisplayName())
                            .replace("$tier", GuildRank.getGuildPrestigeRank(player) + "")));
        }
        player.closeInventory();
        if (!AdventurersGuildConfig.getOnPrestigeUpCommand().isEmpty())
            for (String command : AdventurersGuildConfig.getOnPrestigeUpCommand())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        command.replace("$player", player.getName())
                                .replace("$prestigeRank", GuildRank.getActiveGuildRank(player) + "")
                                .replace("$activeRank", GuildRank.getGuildPrestigeRank(player) + ""));
        player.playSound(player.getLocation(), SoundsConfig.guildPrestigeSound, 1, 1);
    }

    /**
     * Using the following slots for the menu:
     * 4 - Commoner rank
     * Normal ranks:
     * 11, 12, 13, 14, 15
     * 20, 21, 22, 23, 24
     * 39, 30, 31, 32, 34
     * 38, 39, 40, 41, 42
     * 49 - Prestige button
     *
     * @param player Player for whom the menu will be generated
     * @return Returns the inventory generated
     */
    public static Inventory initializeGuildRankMenu(Player player) {
        String menuName = GuildRankMenuConfig.getMenuName();
        if (DefaultConfig.isForceMenuUnicode() || Bukkit.getPluginManager().isPluginEnabled("ResourcePackManager"))
            menuName = ChatColor.WHITE + "\uF801\uDA90\uDEFA\uF805           " + menuName;
        Inventory difficultyMenu = Bukkit.createInventory(player, 54, menuName);
        difficultyMenu = populateInventory(difficultyMenu, player);
        inventories.add(difficultyMenu);
        player.openInventory(difficultyMenu);
        return difficultyMenu;
    }

    public static Inventory populateInventory(Inventory difficultyMenu, Player player) {
        for (int i = 0; i < 11 + GuildRank.getGuildPrestigeRank(player); i++) {
            if (AdventurersGuildConfig.isDisableCommonerRank() && i == 0) continue;
            ItemStack itemStack = null;
            if (GuildRank.isAtOrAboveGuildRank(player, i)) {
                itemStack = difficultyItemStackConstructor(guildRankStatus.UNLOCKED, i, player, false);
            }
            if (GuildRank.getActiveGuildRank(player) < i && GuildRank.getMaxGuildRank(player) >= i) {
                itemStack = difficultyItemStackConstructor(guildRankStatus.UNLOCKED, i, player, false);
            }
            if (GuildRank.getActiveGuildRank(player) == i) {
                itemStack = difficultyItemStackConstructor(guildRankStatus.SELECTED, i, player, false);
            }
            if (GuildRank.getMaxGuildRank(player) < i) {
                itemStack = difficultyItemStackConstructor(guildRankStatus.LOCKED, i, player, false);
            }
            if (GuildRank.getMaxGuildRank(player) + 1 == i) {
                itemStack = difficultyItemStackConstructor(guildRankStatus.NEXT_UNLOCK, i, player, false);
            }
            difficultyMenu.setItem(rankSlots.get(i), itemStack);
        }

        if (GuildRank.getGuildPrestigeRank(player) < 10) {
            if (GuildRank.getActiveGuildRank(player) < 10 + GuildRank.getGuildPrestigeRank(player))
                difficultyMenu.setItem(prestigeRankSlot, difficultyItemStackConstructor
                        (guildRankStatus.PRESTIGE_LOCKED, GuildRank.getGuildPrestigeRank(player) + 1, player, true));
            else
                difficultyMenu.setItem(prestigeRankSlot, difficultyItemStackConstructor
                        (guildRankStatus.PRESTIGE_NEXT_UNLOCK, GuildRank.getGuildPrestigeRank(player) + 1, player, true));
        }

        return difficultyMenu;
    }

    private static ItemStack difficultyItemStackConstructor(guildRankStatus guildRankStatus, int activeGuildRank, Player player, boolean isPrestigeSlot) {

        ItemStack itemStack = null;

        String priceString = "";
        if (!isPrestigeSlot)
            if (tierPriceCalculator(activeGuildRank, GuildRank.getGuildPrestigeRank(player)) > EconomyHandler.checkCurrency(player.getUniqueId()))
                priceString = "&c" + tierPriceCalculator(activeGuildRank, GuildRank.getGuildPrestigeRank(player));
            else
                priceString = "&a" + tierPriceCalculator(activeGuildRank, GuildRank.getGuildPrestigeRank(player));
        else {
            if (tierPriceCalculator(GuildRank.getGuildPrestigeRank(player) + 12, GuildRank.getGuildPrestigeRank(player)) > EconomyHandler.checkCurrency(player.getUniqueId()))
                priceString = "&c" + tierPriceCalculator(GuildRank.getGuildPrestigeRank(player) + 12, GuildRank.getGuildPrestigeRank(player));
            else
                priceString = "&a" + tierPriceCalculator(GuildRank.getGuildPrestigeRank(player) + 12, GuildRank.getGuildPrestigeRank(player));
        }
        switch (guildRankStatus) {
            case UNLOCKED:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getUnlockedButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.grayLock);
                break;
            case LOCKED:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getLockedButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.redLock);
                break;
            case SELECTED:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getCurrentButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.greenLock);
                break;
            case NEXT_UNLOCK:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getNextButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.yellowLock);
                break;
            case PRESTIGE_LOCKED:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getPrestigeLockedButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.redCrown);
                break;
            case PRESTIGE_NEXT_UNLOCK:
                itemStack = constructButtonItemStack(activeGuildRank, GuildRankMenuConfig.getPrestigeNextUnlockButton().clone(), player, priceString);
                CustomModelAdder.addCustomModel(itemStack, CustomModelsConfig.yellowCrown);
                break;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (guildRankStatus.equals(GuildRankMenuHandler.guildRankStatus.UNLOCKED) ||
                guildRankStatus.equals(GuildRankMenuHandler.guildRankStatus.LOCKED) ||
                guildRankStatus.equals(GuildRankMenuHandler.guildRankStatus.SELECTED) ||
                guildRankStatus.equals(GuildRankMenuHandler.guildRankStatus.NEXT_UNLOCK))
            itemMeta.setDisplayName(GuildRank.getRankName(GuildRank.getGuildPrestigeRank(player), activeGuildRank));
        else
            itemMeta.setDisplayName(itemMeta.getDisplayName().replace("$rank", GuildRank.getGuildPrestigeRank(player) + 1 + ""));
        itemStack.setItemMeta(itemMeta);

        return itemStack;

    }

    private static ItemStack constructButtonItemStack(int activeGuildRank, ItemStack itemStack, Player player, String priceString) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> newLore = new ArrayList<>();
        for (String string : itemMeta.getLore()) {
            switch (string) {
                case "$lootTier":
                    if (lootTierString(activeGuildRank) != null)
                        newLore.add(lootTierString(activeGuildRank));
                    break;
                case "$maxHealthIncrease":
                    if (healthBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank) != null)
                        newLore.add(healthBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank));
                    break;
                case "$chanceToCrit":
                    if (critBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank) != null)
                        newLore.add(critBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank));
                    break;
                case "$chanceToDodge":
                    if (dodgeBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank) != null)
                        newLore.add(dodgeBonusString(GuildRank.getGuildPrestigeRank(player), activeGuildRank));
                    break;
                case "$currencyBonusMessage":
                    if (GuildRank.getGuildPrestigeRank(player) > 0)
                        newLore.add(currencyBonusString(GuildRank.getGuildPrestigeRank(player)));
                    break;
                default:
                    if (string.contains("$previousRank")) {
                        newLore.add(string.replace("$previousRank",
                                (GuildRank.getRankName(GuildRank.getGuildPrestigeRank(player), GuildRank.getMaxGuildRank(player) + 1))));
                        continue;
                    }
                    if (string.contains("$price")) {
                        newLore.add(
                                ChatColorConverter.convert(
                                        string.replace("$price", priceString)
                                                .replace("$currencyName", EconomySettingsConfig.getCurrencyName())));
                        continue;
                    }
                    if (string.contains("$currentCurrency")) {
                        newLore.add(string
                                .replace("$currentCurrency", EconomyHandler.checkCurrency(player.getUniqueId()) + "")
                                .replace("$currencyName", EconomySettingsConfig.getCurrencyName()));
                        continue;
                    }
                    newLore.add(string);
                    break;
            }
        }
        itemMeta.setLore(newLore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static String lootTierString(int activeGuildRank) {
        if (!AdventurersGuildConfig.isGuildLootLimiter()) return null;
        return GuildRankMenuConfig.getLootTierMessage().replace("$tier", GuildRank.lootTierValue(activeGuildRank) + "");
    }

    private static String currencyBonusString(int activeGuildRank) {
        return GuildRankMenuConfig.getCurrencyBonusMessage().replace("$amount", GuildRank.currencyBonusMultiplier(activeGuildRank) + "");
    }

    private static String healthBonusString(int prestigeLevel, int guildRank) {
        if (!AdventurersGuildConfig.isAddMaxHealth()) return null;
        if (prestigeLevel < 1)
            return null;
        return GuildRankMenuConfig.getHealthBonusMessage().replace("$amount", GuildRank.healthBonusValue(prestigeLevel, guildRank) + "");
    }

    private static String critBonusString(int prestigeLevel, int guildRank) {
        if (!AdventurersGuildConfig.isAddMaxHealth()) return null;
        if (prestigeLevel < 2)
            return null;
        return GuildRankMenuConfig.getCritBonusMessage().replace("$amount", GuildRank.critBonusValue(prestigeLevel, guildRank) + "");
    }

    private static String dodgeBonusString(int prestigeLevel, int guildRank) {
        if (!AdventurersGuildConfig.isAddMaxHealth()) return null;
        if (prestigeLevel < 3)
            return null;
        return GuildRankMenuConfig.getDodgeBonusMessage().replace("$amount", GuildRank.dodgeBonusValue(prestigeLevel, guildRank) + "");
    }

    private static int tierPriceCalculator(int tier, int prestigeLevel) {
        /*Logic:
        tier * 10 = max mob tier
        max mob tier / 2 = loot shower payout for killing 1 em at max level for that player
        payout * 1000 = amount of elite mobs to kill before going up a rank
         */
        double eliteMobsToKillBeforeGuildRankUp = AdventurersGuildConfig.getBaseKillsForRankUp() + AdventurersGuildConfig.getAdditionalKillsForRankUpPerTier() * (tier - 1);
        return (int) ((tier - 1) * 10D / 2D * eliteMobsToKillBeforeGuildRankUp * (GuildRank.currencyBonusMultiplier(prestigeLevel)));
    }

    @EventHandler
    public void onRankSelectorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!inventories.contains(event.getInventory())) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        event.setCancelled(true);

        if (event.getSlot() == prestigeRankSlot) {
            selectPrestigeUnlock(player);
            return;
        }
        Integer selectedRank = null;

        for (int i = 0; i < rankSlots.size(); i++) {
            if (rankSlots.get(i) == event.getSlot()) {
                if (AdventurersGuildConfig.isDisableCommonerRank() && i == 4) continue;
                selectedRank = i;
                break;
            }
        }

        //Clicked nothing
        if (selectedRank == null) return;

        if (GuildRank.getMaxGuildRank(player) >= selectedRank) {
            selectUnlockedRank(player, selectedRank);
            GuildRankMenuHandler.populateInventory(event.getInventory(), player);
            return;
        }

        if (GuildRank.getMaxGuildRank(player) + 1 == selectedRank) {
            selectRankToUnlock(player, selectedRank);
            GuildRankMenuHandler.populateInventory(event.getInventory(), player);
            return;
        }

        if (GuildRank.getMaxGuildRank(player) < selectedRank) {
            selectPrestigeUnlock(player);
            GuildRankMenuHandler.populateInventory(event.getInventory(), player);
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        inventories.remove(event.getInventory());
    }

    private enum guildRankStatus {
        UNLOCKED,
        SELECTED,
        NEXT_UNLOCK,
        LOCKED,
        PRESTIGE_LOCKED,
        PRESTIGE_NEXT_UNLOCK,
    }

}
