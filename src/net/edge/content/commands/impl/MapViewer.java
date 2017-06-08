package net.edge.content.commands.impl;

import net.edge.content.commands.Command;
import net.edge.content.commands.CommandSignature;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.assets.Rights;
import tool.mapviewer.MapTool;

@CommandSignature(alias = {"mapviewer"}, rights = {Rights.DEVELOPER}, syntax = "Use this command as ::tool.mapviewer")
public final class MapViewer implements Command {

	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		MapTool.start();
	}

}