package com.gempukku.swccgo.cards.set210.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.UtinniEffectCompletedCondition;
import com.gempukku.swccgo.cards.effects.CancelCardResultEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardActionReason;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 10
 * Type: Character
 * Subtype: Alien
 * Title: BoShek, Gritty Smuggler
 */
public class Card210_006 extends AbstractAlien {
    public Card210_006() {
        super(Side.LIGHT, 1, 3, 3, 4, 5, "BoShek, Gritty Smuggler", Uniqueness.UNIQUE, ExpansionSet.SET_10, Rarity.V);
        setLore("Rogue pilot. Outlaw starship tech. Has secret lab in Mos Eisley. He bragged about beating Han Solo's Kessel Run record. Left fringe life behind after meeting Obi-Wan Kenobi.");
        setGameText("[Pilot] 3. If you have completed a Kessel Run, your total power here is +2. Once during battle, may use 1 Force to choose: Subtract 1 from an opponent's just drawn destiny or cancel an attempt to cancel and redraw a destiny. Immune to attrition < 3.");
        addIcons(Icon.PILOT, Icon.VIRTUAL_SET_10);
        addKeywords(Keyword.SMUGGLER);
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        UtinniEffectCompletedCondition KesselRunCompleted = new UtinniEffectCompletedCondition(self.getOwner(), Filters.Kessel_Run);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalPowerModifier(self, Filters.here(self), KesselRunCompleted, 2, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.BOSHEK_VIRTUAL_GAMETEXT_ONCE_PER_BATTLE;

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawnBy(game, effectResult, game.getOpponent(playerId))
                && GameConditions.isInBattle(game, self)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canUseForce(game, playerId, 1)) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Subtract 1 from destiny");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Perform result(s)
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            action.appendEffect(
                    new ModifyDestinyEffect(action, -1));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();
        GameTextActionId gameTextActionId = GameTextActionId.BOSHEK_VIRTUAL_GAMETEXT_ONCE_PER_BATTLE;
        final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);

        // if the cancellation is interrupt based
        if (TriggerConditions.isPlayingCardForReason(game, effect, Filters.any, PlayCardActionReason.ATTEMPTING_TO_CANCEL_AND_REDRAW_A_DESTINY)
                && GameConditions.isInBattle(game, self)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canUseForce(game, playerId, 1)) {
            action.setText("Cancel an attempt to cancel and redraw a destiny");
            action.appendUsage(new OncePerBattleEffect(action));
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            action.appendEffect(
                    new CancelCardResultEffect(action, effect));
            actions.add(action);
        }

        // if the cancellation is from a card on table.
        if (TriggerConditions.isPerformingGameTextActionType(game, effect, Filters.any, GameTextActionId.ANY_CARD__CANCEL_AND_REDRAW_A_DESTINY)
                && GameConditions.isInBattle(game, self)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canUseForce(game, playerId, 1)) {
            action.setText("Cancel attempt to cancel and redraw a destiny");
            action.appendUsage(new OncePerBattleEffect(action));
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            action.appendEffect(
                    new CancelCardResultEffect(action, effect));
            actions.add(action);
        }

        return actions;
    }
}