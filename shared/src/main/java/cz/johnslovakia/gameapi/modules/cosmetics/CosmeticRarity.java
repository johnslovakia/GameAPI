package cz.johnslovakia.gameapi.modules.cosmetics;

import lombok.Getter;
import org.bukkit.ChatColor;

@Getter
public enum CosmeticRarity{

    COMMON("rarity.common", ChatColor.GRAY, 5),
    UNCOMMON("rarity.uncommon", ChatColor.GREEN, 4),
    RARE("rarity.rare", ChatColor.BLUE, 3),
    EPIC("rarity.epic", ChatColor.DARK_PURPLE, 2),
    LEGENDARY("rarity.legendary", ChatColor.GOLD, 1);

    final ChatColor color;
    final String translateKey;
    final Integer rank;

    CosmeticRarity(String translateKey, ChatColor color, Integer rank){
        this.color = color;
        this.translateKey = translateKey;
        this.rank = rank;
    }
}