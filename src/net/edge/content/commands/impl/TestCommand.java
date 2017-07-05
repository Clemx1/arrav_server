package net.edge.content.commands.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.edge.content.commands.Command;
import net.edge.content.commands.CommandSignature;
import net.edge.util.rand.RandomUtils;
import net.edge.world.node.entity.npc.drop.ItemCache;
import net.edge.world.node.entity.npc.drop.NpcDrop;
import net.edge.world.node.entity.npc.drop.NpcDropManager;
import net.edge.world.node.entity.npc.drop.NpcDropTable;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.assets.Rights;
import net.edge.world.node.item.Item;

import java.util.concurrent.ThreadLocalRandom;

@CommandSignature(alias = {"test"}, rights = {Rights.ADMINISTRATOR}, syntax = "Use this command as ::test")
public final class TestCommand implements Command {

	@Override
	public void execute(Player player, String[] cmd, String command) throws Exception {
		NpcDropTable table = NpcDropManager.TABLES.get(-1);//barrows custom.
		ObjectList<Item> loot = new ObjectArrayList<>();
		
		int expect = RandomUtils.inclusive(4,8);
		int gathered = 0;
		while(gathered != expect) {
			
			//random cache
			ItemCache cache = RandomUtils.random(table.getCommon());
			
			//random item from cache
			NpcDrop drop = RandomUtils.random(NpcDropManager.COMMON.get(cache));
			
			//roll
			if(drop.roll(ThreadLocalRandom.current())) {
				loot.add(new Item(drop.getId(), RandomUtils.inclusive(drop.getMinimum(), drop.getMaximum())));
				gathered++;
			}
		}
		
		
		player.getInventory().addOrDrop(loot);
	}

}
