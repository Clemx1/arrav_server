package net.edge.event.item;

import net.edge.event.EventInitializer;
import net.edge.event.impl.ItemEvent;
import net.edge.util.rand.RandomUtils;
import net.edge.world.node.entity.npc.drop.ItemCache;
import net.edge.world.node.entity.npc.drop.NpcDrop;
import net.edge.world.node.entity.npc.drop.NpcDropManager;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.assets.Rights;
import net.edge.world.node.item.Item;
import net.edge.world.node.item.container.impl.Inventory;

import static net.edge.world.node.entity.npc.drop.ItemCache.*;

public class Casket extends EventInitializer {
	@Override
	public void init() {
		ItemEvent e = new ItemEvent() {
			@Override
			public boolean click(Player player, Item item, int container, int slot, int click) {
				if(container != Inventory.INVENTORY_DISPLAY_ID)
					return true;
				Rights right = player.getRights();
				player.getInventory().remove(item, slot);
				switch (right) {
					case PLAYER:
					case IRON_MAN:
					case DESIGNER:
					case YOUTUBER:
					case HELPER:
					case MODERATOR:
					case SENIOR_MODERATOR:
					case ADMINISTRATOR:
						player.getInventory().add(new Item(995, RandomUtils.inclusive(50_000, 100_000)));
						break;
					case DONATOR:
						player.getInventory().add(new Item(995, RandomUtils.inclusive(100_000, 200_000)));
						break;
					case SUPER_DONATOR:
						player.getInventory().add(new Item(995, RandomUtils.inclusive(300_000, 400_000)));
						break;
					case EXTREME_DONATOR:
						player.getInventory().add(new Item(995, RandomUtils.inclusive(500_000, 1_000_000)));
						break;
					case GOLDEN_DONATOR:
						player.getInventory().add(new Item(995, RandomUtils.inclusive(750_000, 1_000_000)));
						break;
				}
				return true;
			}
		};
		e.register(405);
	}
}
