package net.edge.content.commands.impl;

import net.edge.content.commands.Command;
import net.edge.content.commands.CommandSignature;
import net.edge.world.locale.Position;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.entity.actor.player.assets.Rights;

@CommandSignature(alias = {"move"}, rights = {Rights.ADMINISTRATOR, Rights.ADMINISTRATOR}, syntax = "Use this command as ::move position [z/height axis is optional]")
public final class MoveCommand implements Command {

	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		if(cmd.length > 3) {
			player.move(new Position(Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2]), Integer.parseInt(cmd[3])));
		} else if(cmd.length == 3) {
			player.move(new Position(Integer.parseInt(cmd[1]), Integer.parseInt(cmd[2]), player.getPosition().getZ()));
		}
	}

}
