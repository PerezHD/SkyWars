package vc.pvp.skywars.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;

public class BlockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        GamePlayer gamePlayer = PlayerController.get().get(event.getPlayer());
        if (gamePlayer.isPlaying() && gamePlayer.getGame().getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        GamePlayer gamePlayer = PlayerController.get().get(event.getPlayer());
        if (gamePlayer.isPlaying() && gamePlayer.getGame().getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }
}
