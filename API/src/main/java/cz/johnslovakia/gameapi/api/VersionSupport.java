package cz.johnslovakia.gameapi.api;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

public interface VersionSupport {

    public double getMaxPlayerHealth(Player player);
    public void setMaxPlayerHealth(Player player, double max);
    public ItemStack getCustomHead(String url);
    public ItemStack getPlayerHead(Player player);
    public void hidePlayer(Plugin plugin, Player player, Player hide);
    public void showPlayer(Plugin plugin, Player player, Player show);
    public void displayHealthBar(Player player, Scoreboard scoreboard);
    public void setTeamNameTag(Player player, String id, ChatColor chatColor);
    public int getItemDamage(ItemStack itemStack);
    public void setItemDamage(ItemStack itemStack, int damage);
    public void forceRespawn(JavaPlugin plugin, Player player);
    public void openAnvil(Player player);
    public ItemStack getItemInMainHand(Player player);
    public void setItemInMainHand(Player player, ItemStack item);
    public ItemMeta setItemUnbreakable(ItemStack itemStack);
}
