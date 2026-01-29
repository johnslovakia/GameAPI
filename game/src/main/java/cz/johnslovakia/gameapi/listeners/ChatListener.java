package cz.johnslovakia.gameapi.listeners;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.modules.game.GameInstance;
import cz.johnslovakia.gameapi.modules.game.GameState;
import cz.johnslovakia.gameapi.modules.game.team.GameTeam;
import cz.johnslovakia.gameapi.modules.levels.LevelModule;
import cz.johnslovakia.gameapi.modules.messages.MessageModule;
import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.StringUtils;

import net.kyori.adventure.text.Component;
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
        GameInstance game = gamePlayer.getGame();
        GameTeam team = gamePlayer.getGameSession().getTeam();
        

        Component word_all = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "word.all_chat").getTranslated();
        Component word_team = ModuleManager.getModule(MessageModule.class).get(gamePlayer, "word.team_chat").getTranslated();

        String prefix = gamePlayer.getPrefix();

        e.setMessage(ChatColor.stripColor(e.getMessage()));

        LevelModule levelModule = ModuleManager.getModule(LevelModule.class);
        
        if (game != null) {
            if (gamePlayer.isSpectator() && game.getState() == GameState.INGAME) {
                e.setCancelled(true);
                if (!ModuleManager.getModule(MessageModule.class).existMessage("chat.format.spectator")) {
                    String msg = "§8[§7Spectator Chat§8] " + StringUtils.colorizer(prefix) + player.getName() + "§r: " + e.getMessage();
                    game.getSpectators().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                }else {
                    ModuleManager.getModule(MessageModule.class).get(game.getSpectators(), "chat.format.spectator")
                            .replace("%prefix%", StringUtils.colorizer(prefix))
                            .replace("%name%", player.getName())
                            .replace("%message%", e.getMessage())
                            .replace("%level_evolution_icon%", (levelModule != null ? levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon() : Component.text("")))
                            .send();
                }
            }else if (game.getState() == GameState.INGAME) {
                if (game.getSettings().isUseTeams() && game.getSettings().getMaxTeamPlayers() > 1) {
                    if (team != null) {
                        if (e.getMessage().startsWith("!")) {
                            e.setCancelled(true);
                            e.setMessage(e.getMessage().substring(1));
                            if (!ModuleManager.getModule(MessageModule.class).existMessage("chat.format.all")) {
                                String msg = "§8[" + team.getChatColor() + word_all + "§8] " + StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                            }else {
                                ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), "chat.format.all")
                                        .replace("%prefix%", StringUtils.colorizer(prefix))
                                        .replace("%name%", player.getName())
                                        .replace("%team_color%", "" + team.getTeamColor().getChatColor())
                                        .replace("%team%", Component.text(team.getName()).color(team.getTeamColor().getTextColor()))
                                        .replace("%message%", e.getMessage())
                                        .replace("%level_evolution_icon%", (levelModule != null ? levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon() : Component.text("")))
                                        .send();
                            }
                        } else {
                            e.setCancelled(true);
                            if (!ModuleManager.getModule(MessageModule.class).existMessage("chat.format.team")) {
                                String msg = "§8[" + team.getChatColor() + word_team + "§8] " +StringUtils.colorizer(prefix) + "§r" + team.getChatColor() + player.getName() + "§r: " + e.getMessage();
                                team.getAllMembers().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                            }else {
                                ModuleManager.getModule(MessageModule.class).get(team.getAllMembers(), "chat.format.team")
                                        .replace("%prefix%", StringUtils.colorizer(prefix))
                                        .replace("%name%", player.getName())
                                        .replace("%team_color%", "" + team.getTeamColor().getChatColor())
                                        .replace("%team%", Component.text(team.getName()).color(team.getTeamColor().getTextColor()))
                                        .replace("%message%", e.getMessage())
                                        .replace("%level_evolution_icon%", (levelModule != null ? levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon() : Component.text("")))
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
                    if (!ModuleManager.getModule(MessageModule.class).existMessage("chat.format.all")){
                        String msg = "§8[§a" + word_all + "§8] " + StringUtils.colorizer(prefix) + "§r" + player.getName() +  "§r: " + e.getMessage();
                        game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                    }else {
                        ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), "chat.format.all")
                                .replace("%prefix%", StringUtils.colorizer(prefix))
                                .replace("%name%", player.getName())
                                .replace("%message%", e.getMessage())
                                //.replace("%team_color%", "" + team.getTeamColor().getChatColor())
                                .replace("%team%", "")
                                .replace("%level_evolution_icon%", (levelModule != null ? levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon() : Component.text("")))
                                .send();
                    }
                }
            }else{
                e.setCancelled(true);
                if (!ModuleManager.getModule(MessageModule.class).existMessage("chat.format.default")) {
                    String msg = StringUtils.colorizer(prefix) + "§r" + player.getName() + "§r: " + e.getMessage();
                    game.getParticipants().forEach(gp -> gp.getOnlinePlayer().sendMessage(msg));
                }else {
                    ModuleManager.getModule(MessageModule.class).get(game.getParticipants(), "chat.format.default")
                            .replace("%prefix%", StringUtils.colorizer(prefix))
                            .replace("%name%", player.getName())
                            .replace("%message%", e.getMessage())
                            .replace("%level_evolution_icon%", (levelModule != null ? levelModule.getPlayerData(gamePlayer).getLevelEvolution().getIcon() : Component.text("")))
                            .send();
                }
            }
        }else{
            e.setCancelled(true);
        }
    }
}