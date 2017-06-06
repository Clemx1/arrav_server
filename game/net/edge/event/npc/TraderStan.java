package net.edge.event.npc;

import net.edge.content.dialogue.impl.NpcDialogue;
import net.edge.content.dialogue.impl.OptionDialogue;
import net.edge.content.dialogue.impl.PlayerDialogue;
import net.edge.content.dialogue.test.DialogueAppender;
import net.edge.content.teleport.impl.AuburyTeleport;
import net.edge.event.EventInitializer;
import net.edge.event.impl.NpcEvent;
import net.edge.task.Task;
import net.edge.util.rand.RandomUtils;
import net.edge.world.World;
import net.edge.world.node.entity.npc.Npc;
import net.edge.world.node.entity.player.Player;

public class TraderStan extends EventInitializer {
	
	@Override
	public void init() {
		String[] chats = {
				"Come here come here I have a shiny scimitar for you!",
				"Discounted prices, come look!",
				"Only for you, only today! Exceptional offer.",
				"Don't be shy, I won't bite!",
				"This sword was King Arthur's! I swear.",
				"I buy all kinds of nice things.",
				"Search any item you want, I think I have it.",
				"All offers are welcome here.",
				"That would be few coins sir.",
				"Ladies and gentlemens, gather around.",
				"Wait let me note it down on my papirus.",
				"Yes that's an antifier shield on the wall.",
				"My mom actually did fit in that rune suite.",
				"Please be quite don't disturb the owner Sir Prysin",
				"My exchange level is 99",
				"I love coins, I very do.",
				"Look at my market! please...",
				"I'm the wealthiest person in Edgeville.",
				"I'm rich, very rich. Making Edgeville great again.",
				"Who you think rebuild Edgeville with all the money?",
				"My parents once told me to have financial eased friends.",
				"May the gold be with you.",
				"I aint gonna say I had a small loan of a million dollars from my Dad."
		};
		Npc stan = World.get().getNpcs().stream().filter(n -> n.getId() == 4650).findAny().get();
		NpcEvent e = new NpcEvent() {
			@Override
			public boolean click(Player player, Npc npc, int click) {
				DialogueAppender app = new DialogueAppender(player);
				app.chain(new NpcDialogue(5913, "Hello " + player.getFormatUsername() + ", my name is Stan.", "How may I help you today?"));
				app.chain(new OptionDialogue(t -> {
					if(t.equals(OptionDialogue.OptionType.FIRST_OPTION)) {
						app.getBuilder().advance();
					} else if(t.equals(OptionDialogue.OptionType.SECOND_OPTION)) {
						app.getBuilder().skip();
					} else if(t.equals(OptionDialogue.OptionType.THIRD_OPTION)){
						app.getBuilder().go(3);
					} else {
						app.getBuilder().last();
					}
				}, "Search all shops.", "Search for item", "Open my shop.", "Nevermind"));
				app.chain(new PlayerDialogue("I would like to search for all shops.").attachAfter(() -> {
					player.getMessages().sendCloseWindows();
					//open all the shops here
				}));
				app.chain(new PlayerDialogue("I would like to search for an item.").attachAfter(() -> {
					player.getMessages().sendCloseWindows();
					//search for item
				}));
				app.chain(new PlayerDialogue("I would like to open my shop.").attachAfter(() -> {
					player.getMessages().sendCloseWindows();
					//open own shop
				}));
				app.chain(new PlayerDialogue("Nevermind..."));
				app.start();
				return true;
			}
		};
		e.registerFirst(4650);
		Task yell = new Task(150, false) {
			@Override
			protected void execute() {
				stan.forceChat(RandomUtils.random(chats));
			}
		};
		yell.submit();
	}
}
