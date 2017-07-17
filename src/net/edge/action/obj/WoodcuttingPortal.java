package net.edge.action.obj;

import net.edge.action.ActionInitializer;
import net.edge.action.impl.ObjectAction;
import net.edge.world.locale.Position;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.object.ObjectNode;

import static net.edge.content.teleport.impl.DefaultTeleportSpell.TeleportType.TRAINING_PORTAL;

public class WoodcuttingPortal extends ActionInitializer {
	@Override
	public void init() {
		ObjectAction open = new ObjectAction() {
			@Override
			public boolean click(Player player, ObjectNode object, int click) {
				player.teleport(new Position(3220, 3433, 0), TRAINING_PORTAL);
				return true;
			}
		};
		open.registerFirst(8972);
	}
}
