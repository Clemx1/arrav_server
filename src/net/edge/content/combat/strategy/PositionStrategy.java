//package net.edge.content.combat.strategy;
//
//import net.edge.content.combat.CombatType;
//import net.edge.content.combat.attack.FightType;
//import net.edge.content.combat.hit.CombatHit;
//import net.edge.content.combat.hit.Hit;
//import net.edge.world.Animation;
//import net.edge.world.entity.actor.Actor;
//import net.edge.world.locale.Position;
//
//import java.util.List;
//
//public class PositionStrategy extends CombatStrategy<Actor> {
//    private final List<Position> positions;
//    private final boolean global;
//    private final CombatType type;
//
//    public PositionStrategy(CombatType type, boolean global, List<Position> positions) {
//        this.type = type;
//        this.global = global;
//        this.positions = positions;
//    }
//
//    @Override
//    public void hit(Actor attacker, Actor defender, Hit hit) {
//        if (positions.stream().anyMatch(defender.getPosition()::equals)) {
//
//        }
//    }
//
//    @Override
//    public int getAttackDelay(Actor attacker, Actor defender, FightType fightType) {
//        return 0;
//    }
//
//    @Override
//    public int getAttackDistance(Actor attacker, FightType fightType) {
//        return 0;
//    }
//
//    @Override
//    public CombatHit[] getHits(Actor attacker, Actor defender) {
//        return new CombatHit[0];
//    }
//
//    @Override
//    public Animation getAttackAnimation(Actor attacker, Actor defender) {
//        return null;
//    }
//
//    @Override
//    public CombatType getCombatType() {
//        return null;
//    }
//
//}