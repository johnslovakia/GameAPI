package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        GamePlayer gamePlayer = PlayerManager.getGamePlayer(player);
        Game game = gamePlayer.getPlayerData().getGame();
        GameTeam team = gamePlayer.getPlayerData().getTeam();
        World world = player.getWorld();
        GameAPI plugin = GameAPI.getInstance();
        

        String word_all = MessageManager.get(gamePlayer, "word.all_chat").getTranslated();
        String word_team = MessageManager.get(gamePlayer, "word.team_chat").getTranslated();


        String prefix = "";
        String suffix = "";
        if (plugin.getVaultChat() != null) {
            String g = plugin.getVaultPerms().getPrimaryGroup(Bukkit.getPlayer(player.getName()));
            prefix = plugin.getVaultChat().getGroupPrefix(world, g);
            if (prefix.endsWith(" ")){
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            suffix = plugin.getVaultChat().getGroupSuffix(world, g);
        }

        if (game != null) {
            if (gamePlayer.isSpectator() && game.getState() == GameState.INGAME) {
                e.setCancelled(true);
                if (!MessageManager.existMessage("chat.format.spectator")) {
                    String msg = "§8[§7Spectator Chat§8] " + StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null ? "" : " ") + player.getDisplayName() + "§r: " + e.getMessage();
                    e.setMessage(msg);
                    for (GamePlayer t : game.getSpectators()) {
                        t.getOnlinePlayer().sendMessage(e.getMessage());
                    }
                }else {
                    for (GamePlayer t : game.getSpectators()) {
                        MessageManager.get(t, "chat.format.spectator")
                                .replace("%prefix%", StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null || prefix.equals("") ? "" : " "))
                                .replace("%name%", player.getDisplayName())
                                .replace("%message%", e.getMessage())
                                .send();
                    }
                }

                return;
            }
            if (game.getState() == GameState.INGAME || game.getState() == GameState.PREPARATION) {

                if (game.getSettings().isUseTeams() && game.getSettings().getMaxTeamPlayers() > 1) {
                    if (team != null) {
                        if (e.getMessage().startsWith("!")) {
                            e.setCancelled(true);
                            e.setMessage(e.getMessage().substring(1));
                            if (!MessageManager.existMessage("chat.format.all")) {
                                String all = "§8[" + team.getChatColor() + word_all + "§8] " + StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                e.setMessage(all);
                                for (GamePlayer allGPs : game.getParticipants()) {
                                    allGPs.getOnlinePlayer().sendMessage(e.getMessage());
                                }
                            }else {
                                for (GamePlayer allGPs : game.getParticipants()) {
                                    MessageManager.get(allGPs, "chat.format.all")
                                            .replace("%prefix%", StringUtils.colorizer(prefix))
                                            .replace("%name%", player.getDisplayName())
                                            .replace("%team_color%", "" + team.getChatColor())
                                            .replace("%team%", team.getName())
                                            .replace("%message%", e.getMessage())
                                            .send();
                                }
                            }
                        } else {
                            e.setCancelled(true);
                            if (!MessageManager.existMessage("chat.format.team")) {
                                String teamMSG = "§8[" + team.getChatColor() + word_team + "§8] " +StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                e.setMessage(teamMSG);
                                for (GamePlayer t : team.getAllMembers()) {
                                    t.getOnlinePlayer().sendMessage(teamMSG);
                                }
                            }else {
                                for (GamePlayer t : team.getAllMembers()) {
                                    MessageManager.get(t, "chat.format.team")
                                            .replace("%prefix%", StringUtils.colorizer(prefix))
                                            .replace("%name%", player.getDisplayName())
                                            .replace("%team_color%", "" + team.getChatColor())
                                            .replace("%team%", team.getName())
                                            .replace("%message%", e.getMessage())
                                            .send();
                                }
                            }
                        }
                    } else {
                        e.setCancelled(true);
                        String msg = StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null || prefix.equals("") ? "" : " ") + "§7" + player.getName() + "§r: " + e.getMessage();
                        e.setMessage(msg);
                        for (GamePlayer allGPs : game.getParticipants()){
                            allGPs.getOnlinePlayer().sendMessage(e.getMessage());
                        }
                    }

                } else {
                    e.setCancelled(true);
                    if (!MessageManager.existMessage("chat.format.all")){
                        String solo = "§8[§a" + word_all + "§8] " + StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null || prefix.equals("") ? "" : " ") + "§r" + player.getName() +  "§r: " + e.getMessage();
                        e.setMessage(solo);
                        for (GamePlayer allGPs : game.getParticipants()) {
                            allGPs.getOnlinePlayer().sendMessage(e.getMessage());
                        }
                    }else {
                        for (GamePlayer allGPs : game.getParticipants()) {
                            MessageManager.get(allGPs, "chat.format.all")
                                    .replace("%prefix%", StringUtils.colorizer(prefix))
                                    .replace("%name%", player.getDisplayName())
                                    .replace("%message%", e.getMessage())
                                    .replace("%team_color%", "§a")
                                    .replace("%team%", "")
                                    .send();
                        }
                    }
                }
            }else{
                e.setCancelled(true);
                if (!MessageManager.existMessage("chat.format.default")) {
                    String lobby = StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null || prefix.equals("") ? "" : " ") + "§r" + player.getName() + "§r: " + e.getMessage();
                    e.setMessage(lobby);
                    for (GamePlayer allGPs : game.getParticipants()) {
                        allGPs.getOnlinePlayer().sendMessage(e.getMessage());
                    }
                }else {
                    for (GamePlayer allGPs : game.getParticipants()) {
                        MessageManager.get(allGPs, "chat.format.default")
                                .replace("%prefix%", StringUtils.colorizer(prefix) + (prefix.endsWith(" ") || GameAPI.getInstance().getVaultChat() == null || prefix.equals("") ? "" : " "))
                                .replace("%name%", player.getDisplayName())
                                .replace("%message%", e.getMessage())
                                .send();
                    }
                }
            }
        }else{
            e.setCancelled(true);
        }
    }
}