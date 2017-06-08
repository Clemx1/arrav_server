package net.edge.content.shootingstar;

import net.edge.util.Stopwatch;
import net.edge.util.rand.RandomUtils;
import net.edge.world.World;
import net.edge.world.node.NodeState;
import net.edge.world.node.entity.player.Player;

import java.util.concurrent.TimeUnit;

/**
 * The manager class for the shooting star event objects.
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 */
public final class ShootingStarManager {

	/**
	 * Represents the shooting star object.
	 */
	private ShootingStar star;
	
	/**
	 * The stopwatch which will spawn a shooting star every 30 minutes.
	 */
	final Stopwatch stopwatch = new Stopwatch();
	
	/**
	 * The process method which is invoked every minute on start-up.
	 */
	public void process() {
		if(!stopwatch.elapsed(28, TimeUnit.MINUTES) || (star != null && star.sprite.getState().equals(NodeState.ACTIVE))) { // 1 minute correction, because task runs every minute and it might skip.
			return;
		}
		stopwatch.reset();
		if(star != null && !star.isDisabled()) {
			World.get().message(star.locationData.messageActive, true);
			return;
		}
		spawn();
	}
	
	/**
	 * Attempts to spawn the shooting star.
	 */
	public void spawn() {
		star = generateStar();
		star.publish();
		World.get().message(star.locationData.message, true);
	}
	
	/**
	 * Generates a shooting star on a random location.
	 * @return a shooting star on a random location.
	 */
	public ShootingStar generateStar() {
		return new ShootingStar(RandomUtils.random(StarLocationData.VALUES.asList()));
	}
	
	/**
	 * Gets the shooting star.
	 * @return the shooting star.
	 */
	public ShootingStar getShootingStar() {
		return star;
	}

	/**
	 * Attempts to mine the shooting star.
	 * @param player   the player attempting to mine.
	 * @param objectId the object id clicked to mine.
	 * @return {@code true} if the object was mined, {@code false} otherwise.
	 */
	public boolean mine(Player player, int objectId) {
		if(star == null) {
			return false;
		}
		return star.mine(player, objectId);
	}
}