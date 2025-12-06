package cz.johnslovakia.gameapi.utils.chatHead;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;


/**
 * A class for retrieving player head representations as BaseComponents.
 * The head representation is generated based on the players UUID and skin source.
 *
 * @author Minso
 * https://github.com/OGminso/ChatHeadFont/tree/main
 */
public class ChatHeadAPI {

    /**
     * The default SkinSource used in the code of this plugin.
     */
    public static SkinSource defaultSource = new MojangSource();

    private static ChatHeadAPI instance;

    private final Plugin plugin;

    /**
     * Constructs a new ChatHeadAPI instance.
     *
     * @param plugin The JavaPlugin instance associated with the ChatHeadAPI.
     */
    public ChatHeadAPI(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieves the singleton instance of the ChatHeadAPI.
     *
     * @return The singleton instance of ChatHeadAPI.
     * @throws IllegalArgumentException If ChatHeadAPI has not been initialized.
     */
    public static ChatHeadAPI getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("ChatHeadAPI has not been initialized.");
        }
        return instance;
    }

    /**
     * Initializes the ChatHeadAPI with the provided JavaPlugin instance.
     *
     * @param plugin The JavaPlugin instance to associate with the ChatHeadAPI.
     * @throws IllegalStateException If ChatHeadAPI has already been initialized.
     */
    public static void initialize(Plugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("PlayerHeadAPI has already been initialized.");
        }
        instance = new ChatHeadAPI(plugin);
    }


    /**
     * Creates a 8x8 grid of pixels representing a Minecraft player's head.
     * Each pixel in the grid is represented by a TextComponent with a specified hexadecimal color.
     *
     * @param uuid       The UUID of the player whose head is to be retrieved & created.
     * @param overlay    A boolean value indicating whether to apply overlay on the players head.
     * @param skinSource An enum specifying the source from which to retrieve the player's skin.
     *                   Supported sources include MOJANG, MINOTAR, and CRAFATAR.
     * @return An array of BaseComponents representing the player's head.
     * Each BaseComponent represents a single pixel, forming a 8x8 grid of pixels.
     */
    public Component getHead(UUID uuid, boolean overlay, SkinSource skinSource) {
        return skinSource.getHead(Bukkit.getOfflinePlayer(uuid), overlay);

    }

    public Component getHead(OfflinePlayer player, boolean overlay, SkinSource skinSource) {
        return skinSource.getHead(player, overlay);
    }

    public Component getHead(OfflinePlayer player, boolean overlay) {
        return defaultSource.getHead(player, overlay);
    }

    public Component getHead(OfflinePlayer player) {
        return defaultSource.getHead(player, true);
    }

    /**
     * Exports the BaseComponent[] from the getHead method to a String.
     *
     * @param uuid       The UUID of the player whose head is to be retrieved & created.
     * @param overlay    A boolean value indicating whether to apply overlay on the players head.
     * @param skinSource An enum specifying the source from which to retrieve the player's skin.
     *                   Supported sources include MOJANG, MINOTAR, and CRAFATAR.
     */
    public String getHeadAsString(UUID uuid, boolean overlay, SkinSource skinSource) {
        return getHeadAsString(Bukkit.getOfflinePlayer(uuid), true, defaultSource);
    }

    /**
     * Exports the BaseComponent[] from the getHead method to a String.
     *
     * @param player     The Player object representing the player whose head is to be retrieved.
     * @param overlay    A boolean value indicating whether to apply overlay on the players head.
     * @param skinSource An enum specifying the source from which to retrieve the player's skin.
     *                   Supported sources include MOJANG, MINOTAR, and CRAFATAR.
     */
    public String getHeadAsString(OfflinePlayer player, boolean overlay, SkinSource skinSource) {
        return LegacyComponentSerializer.legacySection().serialize(
                this.getHead(player, overlay, skinSource)
        );
    }

    public String getHeadAsString(OfflinePlayer player) {
        return getHeadAsString(player, true, defaultSource);
    }

}