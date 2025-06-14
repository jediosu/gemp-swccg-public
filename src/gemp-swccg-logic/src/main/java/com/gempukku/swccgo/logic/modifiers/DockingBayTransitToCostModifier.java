package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;


/**
 * A modifier that defines the docking bay transit cost to the specified docking bay, which may be in addition to the
 * docking bay transit cost from another docking bay.
 * This is used when the docking bay transit to cost of a docking bay is defined by game text.
 */
public class DockingBayTransitToCostModifier extends AbstractModifier {
    private Evaluator _evaluator;

    /**
     * Creates a modifier that defines the docking bay transit cost to the specified docking bay for the specified player.
     * @param source the source of the modifier
     * @param cost the docking bay transit to cost
     * @param playerId the player
     */
    public DockingBayTransitToCostModifier(PhysicalCard source, int cost, String playerId) {
        this(source, source, null, new ConstantEvaluator(cost), playerId);
    }

    /**
     * Creates a modifier that defines the docking bay transit cost to the specified docking bay for the specified player.
     *
     * @param source    the source of the modifier
     * @param evaluator the docking bay transit to evaluator
     * @param playerId  the player
     */
    public DockingBayTransitToCostModifier(PhysicalCard source, Evaluator evaluator, String playerId) {
        this(source, source, null, evaluator, playerId);
    }

    /**
     * Creates a modifier that defines the docking bay transit cost to the specified docking bay for the specified player.
     *
     * @param source    the source of the modifier
     * @param condition the condition that must be fulfilled for the modifier to be in effect
     * @param evaluator the docking bay transit to evaluator
     * @param playerId  the player
     */
    public DockingBayTransitToCostModifier(PhysicalCard source, Condition condition, Evaluator evaluator, String playerId) {
        this(source, source, condition, evaluator, playerId);
    }

    /**
     * Creates a modifier that defines the docking bay transit cost to docking bays accepted by the filter for the specified player.
     * @param source the source of the modifier
     * @param affectFilter the filter
     * @param condition the condition that must be fulfilled for the modifier to be in effect
     * @param evaluator the evaluator that calculates the amount of the modifier
     * @param playerId the player
     */
    private DockingBayTransitToCostModifier(PhysicalCard source, Filterable affectFilter, Condition condition, Evaluator evaluator, String playerId) {
        super(source, null, Filters.and(Filters.docking_bay, affectFilter), condition, ModifierType.DOCKING_BAY_TRANSIT_TO_COST, true);
        _evaluator = evaluator;
        _playerId = playerId;
    }

    @Override
    public float getValue(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard card) {
        return _evaluator.evaluateExpression(gameState, modifiersQuerying, card);
    }
}
