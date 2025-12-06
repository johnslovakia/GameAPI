package cz.johnslovakia.gameapi.modules.killMessage;

import cz.johnslovakia.gameapi.modules.Module;

import org.bukkit.damage.DamageType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class KillMessageModule implements Module {

    private Map<String, KillMessage> registeredMessages = new HashMap<>();

    @Override
    public void initialize() {
        registerDefaults();
    }

    @Override
    public void terminate() {
        registeredMessages = null;
    }

    private void registerDefaults() {
        register("greek_mythology_themed", msg -> {
            msg.addMessage("chat.kill_message.greek_mythology_themed.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.greek_mythology_themed.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.greek_mythology_themed.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.greek_mythology_themed.ranged", DamageType.ARROW);
        });

        register("dragon", msg -> {
            msg.addMessage("chat.kill_message.dragon.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.dragon.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.dragon.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.dragon.ranged", DamageType.ARROW);
        });

        register("toilet", msg -> {
            msg.addMessage("chat.kill_message.toilet.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.toilet.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.toilet.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.toilet.ranged", DamageType.ARROW);
        });

        register("glorious", msg -> {
            msg.addMessage("chat.kill_message.glorious.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.glorious.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.glorious.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.glorious.ranged", DamageType.ARROW);
        });

        register("wizard", msg -> {
            msg.addMessage("chat.kill_message.wizard.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.wizard.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.wizard.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.wizard.ranged", DamageType.ARROW);
        });

        register("ninja", msg -> {
            msg.addMessage("chat.kill_message.ninja.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.ninja.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.ninja.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.ninja.ranged", DamageType.ARROW);
        });

        register("gen_alpha", msg -> {
            msg.addMessage("chat.kill_message.gen_alpha.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.gen_alpha.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.gen_alpha.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.gen_alpha.ranged", DamageType.ARROW);
        });

        register("enchanted", msg -> {
            msg.addMessage("chat.kill_message.enchanted.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.enchanted.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.enchanted.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.enchanted.ranged", DamageType.ARROW);
        });

        register("dramatic", msg -> {
            msg.addMessage("chat.kill_message.dramatic.melee", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
            msg.addMessage("chat.kill_message.dramatic.fall", DamageType.FALL);
            msg.addMessage("chat.kill_message.dramatic.void", DamageType.OUT_OF_WORLD);
            msg.addMessage("chat.kill_message.dramatic.ranged", DamageType.ARROW);
        });

        register("default", msg -> {
            msg.addMessage("chat.kill", DamageType.PLAYER_ATTACK, DamageType.GENERIC_KILL);
        });
    }

    public void register(String id, Consumer<KillMessage> setup) {
        KillMessage msg = new KillMessage();
        setup.accept(msg);
        registeredMessages.put(id.toLowerCase(), msg);
    }

    public KillMessage getById(String id) {
        return registeredMessages.getOrDefault(id.toLowerCase(), registeredMessages.get("default"));
    }
}