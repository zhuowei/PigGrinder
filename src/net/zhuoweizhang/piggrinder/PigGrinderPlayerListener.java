package net.zhuoweizhang.piggrinder;

import org.bukkit.entity.Item;
import org.bukkit.entity.Pig;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class PigGrinderPlayerListener extends PlayerListener {
	public PigGrinderPlugin plugin;

	public PigGrinderPlayerListener(PigGrinderPlugin plugin) {
		this.plugin = plugin;
	}

	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.isCancelled() || !(event.getRightClicked() instanceof Pig)) {
			return;
		}
		ItemStack tool = event.getPlayer().getItemInHand();
		if (tool == null || !tool.getType().equals(plugin.grinderMaterial) || tool.getDurability() != plugin.grinderMetadata) {
			return;
		}

/*		if (!event.getPlayer().hasPermission("piggrinder.grind")) {
			return;
		}*/

		Pig pig = (Pig) event.getRightClicked();
		for (PigGrinderPlugin.PigGrinderTask task: plugin.tasks) {
			if (task.pig.equals(pig)) {
				return;
			}
		}

		event.setCancelled(true);
		event.getPlayer().setItemInHand(new ItemStack(plugin.grinderMaterial, tool.getAmount() - 1, plugin.grinderMetadata));
		plugin.grind(pig);
	}

}
