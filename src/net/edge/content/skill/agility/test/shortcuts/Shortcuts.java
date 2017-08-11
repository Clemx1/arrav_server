package net.edge.content.skill.agility.test.shortcuts;

import net.edge.action.impl.ObjectAction;
import net.edge.content.skill.agility.obstacle.impl.Walkable;
import net.edge.content.skill.agility.test.Agility;
import net.edge.content.skill.agility.test.gnome.JumpOverBarrier;
import net.edge.content.skill.agility.test.gnome.PoleSwing;
import net.edge.content.skill.agility.test.obstacle.Obstacle;
import net.edge.content.skill.agility.test.obstacle.impl.ClimbableObstacle;
import net.edge.content.skill.agility.test.obstacle.impl.FMObstacle;
import net.edge.content.skill.agility.test.obstacle.impl.WalkableObstacle;
import net.edge.task.LinkedTaskSequence;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.locale.Position;
import net.edge.world.object.GameObject;

import java.util.OptionalInt;
import java.util.function.Function;

/**
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 * @since 11-8-2017.
 */
public final class Shortcuts extends Agility {

    /**
     * Constructs a new {@link Shortcuts}.
     *
     * @param player        {@link #player}.
     * @param object        {@link #object}.
     * @param crossing      {@link #crossing}.
     * @param travelback    {@link #travelback}.
     */
    public Shortcuts(Player player, GameObject object, Obstacle crossing, boolean travelback) {
        super(player, object, crossing, travelback);
    }

    public static void action() {
        for(Obstacles obstacleFunction : Obstacles.values()) {
            ObjectAction action = new ObjectAction() {

                @Override
                public boolean click(Player player, GameObject object, int click) {
                    Obstacle obstacle = obstacleFunction.obstacles.apply(player);

                    //NOTICE: ALWAYS SHOULD FIND PROPER POSITION FOR SHORTCUTS

                    boolean startPoint = player.getPosition().getDistance(obstacle.start[0]) < player.getPosition().getDistance(obstacle.end);
                    Position startPosition = startPoint ? obstacle.start[0] : obstacle.end;
                    player.getMovementQueue().smartWalk(startPosition);
                    player.getMovementListener().append(() -> {
                        if(player.getPosition().same(startPosition)) {
                            LinkedTaskSequence seq = new LinkedTaskSequence();
                            seq.connect(1, () -> new Shortcuts(player, object, obstacle, !startPoint).start());
                            seq.start();
                        }
                    });
                    return true;
                }
            };
            for(int objId : obstacleFunction.ids) {
                action.registerFirst(objId);
            }
        }
    }

    /**
     * The experience given from this skill action.
     *
     * @return the experience given.
     */
    @Override
    public double experience() {
        return 0;
    }

    private enum Obstacles {
        TAVERLY_STRANGE_FLOOR(51297, p -> new FMObstacle(50, OptionalInt.empty(), new Position(2880, 9813), new Position(2878, 9813), 3067, 80, 0) {
            @Override
            public boolean travelback() {
                return true;
            }
        }),
        TAVERLY_OBSTACLE_PIPE(51296, p -> new WalkableObstacle(new Position(2886, 9799), new Position(2892, 9799), 844, 70, 0) {
            @Override
            public boolean travelback() {
                return true;
            }
        }),
        BRIMHAVEN_OBSTACLE_PIPE(5100, p -> new WalkableObstacle(new Position(2655, 9566), new Position(2655, 9573), 844, 70, 0) {
            @Override
            public boolean travelback() {
                return true;
            }
        }),
        BRIMHAVEN_LOG_BALANCE(new int[]{5088, 5090}, p -> new WalkableObstacle(new Position(2682, 9506), new Position(2687, 9506), 762, 70, 0) {
            @Override
            public boolean travelback() {
                return true;
            }
        });


        public final int[] ids;

        Function<Player, Obstacle> obstacles;

        Obstacles(int[] ids, Function<Player, Obstacle> obstacles) {
            this.obstacles = obstacles;
            this.ids = ids;
        }

        Obstacles(int id, Function<Player, Obstacle> obstacles) {
            this.obstacles = obstacles;
            this.ids = new int[]{id};
        }
    }
}