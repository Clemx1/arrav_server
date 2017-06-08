package net.edge.event.but;

import net.edge.content.market.MarketCounter;
import net.edge.content.market.MarketShop;
import net.edge.event.EventInitializer;
import net.edge.event.impl.ButtonEvent;
import net.edge.world.node.entity.player.Player;

public class ShopEditing extends EventInitializer {
	
	@Override
	public void init() {
		ButtonEvent e = new ButtonEvent() {
			@Override
			public boolean click(Player player, int button) {
				player.getMessages().sendEnterAmount("Item id:", t -> () -> {
					int id = Integer.parseInt(t);
					if(player.getMarketShop() != null && player.getMarketShop().getId() != -1) {
						int shopId = player.getMarketShop().getId();
						MarketShop shop = MarketCounter.getShops().get(shopId);
						int[] items = new int[shop.getItems().length + 1];
						items[items.length - 1] = id;
						MarketCounter.getShops().put(id, new MarketShop(shopId, shop.getTitle(), shop.getCurrency(), items));
					}
				});
				return true;
			}
		};
		e.register(124);

	}
	
}