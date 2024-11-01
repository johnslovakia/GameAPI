package cz.johnslovakia.gameapi.api;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

public interface VersionSupport {

    double getMaxPlayerHealth(Player player);
    void setMaxPlayerHealth(Player player, double max);
    ItemStack getCustomHead(String url);
    ItemStack getPlayerHead(Player player);
    void hidePlayer(Plugin plugin, Player player, Player hide);
    void showPlayer(Plugin plugin, Player player, Player show);
    void displayHealthBar(Player player, Scoreboard scoreboard);
    void setTeamNameTag(Player player, String id, ChatColor chatColor);
    int getItemDamage(ItemStack itemStack);
    void setItemDamage(ItemStack itemStack, int damage);
    void forceRespawn(JavaPlugin plugin, Player player);
    void openAnvil(Player player);
    ItemStack getItemInMainHand(Player player);
    void setItemInMainHand(Player player, ItemStack item);
    ItemMeta setItemUnbreakable(ItemStack itemStack);
}
