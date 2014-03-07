package vc.pvp.skywars;

import com.earth2me.essentials.IEssentials;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import vc.pvp.skywars.commands.MainCommand;
import vc.pvp.skywars.controllers.*;
import vc.pvp.skywars.database.Database;
import vc.pvp.skywars.listeners.BlockListener;
import vc.pvp.skywars.listeners.EntityListener;
import vc.pvp.skywars.listeners.InventoryListener;
import vc.pvp.skywars.listeners.PlayerListener;
import vc.pvp.skywars.storage.DataStorage;
import vc.pvp.skywars.storage.SQLStorage;
import vc.pvp.skywars.tasks.SyncTask;
import vc.pvp.skywars.utilities.CraftBukkitUtil;
import vc.pvp.skywars.utilities.FileUtils;
import vc.pvp.skywars.utilities.Messaging;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;

public class SkyWars extends JavaPlugin {

    private static SkyWars instance;
    private static Permission permission;
    private static Economy economy;
    private static Chat chat;
    private Database database;

    @Override
    public void onEnable() {
        instance = this;

        deleteIslandWorlds();

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        new Messaging(this);

        getCommand("skywars").setExecutor(new MainCommand());

        try {
            DataStorage.DataStorageType dataStorageType = DataStorage.DataStorageType.valueOf(getConfig().getString("data-storage", "FILE"));
            if (dataStorageType == DataStorage.DataStorageType.SQL && !setupDatabase()) {
                getLogger().log(Level.INFO, "Couldn't setup database, now using file storage.");
                DataStorage.setInstance(DataStorage.DataStorageType.FILE);

            } else {
                DataStorage.setInstance(dataStorageType);
            }

        } catch (NullPointerException npe) {
            DataStorage.setInstance(DataStorage.DataStorageType.FILE);
        }

        setupPermission();
        setupEconomy();
        setupChat();

        SchematicController.get();
        WorldController.get();
        GameController.get();
        PlayerController.get();
        ChestController.get();
        KitController.get();
        IconMenuController.get();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(), this);
        pm.registerEvents(new EntityListener(), this);
        pm.registerEvents(new BlockListener(), this);
        pm.registerEvents(new InventoryListener(), this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new SyncTask(), 20L, 20L);

    }
    
    
    
    @SuppressWarnings({"StatementWithEmptyBody", "empty-statement"})
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        GameController.get().shutdown();
        PlayerController.get().shutdown();

        if (DataStorage.get() instanceof SQLStorage && !CraftBukkitUtil.isRunning()) {
            SQLStorage sqlStorage = (SQLStorage) DataStorage.get();
            while (!sqlStorage.saveProcessor.isEmpty());
            long currentTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - currentTime < 1000L);
            sqlStorage.saveProcessor.stop();
        }

        if (database != null) {
            database.close();
        }

        deleteIslandWorlds();
    }

    private void deleteIslandWorlds() {

        File workingDirectory = new File(".");
        File[] contents = workingDirectory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                if (!file.isDirectory() || !file.getName().matches("island-\\d+")) {
                    continue;
                }
                World world = this.getServer().getWorld(file.getName());
                Boolean result = false;
                if (!result) {
                    if (world != null) {
                        result = this.getServer().unloadWorld(world, true);
                        if (result == true) {
                            this.getLogger().log(Level.INFO, "World ''{0}'' was unloaded from memory.", file.getName());
                        } else {
                            this.getLogger().log(Level.SEVERE, "World ''{0}'' could not be unloaded.", file.getName());
                        }
                    }
                    result = FileUtils.deleteFolder(file);
                    if (result == true) {
                        this.getLogger().log(Level.INFO, "World ''{0}'' was deleted.", file.getName());
                    } else {
                        this.getLogger().log(Level.SEVERE, "World ''{0}'' was NOT deleted.", file.getName());
                        this.getLogger().log(Level.SEVERE, "Are you sure the folder {0} exists?", file.getName());
                        this.getLogger().log(Level.SEVERE, "Please check your file permissions on ''{0}''", file.getName());
                    }
                }
            }
        }
    }

    private boolean setupDatabase() {
        try {
            database = new Database(getConfig().getConfigurationSection("database"));

        } catch (ClassNotFoundException exception) {
            getLogger().log(Level.SEVERE, String.format("Unable to register JDCB driver: %s", exception.getMessage()));
            return false;

        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, String.format("Unable to connect to SQL server: %s", exception.getMessage()));
            return false;
        }

        try {
            database.createTables();
        } catch (IOException | SQLException exception) {
            getLogger().log(Level.SEVERE, String.format("An exception was thrown while attempting to create tables: %s", exception.getMessage()));
            return false;
        }

        return true;
    }

    private void setupPermission() {
        RegisteredServiceProvider<Permission> chatProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (chatProvider != null) {
            permission = chatProvider.getProvider();
        }
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> chatProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (chatProvider != null) {
            economy = chatProvider.getProvider();
        }
    }

    private void setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }
    }

    public static SkyWars get() {
        return instance;
    }

    public static IEssentials getEssentials() {
        return (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
    }

    public static Permission getPermission() {
        return permission;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static Chat getChat() {
        return chat;
    }

    public static Database getDB() {
        return instance.database;
    }
}
