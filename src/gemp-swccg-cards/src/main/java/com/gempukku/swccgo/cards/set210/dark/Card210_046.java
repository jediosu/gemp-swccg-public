package com.gempukku.swccgo.cards.set210.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.MoveCardUsingLandspeedEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.MovedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 10
 * Type: Character
 * Subtype: Imperial
 * Title: The Grand Inquisitor
 */
public class Card210_046 extends AbstractImperial {
    public Card210_046() {
        super(Side.DARK, 1, 5, 5, 5, 7, "The Grand Inquisitor", Uniqueness.UNIQUE, ExpansionSet.SET_10, Rarity.V);
        setLore("Leader.");
        setGameText("Adds 2 to power of anything he pilots. If a Jedi or Padawan just lost from same site as any Inquisitor, opponent loses 1 Force. If a Jedi or Padawan just moved from here, Inquisitors present may follow that character (using landspeed). Immune to attrition < 4.");
        addPersona(Persona.THE_GRAND_INQUISITOR);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_10);
        addKeywords(Keyword.INQUISITOR, Keyword.LEADER);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 4));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        //  If a Jedi or Padawan just lost from same site as any Inquisitor, opponent loses 1 Force.
        Filter jediOrPadawan = Filters.or(Filters.Jedi, Filters.padawan);
        Filter sameSiteAsYourInquisitor = Filters.sameSiteAs(self, Filters.and(Keyword.INQUISITOR));

        // Check condition(s)
        if (TriggerConditions.justLostFromLocation(game, effectResult, jediOrPadawan, sameSiteAsYourInquisitor)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);
            action.setText("Make opponent lose 1 Force");

            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return actions;
    }



    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        // If a Jedi or Padawan just moved from here, Inquisitors present may follow that character (using landspeed)
        Filter jediOrPadawan = Filters.or(Filters.Jedi, Filters.padawan);

        // Check condition(s)
        if (TriggerConditions.movedFromOrThroughLocationToLocation(game, effectResult, jediOrPadawan, Filters.here(self), Filters.location)) {
            MovedResult movedResult = (MovedResult) effectResult;
            if (movedResult.isMoveComplete()) {
                final Filter toLocation = Filters.sameLocation(movedResult.getMovedTo());
                Filter movableFilter = Filters.and(Filters.your(self), Filters.inquisitor, Filters.present(self),
                        Filters.movableAsRegularMoveUsingLandspeed(playerId, false, false, false, 0, null, toLocation));
                if (GameConditions.canSpot(game, self, movableFilter)) {

                    final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                    action.setRepeatableTrigger(true);
                    action.setText("Have Inquisitor follow character");
                    // Choose target(s)
                    action.appendTargeting(
                            new ChooseCardOnTableEffect(action, playerId, "Choose Inquisitor", movableFilter) {
                                @Override
                                protected void cardSelected(final PhysicalCard character) {
                                    action.addAnimationGroup(character);
                                    action.setActionMsg("Have " + GameUtils.getCardLink(character) + " move using landspeed to follow character");
                                    // Perform result(s)
                                    action.appendEffect(
                                            new MoveCardUsingLandspeedEffect(action, playerId, character, false, toLocation));
                                }
                            }
                    );
                    return Collections.singletonList(action);
                }
            }
        }
        return null;
    }

}
