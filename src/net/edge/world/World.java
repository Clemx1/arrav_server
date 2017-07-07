package net.edge.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.edge.Server;
import net.edge.content.commands.impl.UpdateCommand;
import net.edge.content.trivia.TriviaTask;
import net.edge.game.GameConstants;
import net.edge.game.GameExecutor;
import net.edge.game.GamePulseHandler;
import net.edge.net.database.Database;
import net.edge.net.database.pool.ConnectionPool;
import net.edge.task.Task;
import net.edge.task.TaskManager;
import net.edge.util.LoggerUtils;
import net.edge.util.Stopwatch;
import net.edge.util.ThreadUtil;
import net.edge.util.log.LoggingManager;
import net.edge.content.PlayerPanel;
import net.edge.content.clanchat.ClanManager;
import net.edge.world.node.entity.player.assets.Rights;
import net.edge.world.node.item.container.session.ExchangeSessionManager;
import net.edge.content.scoreboard.ScoreboardManager;
import net.edge.content.shootingstar.ShootingStarManager;
import net.edge.content.skill.firemaking.pits.FirepitManager;
import net.edge.locale.InstanceManager;
import net.edge.locale.area.AreaManager;
import net.edge.world.node.NodeState;
import net.edge.world.node.entity.EntityList;
import net.edge.world.node.entity.EntityNode;
import net.edge.world.node.entity.move.path.AStarPathFinder;
import net.edge.world.node.entity.move.path.SimplePathChecker;
import net.edge.world.node.entity.move.path.impl.SimplePathFinder;
import net.edge.world.node.entity.move.path.distance.Manhattan;
import net.edge.world.node.entity.npc.Npc;
import net.edge.world.node.entity.npc.NpcMovementTask;
import net.edge.world.node.entity.npc.NpcUpdater;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.entity.player.PlayerUpdater;
import net.edge.world.node.item.ItemNode;
import net.edge.world.node.region.Region;
import net.edge.world.node.region.RegionManager;
import net.edge.world.node.region.TraversalMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The static utility class that contains functions to manage and process game characters.
 * @author Artem Batutin <artembatutin@gmail.com>
 */
public final class World {
	
	/**
	 * The logger that will print important information.
	 */
	private static Logger logger = LoggerUtils.getLogger(World.class);
	
	/**
	 * An implementation of the singleton pattern to prevent indirect
	 * instantiation of this class file.
	 */
	private static final World singleton = new World();

	/**
	 * The scheduled executor service.
	 */
	private final ScheduledExecutorService sync = Executors.newSingleThreadScheduledExecutor(ThreadUtil.create("GamePulse"));
	
	/**
	 * The game executor in charge of managing process.
	 */
	private final GameExecutor executor = new GameExecutor();
	
	/**
	 * The manager for the queue of game tasks.
	 */
	private final TaskManager taskManager = new TaskManager();
	
	/**
	 * The queue of {@link Player}s waiting to be logged in.
	 */
	private final Queue<Player> logins = new ConcurrentLinkedQueue<>();
	
	/**
	 * The queue of {@link Player}s waiting to be logged out.
	 */
	private final Queue<Player> logouts = new ConcurrentLinkedQueue<>();
	
	/**
	 * The collection of active NPCs.
	 */
	private final EntityList<Npc> npcs = new EntityList<>(16384);
	
	/**
	 * The collection of active players.
	 */
	private final EntityList<Player> players = new EntityList<>(2048);
	
	/**
	 * Amount of staff players online.
	 */
	private int staffCount;
	
	/**
	 * The regional tick counter for processing such as {@link ItemNode} in a region.
	 */
	private int regionalTick;
	
	/**
	 * The time it took in milliseconds to do the sync.
	 */
	public static long millis;
	
	static {
		int amtCpu = Runtime.getRuntime().availableProcessors();
		try {
			donation = new Database(!Server.DEBUG ? "127.0.0.1" : "192.95.33.132", "edge_donate", !Server.DEBUG ? "root" : "edge_avro", !Server.DEBUG ? "FwKVM3/2Cjh)f?=j": "%GL5{)hAJBU(MB3h", amtCpu);
			score = new Database(!Server.DEBUG ? "127.0.0.1" : "192.95.33.132", "edge_score", !Server.DEBUG ? "root" : "edge_avro", !Server.DEBUG ? "FwKVM3/2Cjh)f?=j": "%GL5{)hAJBU(MB3h", amtCpu);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts the synchronized pulser.
	 */
	public void start() {
		sync.scheduleAtFixedRate(new GamePulseHandler(this), 600, 600, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Sends a task to the synchronized pulser.
	 * @param r task to be sent.
	 */
	public void run(Runnable r) {
		sync.submit(r);
	}
	
	/**
	 * Closes the synchronized pulser.
	 */
	public void shutdown() {
		sync.shutdownNow();
	}
	
	/**
	 * The method that executes the update sequence for all in game characters every cycle.
	 */
	public void pulse() {
		long start = System.currentTimeMillis();
		// Handle queued logins.
		for(int amount = 0; amount < GameConstants.LOGIN_THRESHOLD; amount++) {
			Player player = logins.poll();
			if(player == null)
				break;
			if(!players.add(player) && player.isHuman()) {
				player.getSession().getChannel().close();
			}
		}
		
		// Handle task processing.
		taskManager.sequence();
		
		Npc n;
		Player p;
		EntityList.EntityListIterator<Npc> ni = npcs.entityIterator();
		EntityList.EntityListIterator<Player> pi = players.entityIterator();
		
		// Pre synchronization
		while((p = pi.next()) != null) {
			try {
				p.getMovementQueue().sequence();
				p.sequence();
			} catch(Exception e) {
				queueLogout(p);
				logger.log(Level.WARNING, "Couldn't pre sync player " + p.toString(), e);
			}
		}
		pi.reset();
		while((n = ni.next()) != null) {
			if(n.isActive()) {
				try {
					n.sequence();
					n.getMovementQueue().sequence();
				} catch(Exception e) {
					logger.log(Level.WARNING, "Couldn't pre sync npc " + n.toString(), e);
				}
			}
		}
		ni.reset();
		
		// Synchronization
		while((p = pi.next()) != null) {
			try {
				if(p.isHuman()) {
					PlayerUpdater.write(p);
					NpcUpdater.write(p);
				}
			} catch(Exception e) {
				queueLogout(p);
				logger.log(Level.WARNING, "Couldn't sync player " + p.toString(), e);
			}
		}
		pi.reset();
		
		// Post synchronization
		while((p = pi.next()) != null) {
			if(p.isHuman())
				p.getSession().flushQueue();
			p.reset();
			p.setCachedUpdateBlock(null);
		}
		pi.reset();
		while((n = ni.next()) != null) {
			n.reset();
		}
		ni.reset();
		
		// Region tick
		regionalTick++;
		if(regionalTick == 10) {
			for(Int2ObjectMap.Entry<Region> entry : World.getRegions().getRegions().int2ObjectEntrySet()) {
				entry.getValue().sequence();
			}
			regionalTick = 0;
		}
		
		// Handle queued logouts.
		int amount = 0;
		Iterator<Player> $it = logouts.iterator();
		while($it.hasNext()) {
			Player player = $it.next();
			if(player == null || amount >= GameConstants.LOGOUT_THRESHOLD)
				break;
			if(logout(player)) {
				$it.remove();
				amount++;
			}
		}
		
		millis = System.currentTimeMillis() - start;

		System.out.println("Took " + millis + " ms");
	}
	
	/**
	 * Queues {@code player} to be logged in on the next server sequence.
	 * @param player the player to log in.
	 */
	public void queueLogin(Player player) {
		if(!logins.contains(player)) {
			logins.add(player);
		}
	}
	
	/**
	 * Queues {@code player} to be logged out on the next server sequence.
	 * @param player the player to log out.
	 */
	public void queueLogout(Player player) {
		if(player.getState() == NodeState.ACTIVE && !logouts.contains(player)) {
			if(player.getCombatBuilder().inCombat())
				player.getLogoutTimer().reset();
			logouts.add(player);
		}
	}
	
	/**
	 * Submits {@code t} to the backing {@link TaskManager}.
	 * @param t the task to submit to the queue.
	 */
	public void submit(Task t) {
		taskManager.submit(t);
	}
	
	/**
	 * Returns a player within an optional whose name hash is equal to
	 * {@code username}.
	 * @param username the name hash to check the collection of players for.
	 * @return the player within an optional if found, or an empty optional if
	 * not found.
	 */
	public Optional<Player> getPlayer(long username) {
		Player p;
		Iterator<Player> it = players.entityIterator();
		while((p = it.next()) != null) {
			if(p.getUsernameHash() == username)
				return Optional.of(p);
		}
		return Optional.empty();
	}
	
	/**
	 * Returns a player within an optional whose name is equal to
	 * {@code username}.
	 * @param username the name to check the collection of players for.
	 * @return the player within an optional if found, or an empty optional if
	 * not found.
	 */
	public Optional<Player> getPlayer(String username) {
		Player p;
		Iterator<Player> it = players.entityIterator();
		while((p = it.next()) != null) {
			if(Objects.equals(p.getUsername(), username))
				return Optional.of(p);
		}
		return Optional.empty();
	}
	
	/**
	 * Retrieves and returns the local {@link Player}s for {@code character}.
	 * The specific players returned is completely dependent on the character
	 * given in the argument.
	 * @param character the character that it will be returned for.
	 * @return the local players.
	 */
	public Iterator<Player> getLocalPlayers(EntityNode character) {
		if(character.isPlayer())
			return character.toPlayer().getLocalPlayers().iterator();
		return players.iterator();
	}
	
	/**
	 * Retrieves and returns the local {@link Npc}s for {@code character}. The
	 * specific npcs returned is completely dependent on the character given in
	 * the argument.
	 * @param character the character that it will be returned for.
	 * @return the local npcs.
	 */
	public Iterator<Npc> getLocalNpcs(EntityNode character) {
		if(character.isPlayer())
			return character.toPlayer().getLocalNpcs().iterator();
		return npcs.iterator();
	}
	
	/**
	 * Gets every single character in the player and npc character lists.
	 * @return a set containing every single character.
	 */
	public Set<EntityNode> getEntities() {
		Set<EntityNode> characters = new HashSet<>();
		players.forEach(characters::add);
		npcs.forEach(characters::add);
		return characters;
	}
	
	/**
	 * Sends {@code message} to all online players with an announcement dependent of {@code announcement}.
	 * @param message      the message to send to all online players.
	 * @param announcement determines if this message is an announcement.
	 */
	public void message(String message, boolean announcement) {
		Iterator<Player> it = players.entityIterator();
		Player p;
		while((p = it.next()) != null) {
			p.message((announcement ? "@red@[ANNOUNCEMENT]: " : "") + message);
		}
	}
	
	/**
	 * Sends {@code message} to all online players.
	 * @param message      the message to send to all online players.
	 */
	public void message(String message) {
		message(message, false);
	}
	
	/**
	 * Sends {@code message} to all online players as a yell.
	 * @param author author yelling.
	 * @param message the message being yelled.
	 * @param rights the rights of the author.
	 */
	public void yell(String author, String message, Rights rights) {
		Iterator<Player> it = players.entityIterator();
		Player p;
		while((p = it.next()) != null) {
			p.getMessages().sendYell(author, message, rights);
		}
	}
	
	/**
	 * Performs all of the disconnection logic for {@code player} assuming they
	 * are in the logout queue.
	 * @param player the player to attempt to logout.
	 * @return {@code true} if the player was logged out, {@code false}
	 * otherwise.
	 */
	public boolean logout(Player player) {
		try {
			// If the player x-logged, don't log the player out. Keep the
			// player queued until they are out of combat to prevent x-logging.
			if(player.getLogoutTimer().elapsed(GameConstants.LOGOUT_SECONDS, TimeUnit.SECONDS) && player.getCombatBuilder().isBeingAttacked() && UpdateCommand.inProgess == 0) {
				return false;
			}
			boolean response = players.remove(player);
			for(Npc mob : player.getMobs()) {
				npcs.remove(mob);
			}
			player.getMobs().clear();
			if(response)
				logger.info(player.toString() + " has logged out.");
			else
				logger.info(player.toString() + " couldn't be logged out.");
			return response;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Gets the game executor which forwards process.
	 * @return game executor.
	 */
	public GameExecutor getExecutor() {
		return executor;
	}
	
	/**
	 * Gets the manager for the queue of game tasks.
	 * @return the queue of tasks.
	 */
	public TaskManager getTask() {
		return taskManager;
	}

	/**
	 * Gets the collection of active players.
	 * @return the active players.
	 */
	public EntityList<Player> getPlayers() {
		return players;
	}
	
	/**
	 * Gets the staff count online.
	 * @return staff count.
	 */
	public int getStaffCount() {
		return staffCount;
	}
	
	/**
	 * Sets a new staff count.
	 * @param staffCount staff count to set.
	 */
	public void setStaffCount(int staffCount) {
		this.staffCount = staffCount;
	}
	
	/**
	 * Gets the collection of active npcs.
	 * @return the active npcs.
	 */
	public EntityList<Npc> getNpcs() {
		return npcs;
	}


	/* CONSTANTS DECLARATIONS */
	
	/**
	 * The trivia task for this world.
	 */
	private static final TriviaTask TRIVIA_BOT = new TriviaTask();
	
	/**
	 * The time the server has been running.
	 */
	private static final Stopwatch RUNNING_TIME = new Stopwatch().reset();
	
	/**
	 * The shooting star event for the world.
	 */
	private static final ShootingStarManager SHOOTING_STAR_MANAGER = new ShootingStarManager();
	
	/**
	 * The fire pit event for the world.
	 */
	private static final FirepitManager FIRE_PIT_EVENT = new FirepitManager();
	
	/**
	 * The clan manager for the world.
	 */
	private static final ClanManager CLAN_MANAGER = new ClanManager();
	
	/**
	 * The instance manager for the world.
	 */
	private static final InstanceManager INSTANCE_MANAGER = new InstanceManager();
	
	/**
	 * The {@link RegionManager} that manages region caching.
	 */
	private static final RegionManager REGION_MANAGER = new RegionManager();
	
	/**
	 * This world's {@link TraversalMap} used to handle the clipping around the world.
	 */
	private static final TraversalMap TRAVERSAL_MAP = new TraversalMap();
	
	/**
	 * This world's {@link AreaManager} used to handle to check if certain areas have permissions.
	 */
	private static final AreaManager AREA_MANAGER = new AreaManager();
	
	/**
	 * The world's A* path finder use for player's movements.
	 */
	private static final AStarPathFinder A_STAR_PATH_FINDER = new AStarPathFinder(TRAVERSAL_MAP, new Manhattan());
	
	/**
	 * This world's straight line pathfinder used for NPCs movements.
	 */
	private static final SimplePathFinder SIMPLE_PATH_FINDER = new SimplePathFinder(TRAVERSAL_MAP);
	
	/**
	 * This world's straight line path checker for combat.
	 */
	private static final SimplePathChecker SIMPLE_PATH_CHECKER = new SimplePathChecker(TRAVERSAL_MAP);
	
	/**
	 * This world's {@link ExchangeSessionManager} used to handle container sessions.
	 */
	private static final ExchangeSessionManager EXCHANGE_SESSION_MANAGER = new ExchangeSessionManager();
	
	/**
	 * This world's {@link LoggingManager} used to log player actions.
	 */
	private static final LoggingManager LOG_MANAGER = new LoggingManager();
	
	/**
	 * This world's {@link ScoreboardManager} used to check scores.
	 */
	private static final ScoreboardManager SCOREBOARD_MANAGER = new ScoreboardManager();
	
	/**
	 * A integral {@link Task} that handles ranomized movement of all {@link Npc}s.
	 */
	private static final NpcMovementTask NPC_MOVEMENT_TASK = new NpcMovementTask();
	
	/**
	 * The scores database connection.
	 */
	private static Database score;
	
	/**
	 * The donation database connection.
	 */
	private static Database donation;
	
	
	/* ASSETS GATHERS METHODS. */
	
	/**
	 * Returns the trivia bot handler.
	 * @return
	 */
	public static TriviaTask getTriviaBot() {
		return TRIVIA_BOT;
	}
	
	/**
	 * Returns this world's {@link ExchangeSessionManager}.
	 */
	public static ExchangeSessionManager getExchangeSessionManager() {
		return EXCHANGE_SESSION_MANAGER;
	}
	
	/**
	 * Returns this world's {@link LoggingManager}.
	 */
	public static LoggingManager getLoggingManager() {
		return LOG_MANAGER;
	}
	
	/**
	 * Returns this world's {@link ScoreboardManager}.
	 */
	public static ScoreboardManager getScoreboardManager() {
		return SCOREBOARD_MANAGER;
	}
	
	/**
	 * Returns the running time {@link Stopwatch}.
	 */
	public static Stopwatch getRunningTime() {
		return RUNNING_TIME;
	}
	
	/**
	 * Returns the fire pit event manager.
	 */
	public static FirepitManager getFirepitEvent() {
		return FIRE_PIT_EVENT;
	}
	
	/**
	 * Returns the shooting star event manager.
	 */
	public static ShootingStarManager getShootingStarEvent() {
		return SHOOTING_STAR_MANAGER;
	}
	
	/**
	 * Returns the clan chat manager.
	 */
	public static ClanManager getClanManager() {
		return CLAN_MANAGER;
	}
	
	/**
	 * Returns the instance manager.
	 */
	public static InstanceManager getInstanceManager() {
		return INSTANCE_MANAGER;
	}
	
	/**
	 * Returns this world's {@link RegionManager}.
	 */
	public static RegionManager getRegions() {
		return REGION_MANAGER;
	}
	
	/**
	 * Returns this world's {@link TraversalMap}.
	 */
	public static TraversalMap getTraversalMap() {
		return TRAVERSAL_MAP;
	}
	
	/**
	 * Returns this world'{@link AreaManager}.
	 */
	public static AreaManager getAreaManager() {
		return AREA_MANAGER;
	}
	
	/**
	 * Returns this world's A* pathfinder.
	 */
	public static AStarPathFinder getAStarPathFinder() {
		return A_STAR_PATH_FINDER;
	}
	
	/**
	 * Returns this world's straight pathfinder.
	 */
	public static SimplePathFinder getSimplePathFinder() {
		return SIMPLE_PATH_FINDER;
	}
	
	/**
	 * Returns this world's straight path checker.
	 */
	public static SimplePathChecker getSimplePathChecker() {
		return SIMPLE_PATH_CHECKER;
	}
	
	/**
	 * Returns the randomized npc task.
	 */
	public static NpcMovementTask getNpcMovementTask() {
		return NPC_MOVEMENT_TASK;
	}
	
	/**
	 * Gets the highscores database connection pool.
	 */
	public static ConnectionPool getScore() {
		if(score == null)
			return null;
		return score.getPool();
	}
	
	/**
	 * Gets the donation database connection pool.
	 */
	public static Connection getDonation() {
		if(donation == null)
			return null;
		try {
			return donation.getConnection();
		} catch(SQLException e) {
			return null;
		}
	}
	
	/**
	 * Returns the singleton pattern implementation.
	 * @return The returned implementation.
	 */
	public static World get() {
		return singleton;
	}
	
}