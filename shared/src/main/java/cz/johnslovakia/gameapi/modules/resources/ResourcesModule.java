package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.Shared;
import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter @NoArgsConstructor
public class ResourcesModule implements Module, Listener {

    public List<Resource> resources = new ArrayList<>();

    @Override
    public void initialize() {
    }

    @Override
    public void terminate() {
        resources.forEach(resource -> resource.getResourceInterface().shutdown());
        resources = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        preloadAll(player);
    }

    public CompletableFuture<Void> preloadAll(OfflinePlayer player) {
        if (resources == null || resources.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (Resource resource : resources) {
            futures.add(resource.getResourceInterface().getBalance(player));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> preloadAll(PlayerIdentity playerIdentity) {
        return preloadAll(playerIdentity.getOfflinePlayer());
    }


    public void registerResource(Resource... resources) {
        this.resources.addAll(List.of(resources));
    }

    public Resource getResourceByName(String name) {
        for (Resource resource : getResources()) {
            if (resource.getName().equalsIgnoreCase(name)) {
                return resource;
            }
        }
        return null;
    }


    public CompletableFuture<Integer> getPlayerBalance(PlayerIdentity playerIdentity, String resource) {
        return getPlayerBalance(playerIdentity.getOfflinePlayer(), resource);
    }

    public CompletableFuture<Integer> getPlayerBalance(OfflinePlayer offlinePlayer, String resource) {
        return getResourceByName(resource).getResourceInterface().getBalance(offlinePlayer);
    }

    public CompletableFuture<Integer> getPlayerBalance(PlayerIdentity playerIdentity, Resource resource) {
        return getPlayerBalance(playerIdentity.getOfflinePlayer(), resource);
    }

    public CompletableFuture<Integer> getPlayerBalance(OfflinePlayer offlinePlayer, Resource resource) {
        return resource.getResourceInterface().getBalance(offlinePlayer);
    }


    public int getPlayerBalanceCached(PlayerIdentity playerIdentity, String resource) {
        return getResourceByName(resource).getResourceInterface().getBalanceCached(playerIdentity.getOfflinePlayer());
    }

    public int getPlayerBalanceCached(Player player, String resource) {
        return getPlayerBalanceCached(PlayerIdentityRegistry.get(player), resource);
    }

    public int getPlayerBalanceCached(PlayerIdentity playerIdentity, Resource resource) {
        return resource.getResourceInterface().getBalanceCached(playerIdentity.getOfflinePlayer());
    }

    public int getPlayerBalanceCached(Player player, Resource resource) {
        return getPlayerBalanceCached(PlayerIdentityRegistry.get(player), resource);
    }


    public int getPlayerBalanceCached(OfflinePlayer offlinePlayer, Resource resource) {
        return resource.getResourceInterface().getBalanceCached(offlinePlayer);
    }

    public void deposit(OfflinePlayer offlinePlayer, Resource resource, int amount) {
        resource.getResourceInterface().deposit(offlinePlayer, amount);
    }

    public void deposit(PlayerIdentity playerIdentity, Resource resource, int amount) {
        deposit(playerIdentity.getOfflinePlayer(), resource, amount);
    }

    public void withdraw(OfflinePlayer offlinePlayer, Resource resource, int amount) {
        resource.getResourceInterface().withdraw(offlinePlayer, amount);
    }

    public void withdraw(PlayerIdentity playerIdentity, Resource resource, int amount) {
        withdraw(playerIdentity.getOfflinePlayer(), resource, amount);
    }
}