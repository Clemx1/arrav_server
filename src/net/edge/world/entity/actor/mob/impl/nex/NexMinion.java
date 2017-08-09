package net.edge.world.entity.actor.mob.impl.nex;

import net.edge.world.entity.actor.mob.Mob;
import net.edge.world.entity.actor.mob.strategy.impl.NexStrategy;
import net.edge.world.locale.Position;

import java.util.Optional;

/**
 * The class which represents a nex minion.
 * @author Artem Batutin <artembatutin@gmail.com>
 */
public final class NexMinion extends Mob {
	
	/**
	 * Constructs a new {@link NexMinion}.
	 */
	public NexMinion() {
		super(9177, new Position(3386, 3517));
	}
	
	@Override
	public Mob create() {
		return new NexMinion();
	}
	
}
