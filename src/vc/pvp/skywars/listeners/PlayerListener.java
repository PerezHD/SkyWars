package vc.pvp.skywars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.config.PluginConfig;
import vc.pvp.skywars.controllers.GameController;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.controllers.SchematicController;
import vc.pvp.skywars.game.Game;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.Messaging;
import vc.pvp.skywars.utilities.StringUtils;

import org.bukkit.event.EventPriority;

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        PlayerController.get().register(player);
        player.teleport(PluginConfig.getLobbySpawn());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            gamePlayer.getGame().onPlayerLeave(gamePlayer);
        }

        gamePlayer.save();
        PlayerController.get().unregister(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        final GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            event.setRespawnLocation(PluginConfig.getLobbySpawn());

            if (PluginConfig.saveInventory()) {
                Bukkit.getScheduler().runTaskLater(SkyWars.get(), new Runnable() {
                    @Override
                    public void run() {
                        gamePlayer.restoreState();
                    }
                }, 2L);
            }
        }
    }

    SchematicController controller = SchematicController.get();
    PlayerController pController = PlayerController.get();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == null || event.getAction() != Action.PHYSICAL || event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.STONE_PLATE) {
            return;
        }
        Player player = event.getPlayer();
        GamePlayer gamePlayer = pController.get(player);

        if (!gamePlayer.isPlaying() && player.getWorld().getName().equals(PluginConfig.getLobbySpawnWorldName())) {
            if (controller.size() == 0) {
                player.sendMessage(new Messaging.MessageFormatter().format("error.no-schematics"));
                return;
            }

            Game game = GameController.get().findEmpty();
            game.onPlayerJoin(gamePlayer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);
        String message = new Messaging.MessageFormatter().setVariable("score", StringUtils.formatScore(gamePlayer.getScore())).setVariable("player", player.getDisplayName()).setVariable("message", Messaging.stripColor(event.getMessage())).setVariable("prefix", SkyWars.getChat().getPlayerPrefix(player)).format("&8[&c{score}&8] {prefix}&b{player} &d&l>> &r&f{message}");

        event.setCancelled(true);
        Bukkit.broadcastMessage(message);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            String command = event.getMessage().split(" ")[0].toLowerCase();

            if (!command.equals("/sw") && !PluginConfig.isCommandWhitelisted(command)) {
                event.setCancelled(true);
                player.sendMessage(new Messaging.MessageFormatter().withPrefix().format("error.cmd-disabled"));
            }
        }
    }
    
}
