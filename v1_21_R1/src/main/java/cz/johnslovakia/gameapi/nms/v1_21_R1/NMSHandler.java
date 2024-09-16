package cz.johnslovakia.gameapi.nms.v1_21_R1;

import cz.johnslovakia.gameapi.api.VersionSupport;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scoreboard.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class NMSHandler implements VersionSupport {

    @Override
    public double getMaxPlayerHealth(Player player) {
        return player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    @Override
    public void setMaxPlayerHealth(Player player, double max) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);
    }

    @Override
    public ItemStack getCustomHead(String url) {
        url = url.toLowerCase();
        url = url.replace("http://textures.minecraft.net/texture/", "");
        url = url.replace("https://textures.minecraft.net/texture/", "");


        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) meta;

        PlayerProfile pp = Bukkit.createPlayerProfile(UUID.fromString("4fbecd49-c7d4-4c18-8410-adf7a7348728"));
        PlayerTextures pt = pp.getTextures();
        try {
            pt.setSkin(new URL("http://textures.minecraft.net/texture/" + url));
        } catch (MalformedURLException e) {e.printStackTrace();}
        pp.setTextures(pt);
        skullMeta.setOwnerProfile(pp);
        meta = skullMeta;
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public ItemStack getPlayerHead(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void hidePlayer(Plugin plugin, Player player, Player hide) {
        player.hidePlayer(plugin, hide);
    }

    @Override
    public void showPlayer(Plugin plugin, Player player, Player show) {
        player.showPlayer(plugin, show);
    }

    @Override
    public void displayHealthBar(Player player, Scoreboard scoreboard) {
        Objective h = scoreboard.getObjective("showhealth");
        if (h == null) {
            h = scoreboard.registerNewObjective("showhealth", Criteria.HEALTH, ChatColor.RED + "❤");
        }
        h.setDisplaySlot(DisplaySlot.BELOW_NAME);
        h.setDisplayName(ChatColor.DARK_RED + "❤");

        //h.displayName(Component.score(ChatColor.DARK_RED + "❤", "below_name"));

        player.setScoreboard(scoreboard);
    }


    @Override
    public void setTeamNameTag(Player player, String id, ChatColor chatColor) {
        for (Player each : Bukkit.getOnlinePlayers()) {
            Scoreboard board = each.getScoreboard();

            Team boardTeam = board.getTeam(id);
            if (boardTeam == null) {
                boardTeam = board.registerNewTeam(id);
            }
            boardTeam.setColor(chatColor);
            boardTeam.setPrefix(chatColor + "");


            boardTeam.addEntry(player.getName());
        }
    }

    @Override
    public int getItemDamage(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            Damageable damageable = (Damageable) itemStack.getItemMeta();
            return damageable.getDamage();
        }
        return itemStack.getType().getMaxDurability();
    }

    @Override
    public void setItemDamage(ItemStack is, int damage) {
        Damageable d = (Damageable) is.getItemMeta();
        d.setDamage((is.getType().getMaxDurability() - d.getDamage()) - damage);
        is.setItemMeta(d);
    }

    @Override
    public void forceRespawn(JavaPlugin plugin, Player player) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                player.spigot().respawn();
            }
        }, 1L);
    }

    @Override
    public void openAnvil(Player player){
    }

    @Override
    public ItemStack getItemInMainHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    @Override
    public void setItemInMainHand(Player player, ItemStack item) {
        player.getInventory().setItemInMainHand(item);
    }

    @Override
    public ItemMeta setItemUnbreakable(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setUnbreakable(true);
        itemStack.setItemMeta(meta);
        return meta;
    }

}
