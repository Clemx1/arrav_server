package net.edge.content.wilderness;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.edge.event.impl.ObjectEvent;
import net.edge.task.Task;
import net.edge.util.rand.RandomUtils;
import net.edge.locale.Position;
import net.edge.locale.loc.SquareLocation;
import net.edge.world.World;
import net.edge.world.node.entity.player.Player;
import net.edge.world.object.ObjectNode;
import net.edge.world.node.region.Region;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.edge.content.teleport.impl.DefaultTeleportSpell.TeleportType.OBELISK;

/**
 * Handles the wilderness obelisks.
 */
public enum Obelisk {
	LEVEL_FIFTY(14831, new SquareLocation(3307, 3916, 0, 2)),
	LEVEL_SEVENTEEN(14830, new SquareLocation(3219, 3656, 0, 2)),
	LEVEL_THIRTEEN(14829, new SquareLocation(3156, 3620, 0, 2)),
	LEVEL_THIRTY_FIVE(14828, new SquareLocation(3106, 3794, 0, 2)),
	LEVEL_TWENTY_SEVEN(14827, new SquareLocation(3035, 3732, 0, 2)),
	LEVEL_FORTY_FOUR(14826, new SquareLocation(2980, 3866, 0, 2));
	
	/**
	 * Caches our enum values.
	 */
	private static final ImmutableSet<Obelisk> VALUES = Sets.immutableEnumSet(EnumSet.allOf(Obelisk.class));
	
	/**
	 * The obelisk object id.
	 */
	private final int object;
	
	/**
	 * The obelisk's boundary in a {@link SquareLocation}.
	 */
	private final SquareLocation boundary;
	
	Obelisk(int object, SquareLocation boundary) {
		this.object = object;
		this.boundary = boundary;
	}
	
	public int getObject() {
		return object;
	}
	
	/**
	 * Gets the definition for this obelisk.
	 * @param object the object being clicked.
	 * @return an optional holding the {@link Obelisk} value found,
	 * {@link Optional#empty} otherwise.
	 */
	public static Optional<Obelisk> get(ObjectNode object) {
		return VALUES.stream().filter(t -> object.getId() == t.object).findAny();
	}
	
	public static void init() {
		ObjectEvent a = new ObjectEvent() {
			@Override
			public boolean click(Player player, ObjectNode object, int click) {
				Optional<Obelisk> obelisk = get(object);
				if(obelisk.isPresent()) {
					World.get().submit(new ObeliskTask(obelisk.get(), player.getRegion()));
					return true;
				}
				return false;
			}
		};
		VALUES.forEach(o -> a.registerFirst(o.getObject()));
	}
	
	/**
	 * Handles the main obelisk.
	 */
	private static class ObeliskTask extends Task {
		
		/**
		 * The obelisk data toggled.
		 */
		private final Obelisk data;
		
		/**
		 * The {@link Region} instance in which the obelisk is located.
		 */
		private final Region reg;
		
		/**
		 * The obelisks objects.
		 */
		private List<ObjectNode> obelisks;
		
		ObeliskTask(Obelisk data, Region reg) {
			super(8, false);
			this.data = data;
			this.reg = reg;
		}
		
		@Override
		protected void onSubmit() {
			obelisks = reg.getInteractiveObjects(data.object);
			obelisks.forEach(o -> {
				o.setId(14825);
				o.publish();
			});
		}
		
		@Override
		protected void execute() {
			obelisks.forEach(o -> {
				o.setId(data.object);
				o.publish();
			});
			Obelisk dest = RandomUtils.random(VALUES.stream().filter(d -> d != data).collect(Collectors.toList()));
			int x = dest.boundary.getSwX();
			int y = dest.boundary.getSwY();
			reg.getPlayers().forEach((i, p) -> {
				if(data.boundary.inLocation(p.getPosition())) {
					p.teleport(new Position(x + RandomUtils.inclusive(1, 3), y + RandomUtils.inclusive(1, 3)), OBELISK);
					p.message("Ancient magic teleports you somewhere in the wilderness...");
				}
			});
			this.cancel();
		}
	}
}
