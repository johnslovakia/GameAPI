package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.modules.Module;
import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter @NoArgsConstructor
public class ResourcesModule implements Module {

    public List<Resource> resources = new ArrayList<>();

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {
        resources.forEach(resource -> resource.getResourceInterface().shutdown());
        resources = null;
    }

    public void registerResource(Resource... resources){
        this.resources.addAll(List.of(resources));
    }

    public Resource getResourceByName(String name){
        for (Resource resource : getResources()){
            if (resource.getName().equalsIgnoreCase(name)){
                return resource;
            }
        }
        return null;
    }

    public CompletableFuture<Integer> getPlayerBalance(PlayerIdentity playerIdentity, String resource){
        return getResourceByName(resource).getResourceInterface().getBalance(playerIdentity);
    }

    public CompletableFuture<Integer> getPlayerBalance(Player player, String resource){
        return getPlayerBalance(PlayerIdentityRegistry.get(player), resource);
    }

    public CompletableFuture<Integer> getPlayerBalance(PlayerIdentity playerIdentity, Resource resource){
        return resource.getResourceInterface().getBalance(playerIdentity);
    }

    public CompletableFuture<Integer> getPlayerBalance(Player player, Resource resource){
        return getPlayerBalance(PlayerIdentityRegistry.get(player), resource);
    }

    public int getPlayerBalanceCached(PlayerIdentity playerIdentity, String resource){
        return getResourceByName(resource).getResourceInterface().getBalanceCached(playerIdentity);
    }

    public int getPlayerBalanceCached(Player player, String resource){
        return getPlayerBalanceCached(PlayerIdentityRegistry.get(player), resource);
    }

    public int getPlayerBalanceCached(PlayerIdentity playerIdentity, Resource resource){
        return resource.getResourceInterface().getBalanceCached(playerIdentity);
    }

    public int getPlayerBalanceCached(Player player, Resource resource){
        return getPlayerBalanceCached(PlayerIdentityRegistry.get(player), resource);
    }

    public void deposit(OfflinePlayer offlinePlayer, Resource resource, int amount){
        resource.getResourceInterface().deposit(PlayerIdentityRegistry.get(offlinePlayer), amount);
    }

    public void deposit(PlayerIdentity playerIdentity, Resource resource, int amount){
        deposit(playerIdentity.getOfflinePlayer(), resource, amount);
    }

    public void withdraw(OfflinePlayer offlinePlayer, Resource resource, int amount){
        resource.getResourceInterface().withdraw(PlayerIdentityRegistry.get(offlinePlayer), amount);
    }

    public void withdraw(PlayerIdentity playerIdentity, Resource resource, int amount){
        withdraw(playerIdentity.getOfflinePlayer(), resource, amount);
    }
}