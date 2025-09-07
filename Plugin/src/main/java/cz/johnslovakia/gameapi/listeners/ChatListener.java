package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.Game;
import cz.johnslovakia.gameapi.game.GameState;
import cz.johnslovakia.gameapi.game.team.GameTeam;
import cz.johnslovakia.gameapi.levelSystem.LevelManager;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.messages.MessageManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.StringUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
        Game game = gamePlayer.getGame();
        GameTeam team = gamePlayer.getTeam();
        World world = player.getWorld();
        GameAPI plugin = GameAPI.getInstance();
        

        Component word_all = MessageManager.get(gamePlayer, "word.all_chat").getTranslated();
        Component word_team = MessageManager.get(gamePlayer, "word.team_chat").getTranslated();


        String prefix = "";
        String suffix = "";
        if (plugin.getVaultChat() != null) {
            String g = plugin.getVaultPerms().getPrimaryGroup(Bukkit.getPlayer(player.getName()));
            prefix = plugin.getVaultChat().getGroupPrefix(world, g);
            if (!prefix.endsWith(" ")){
                prefix = prefix + " ";
            }
            suffix = plugin.getVaultChat().getGroupSuffix(world, g);
        }

        e.setMessage(ChatColor.stripColor(e.getMessage()));

        LevelManager levelManager = Minigame.getInstance().getLevelManager();
        
        if (game != null) {
            if (gamePlayer.isSpectator() && game.getState() == GameState.INGAME) {
                e.setCancelled(true);
                if (!MessageManager.existMessage("chat.format.spectator")) {
                    String msg = "§8[§7Spectator Chat§8] " + StringUtils.colorizer(prefix) + player.getName() + "§r: " + e.getMessage();
                    game.getSpectators().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                }else {
                    MessageManager.get(game.getSpectators(), "chat.format.spectator")
                            .replace("%prefix%", StringUtils.colorizer(prefix))
                            .replace("%name%", player.getName())
                            .replace("%message%", e.getMessage())
                            .replace("%level_evolution_icon%", (levelManager != null ? Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer).levelEvolution().getIcon() : Component.text("")))
                            .send();
                }
            }else if (game.getState() == GameState.INGAME) {
                if (game.getSettings().isUseTeams() && game.getSettings().getMaxTeamPlayers() > 1) {
                    if (team != null) {
                        if (e.getMessage().startsWith("!")) {
                            e.setCancelled(true);
                            e.setMessage(e.getMessage().substring(1));
                            if (!MessageManager.existMessage("chat.format.all")) {
                                String msg = "§8[" + team.getChatColor() + word_all + "§8] " + StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                            }else {
                                MessageManager.get(game.getParticipants(), "chat.format.all")
                                        .replace("%prefix%", StringUtils.colorizer(prefix))
                                        .replace("%name%", player.getName())
                                        .replace("%team_color%", Component.empty().color(team.getTeamColor().getTextColor()))
                                        .replace("%team%", Component.text(team.getName()).color(team.getTeamColor().getTextColor()))
                                        .replace("%message%", e.getMessage())
                                        .replace("%level_evolution_icon%", (levelManager != null ? Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer).levelEvolution().getIcon() : Component.text("")))
                                        .send();
                            }
                        } else {
                            e.setCancelled(true);
                            if (!MessageManager.existMessage("chat.format.team")) {
                                String msg = "§8[" + team.getChatColor() + word_team + "§8] " +StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                team.getAllMembers().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                            }else {
                                MessageManager.get(team.getAllMembers(), "chat.format.team")
                                        .replace("%prefix%", StringUtils.colorizer(prefix))
                                        .replace("%name%", player.getName())
                                        .replace("%team_color%", Component.text("").color(team.getTeamColor().getTextColor()))
                                        .replace("%team%", Component.text(team.getName()).color(team.getTeamColor().getTextColor()))
                                        .replace("%message%", e.getMessage())
                                        .replace("%level_evolution_icon%", (levelManager != null ? Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer).levelEvolution().getIcon() : Component.text("")))
                                        .send();
                            }
                        }
                    } else {
                        e.setCancelled(true);
                        String msg = StringUtils.colorizer(prefix) + "§7" + player.getName() + "§r: " + e.getMessage();
                        game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                    }

                } else {
                    e.setCancelled(true);
                    if (!MessageManager.existMessage("chat.format.all")){
                        String msg = "§8[§a" + word_all + "§8] " + StringUtils.colorizer(prefix) + "§r" + player.getName() +  "§r: " + e.getMessage();
                        game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                    }else {
                        MessageManager.get(game.getParticipants(), "chat.format.all")
                                .replace("%prefix%", StringUtils.colorizer(prefix))
                                .replace("%name%", player.getName())
                                .replace("%message%", e.getMessage())
                                .replace("%team_color%", "§a")
                                .replace("%team%", "")
                                .replace("%level_evolution_icon%", (levelManager != null ? Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer).levelEvolution().getIcon() : Component.text("")))
                                .send();
                    }
                }
            }else{
                e.setCancelled(true);
                if (!MessageManager.existMessage("chat.format.default")) {
                    String msg = StringUtils.colorizer(prefix) + "§r" + player.getName() + "§r: " + e.getMessage();
                    game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                }else {
                    MessageManager.get(game.getParticipants(), "chat.format.default")
                            .replace("%prefix%", StringUtils.colorizer(prefix))
                            .replace("%name%", player.getName())
                            .replace("%message%", e.getMessage())
                            .replace("%level_evolution_icon%", (levelManager != null ? Minigame.getInstance().getLevelManager().getLevelProgress(gamePlayer).levelEvolution().getIcon() : Component.text("")))
                            .send();
                }
            }
        }else{
            e.setCancelled(true);
        }
    }
}