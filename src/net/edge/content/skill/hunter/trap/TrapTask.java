package net.edge.content.skill.hunter.trap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.edge.task.Task;
import net.edge.util.rand.RandomUtils;
import net.edge.content.skill.hunter.Hunter;
import net.edge.world.node.entity.player.Player;

/**
 * Represents a single task which will run for each trap.
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 */
public final class TrapTask extends Task {
	
	/**
	 * The player this task is dependant of.
	 */
	private final Player player;
	
	/**
	 * The trap this task is running for.
	 */
	private ObjectList<Trap> trap = new ObjectArrayList<>();
	
	/**
	 * Constructs a new {@link TrapTask}.
	 * @param player {@link #player}.
	 */
	public TrapTask(Player player) {
		super(10, false);
		this.player = player;
	}
	
	@Override
	public void onSequence() {
		if(Hunter.GLOBAL_TRAPS.get(player) == null || !Hunter.GLOBAL_TRAPS.get(player).getTask().isPresent() || Hunter.GLOBAL_TRAPS.get(player).getTraps().isEmpty()) {
			this.cancel();
			return;
		}
		
		this.trap.forEach(t -> {
			boolean withinDistance = player.getPosition().withinDistance(t.getObject().getGlobalPos(), 25);
			if(!withinDistance && !t.isAbandoned()) {
				Hunter.abandon(player, t, false);
			}
		});
		
	}
	
	@Override
	public void execute() {
		this.trap.clear();
		this.trap.addAll(Hunter.GLOBAL_TRAPS.get(player).getTraps());
		
		Trap trap = RandomUtils.random(this.trap);
		if(!Hunter.getTrap(player, trap.getObject()).isPresent() || !trap.getState().equals(Trap.TrapState.PENDING)) {
			return;
		}
		
		trap.onSequence(this);
	}
}
