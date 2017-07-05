package net.edge.content.commands.impl;

import net.edge.task.Task;
import net.edge.util.json.impl.*;
import net.edge.content.commands.Command;
import net.edge.content.commands.CommandSignature;
import net.edge.content.market.MarketCounter;
import net.edge.world.World;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.assets.Rights;
import net.edge.world.object.ObjectNode;

@CommandSignature(alias = {"reload"}, rights = {Rights.ADMINISTRATOR}, syntax = "Use this command as just ::reload")
public final class ReloadCommand implements Command {
	
	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		World.get().getTask().submit(new Task(2, false) {
			
			boolean removed;
			
			@Override
			protected void execute() {
				if(!removed) {
					//Removing npcs.
					World.get().getNpcs().clear();
					World.get().getPlayers().forEach(p -> p.getLocalNpcs().clear());
					//Removing objects.
					World.getRegions().getRegions().forEach((c, r) -> r.getDynamicObjects().forEach(ObjectNode::remove));
					
					//clearing areas
					World.getAreaManager().getAreas().clear();
					
					//Deleting shops
					MarketCounter.getShops().clear();
					player.message("Successfully removed the world nodes [Objects, Npcs, Shops, Areas].");
					removed = true;
				} else {
					new ItemDefinitionLoader().load();
					new NpcDefinitionLoader().load();
					new NpcNodeLoader().load();
					new ShopLoader().load();
					new AreaLoader().load();
					this.cancel();
					player.message("Successfully reloaded the world [Objects, Npcs, Shops, Defs, Areas].");
				}
			}
		});
	}
}
