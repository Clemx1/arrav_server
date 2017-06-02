package net.edge.content.commands.impl;

import net.edge.content.commands.Command;
import net.edge.content.commands.CommandSignature;
import net.edge.content.skill.Skills;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.assets.Rights;

@CommandSignature(alias = {"master"}, rights = {Rights.DEVELOPER}, syntax = "Use this command as just ::master")
public final class MasterCommand implements Command {
	
	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		for(int i = 0; i < player.getSkills().length; i++) {
			Skills.experience(player, (Integer.MAX_VALUE - player.getSkills()[i].getExperience()), i);
		}
	}
	
}