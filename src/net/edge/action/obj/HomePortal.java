package net.edge.action.obj;

import net.edge.content.dialogue.impl.OptionDialogue;
import net.edge.content.skill.Skill;
import net.edge.action.ActionInitializer;
import net.edge.action.impl.ObjectAction;
import net.edge.world.locale.Position;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.object.ObjectNode;

import static net.edge.content.teleport.impl.DefaultTeleportSpell.TeleportType.LADDER;

public class HomePortal extends ActionInitializer {
	@Override
	public void init() {
		//Green portal at home
		ObjectAction portal = new ObjectAction() {
			@Override
			public boolean click(Player player, ObjectNode object, int click) {
				player.getDialogueBuilder().append(new OptionDialogue(t -> {
					if(t.equals(OptionDialogue.OptionType.FIRST_OPTION)) {
						player.widget(-4);
					} else if(t.equals(OptionDialogue.OptionType.SECOND_OPTION)) {
						player.widget(-6);
					} else if(t.equals(OptionDialogue.OptionType.THIRD_OPTION)) {
						player.widget(-9);
					} else if(t.equals(OptionDialogue.OptionType.FOURTH_OPTION)) {
						player.widget(-7);
					}
				}, "Skills", "Minigames", "Monsters", "Bosses"));
				return true;
			}
		};
		portal.registerFirst(28139);
		
		//Home staires
		ObjectAction staires = new ObjectAction() {
			@Override
			public boolean click(Player player, ObjectNode object, int click) {
				if(object.getGlobalPos().same(new Position(3082, 3510, 0))) {
					if(!player.getRights().isStaff() && !player.getRights().isDonator()) {
						int total = 0;
						for(Skill s : player.getSkills())
							total += s.getRealLevel();
						if(total < 2000) {
							player.message("You need total level greater than 2000 to go up, or be a donator.");
							return true;
						}
					}
					player.teleport(new Position(3081, 3510, 1), LADDER);
				}
				return true;
			}
		};
		staires.registerFirst(2711);
		
		//Bossing hub staires
		ObjectAction hub = new ObjectAction() {
			@Override
			public boolean click(Player player, ObjectNode object, int click) {
				player.getDialogueBuilder().append(new OptionDialogue(t -> {
					if(t.equals(OptionDialogue.OptionType.SECOND_OPTION)) {
						player.widget(-9);
					} else if(t.equals(OptionDialogue.OptionType.THIRD_OPTION)) {
						player.widget(-7);
					} else {
						player.closeWidget();
					}
				}, "@red@This will be clan bossing hub soon!", "Monsters", "Bosses"));
				return true;
			}
		};
		hub.registerFirst(2522);
		
	}
}
