package cz.johnslovakia.gameapi.utils;

import cz.johnslovakia.gameapi.users.PlayerIdentity;

import java.util.List;

public final class VipBonusUtils {

    private static final List<Integer> REWARD_BONUS_PERCENTAGES
            = List.of(100, 75, 50, 45, 40, 35, 30, 25, 20, 17, 15, 12, 10, 7, 5);

    public static int getRewardBonus(PlayerIdentity playerIdentity) {
        if (playerIdentity.getOnlinePlayer() == null) return 0;

        for (Integer percent : REWARD_BONUS_PERCENTAGES) {
            if (playerIdentity.getOnlinePlayer().hasPermission("vip.bonus" + percent)) {
                return percent;
            }
        }

        return 0;
    }
}
