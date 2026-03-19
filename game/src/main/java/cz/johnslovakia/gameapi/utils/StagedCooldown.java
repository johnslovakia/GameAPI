package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.users.PlayerIdentity;
import cz.johnslovakia.gameapi.users.PlayerIdentityRegistry;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class StagedCooldown {

    private final int usesPerCycle;
    private final Cooldown shortCooldown;
    private final Cooldown longCooldown;

    private final Map<UUID, Integer> uses = new HashMap<>();

    public StagedCooldown(int usesPerCycle, double shortCooldownSeconds, double longCooldownSeconds) {
        this.usesPerCycle = usesPerCycle;
        this.shortCooldown = new Cooldown(StringUtils.randomString(8, true, true, false), shortCooldownSeconds);
        this.longCooldown  = new Cooldown(StringUtils.randomString(8, true, true, false), longCooldownSeconds);
    }

    public Cooldown use(PlayerIdentity playerIdentity, ItemStack itemStack) {
        UUID uuid = playerIdentity.getUniqueId();
        int currentUses = uses.getOrDefault(uuid, 0) + 1;

        if (currentUses >= usesPerCycle) {
            uses.put(uuid, 0);
            longCooldown.startItemStackCooldown(playerIdentity, itemStack);
            return longCooldown;
        } else {
            uses.put(uuid, currentUses);
            shortCooldown.startItemStackCooldown(playerIdentity, itemStack);
            return shortCooldown;
        }
    }

    public boolean isOnCooldown(PlayerIdentity playerIdentity) {
        return shortCooldown.contains(playerIdentity) || longCooldown.contains(playerIdentity);
    }

    public boolean isOnCooldown(Player player) {
        return isOnCooldown(PlayerIdentityRegistry.get(player));
    }

    public double getCountdown(Player player) {
        return Math.max(shortCooldown.getCountdown(player), longCooldown.getCountdown(player));
    }

    public double getCountdown(PlayerIdentity playerIdentity) {
        return getCountdown(playerIdentity.getOnlinePlayer());
    }

    public void clearPlayer(UUID uuid) {
        uses.remove(uuid);
        shortCooldown.cancelCooldown(uuid);
        longCooldown.cancelCooldown(uuid);
    }

    public void clearPlayer(Player player) {
        clearPlayer(player.getUniqueId());
    }
}