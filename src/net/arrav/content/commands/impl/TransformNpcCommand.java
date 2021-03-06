package net.arrav.content.commands.impl;

import net.arrav.content.commands.Command;
import net.arrav.content.commands.CommandSignature;
import net.arrav.world.entity.actor.player.Player;
import net.arrav.world.entity.actor.player.assets.Rights;
import net.arrav.world.entity.actor.update.UpdateFlag;

@CommandSignature(alias = {"pmob", "pnpc", "transformnpc"}, rights = {Rights.ADMINISTRATOR}, syntax = "Transforms to a mob, ::pnpc id")
public final class TransformNpcCommand implements Command {
	
	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		int id = Integer.parseInt(cmd[1]);
		player.setPlayerNpc(id);
		player.getFlags().flag(UpdateFlag.APPEARANCE);
	}
	
}
