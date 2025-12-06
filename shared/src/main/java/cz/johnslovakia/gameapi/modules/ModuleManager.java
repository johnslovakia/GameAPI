package cz.johnslovakia.gameapi.modules;

import cz.johnslovakia.gameapi.utils.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {

    @Getter
    private static ModuleManager instance;

    private final JavaPlugin plugin;
    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

    public ModuleManager(JavaPlugin plugin) {
        instance = this;

        this.plugin = plugin;
        Bukkit.getServicesManager().register(ModuleManager.class, this, plugin, ServicePriority.Normal);
    }

    public <T extends Module> boolean hasModule(Class<T> clazz) {
        return modules.containsKey(clazz);
    }

    public <T extends Module> T registerModule(T module) {
        Class<? extends Module> moduleClass = module.getClass();
        if (modules.containsKey(moduleClass))
            throw new IllegalArgumentException("Module " + moduleClass.getSimpleName() + " is already registered!");

        modules.put(moduleClass, module);
        module.initialize();

        if (module instanceof Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        return module;
    }

    public void registerModule(Module... modules) {
        Arrays.stream(modules).forEach(this::registerModule);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> moduleClass) {
        return (T) getInstance().modules.get(moduleClass);
    }

    public <T extends Module> void destroyModule(Class<T> moduleClass) {
        Module module = modules.remove(moduleClass);
        if (module != null) {
            if (module instanceof Listener listener)
                HandlerList.unregisterAll(listener);

            module.terminate();
        }
    }

    public void destroyAll() {
        for (Module module : modules.values()) {
            if (module instanceof Listener listener)
                HandlerList.unregisterAll(listener);

            module.terminate();
        }
        modules.clear();
        Bukkit.getServicesManager().unregister(this);
    }
}
