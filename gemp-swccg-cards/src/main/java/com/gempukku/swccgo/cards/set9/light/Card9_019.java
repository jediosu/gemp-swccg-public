package com.gempukku.swccgo.cards.set9.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Death Star II
 * Type: Character
 * Subtype: Rebel
 * Title: Keir Santage
 */

public class Card9_019 extends AbstractRebel {
    public Card9_019(){
        super(Side.LIGHT, 3, 2, 2, 2, 4, Title.Keir_Santage, Uniqueness.UNIQUE);
        setLore("Rescued from an Imperial detention center by Wedge Antilles. Rogue Squadron veteran. Assigned to Red Squadron at the Battle of Endor. Coordinates with Rebellion procurement.");
        setGameText("Adds 2 to power of anything he pilots. When at a system, sector or docking bay, once during each of your deploy phases, may subtract 2 from deploy cost of your unique (•) X-wing deploying here.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT);
        addKeywords(Keyword.ROGUE_SQUADRON, Keyword.RED_SQUADRON);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalBeforeTriggers(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // unique not working - using just x-wing for now
        // TriggerConditions.isPlayingCard(game, effect, playerId, Filters.X_wing)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.isAtLocation(game, self, Filters.or(Filters.system, Filters.sector, Filters.docking_bay))){
            final PhysicalCard cardPlayed = ((RespondablePlayingCardEffect) effect).getCard();

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Subtract 2 from unique X-wing deploy cost deploying here");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new AddUntilEndOfCardPlayedModifierEffect(action, cardPlayed,
                            new DeployCostModifier(cardPlayed, -2),
                            "Subtracts 2 from deploy cost of " + GameUtils.getCardLink(cardPlayed)));
            return Collections.singletonList(action);
        }
        return null;
    }
}

