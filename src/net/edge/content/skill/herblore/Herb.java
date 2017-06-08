package net.edge.content.skill.herblore;

import net.edge.event.impl.ItemEvent;
import net.edge.task.Task;
import net.edge.content.skill.SkillData;
import net.edge.content.skill.action.impl.ProducingSkillAction;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.item.Item;

import java.util.Optional;

/**
 * Represents procession for identifying of grimy herbs.
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 */
public final class Herb extends ProducingSkillAction {
	
	/**
	 * The {@link GrimyHerb} holding all the data required for processing
	 * the identification for herbs.
	 */
	private final GrimyHerb definition;
	
	/**
	 * Constructs a new {@link Herb}.
	 * @param player {@link #getPlayer()}.
	 */
	private Herb(Player player, GrimyHerb herb) {
		super(player, Optional.of(player.getPosition()));
		definition = herb;
	}
	
	public static void event() {
		for(GrimyHerb h : GrimyHerb.values()) {
			ItemEvent e = new ItemEvent() {
				@Override
				public boolean click(Player player, Item item, int container, int slot, int click) {
					Herb herb = new Herb(player, h);
					herb.start();
					return true;
				}
			};
			e.registerInventory(h.grimy.getId());
		}
	}
	
	@Override
	public void onProduce(Task t, boolean success) {
		t.cancel();
	}
	
	@Override
	public Optional<Item[]> removeItem() {
		return Optional.of(new Item[]{definition.grimy});
	}
	
	@Override
	public Optional<Item[]> produceItem() {
		return Optional.of(new Item[]{definition.clean});
	}
	
	@Override
	public int delay() {
		return 0;
	}
	
	@Override
	public boolean instant() {
		return true;
	}
	
	@Override
	public boolean init() {
		return canProduce();
	}
	
	@Override
	public boolean canExecute() {
		return canProduce();
	}
	
	@Override
	public double experience() {
		return definition.experience;
	}
	
	@Override
	public SkillData skill() {
		return SkillData.HERBLORE;
	}
	
	private boolean canProduce() {
		if(!getPlayer().getSkills()[skill().getId()].reqLevel(definition.level)) {
			getPlayer().message("You need a herblore level of " + definition.level + " to clean this herb.");
			return false;
		}
		return true;
	}
	
	/**
	 * The data required for processing the identifying of grimy herbs.
	 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
	 */
	public enum GrimyHerb {
		GUAM(1, 199, 249, 2.5),
		ROGUESPURSE(3, 1533, 1534, 2.5),
		SNAKEWEED(3, 1525, 1526, 2.5),
		MARRENTILL(5, 201, 251, 3.8),
		TARROMIN(11, 203, 253, 5),
		HARRALANDER(20, 205, 255, 6.3),
		RANNAR(25, 207, 257, 7.5),
		TOADFLAX(30, 3049, 2998, 8),
		IRIT(40, 209, 259, 8.8),
		AVANTOE(48, 211, 261, 10),
		KWUARM(54, 213, 263, 11.3),
		SNAPDRAGON(59, 3051, 3000, 11.8),
		CADANTINE(65, 215, 265, 12.5),
		LANTADYME(67, 2485, 2481, 13.1),
		DWARFWEED(70, 217, 267, 13.8),
		TORSTOL(75, 219, 269, 15);
		
		/**
		 * The required level for creating this potion.
		 */
		private final int level;
		
		/**
		 * The identification for this grimy item.
		 */
		private final Item grimy;
		
		/**
		 * The identification for this clean grimy item.
		 */
		private final Item clean;
		
		/**
		 * The identifier which identifies the amount of experience given for this potion.
		 */
		private final double experience;
		
		/**
		 * Constructs a new {@link GrimyHerb} enum.
		 * @param level      {@link #level}.
		 * @param grimy      {@link #grimy}.
		 * @param clean      {@link #clean}.
		 * @param level      {@link #level}.
		 * @param experience {@link #experience}.
		 */
		GrimyHerb(int level, int grimy, int clean, double experience) {
			this.level = level;
			this.grimy = new Item(grimy);
			this.clean = new Item(clean);
			this.experience = experience;
		}
	}
}
