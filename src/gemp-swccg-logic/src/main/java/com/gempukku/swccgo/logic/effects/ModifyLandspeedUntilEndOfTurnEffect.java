package com.gempukku.swccgo.logic.effects;

import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.ActionsEnvironment;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.modifiers.LandspeedModifier;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersEnvironment;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.AbstractSuccessfulEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.ResetOrModifyCardAttributeResult;

/**
 * An effect to modify the landspeed of a card until end of the turn.
 */
public class ModifyLandspeedUntilEndOfTurnEffect extends AbstractSuccessfulEffect {
    private PhysicalCard _cardToModify;
    private float _modifierAmount;

    /**
     * Creates an effect that modifies the landspeed of a card until end of the turn.
     * @param action the action performing this effect
     * @param cardToModify the card whose landspeed is modified
     * @param modifierAmount the amount of the modifier
     */
    public ModifyLandspeedUntilEndOfTurnEffect(Action action, PhysicalCard cardToModify, float modifierAmount) {
        super(action);
        _cardToModify = cardToModify;
        _modifierAmount = modifierAmount;
    }

    @Override
    protected void doPlayEffect(SwccgGame game) {
        GameState gameState = game.getGameState();
        ModifiersQuerying modifiersQuerying = game.getModifiersQuerying();

        ActionsEnvironment actionsEnvironment = game.getActionsEnvironment();
        ModifiersEnvironment modifiersEnvironment = game.getModifiersEnvironment();
        PhysicalCard source = _action.getActionSource();

        String actionMsg;
        if (_modifierAmount < 0) {
            actionMsg = "reduces " + GameUtils.getCardLink(_cardToModify) + "'s landspeed by " + GuiUtils.formatAsString(-_modifierAmount) + " until end of the turn";
        }
        else {
            // Check if card's landspeed may not be increased
            if (modifiersQuerying.isProhibitedFromHavingLandspeedIncreased(gameState, _cardToModify)) {
                gameState.sendMessage(GameUtils.getCardLink(_cardToModify) + "'s landspeed is prevented from being increased");
                return;
            }
            else {
                actionMsg = "adds " + GuiUtils.formatAsString(_modifierAmount) + " to " + GameUtils.getCardLink(_cardToModify) + "'s landspeed until end of the turn";
            }
        }

        if (_action.getPerformingPlayer() == null)
            gameState.sendMessage(GameUtils.getCardLink(_action.getActionSource()) + " " + actionMsg);
        else
            gameState.sendMessage(_action.getPerformingPlayer() + " " + actionMsg + " using " + GameUtils.getCardLink(_action.getActionSource()));

        // Filter for same card while it is in play
        Filter cardFilter = Filters.and(Filters.sameCardId(_cardToModify), Filters.in_play);

        modifiersEnvironment.addUntilEndOfTurnModifier(
                new LandspeedModifier(source, cardFilter, _modifierAmount));

        actionsEnvironment.emitEffectResult(new ResetOrModifyCardAttributeResult(_action.getPerformingPlayer(), _cardToModify));
    }
}
