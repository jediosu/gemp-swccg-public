package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Actions are top-level decisions that involve legal game operations available to players.  For example, deploying
 * a card, playing an interrupt, and activating a card ability are all Actions.
 */
public interface Actions extends Decisions, Choices {

	/**
	 * @return Gets the text descriptions of all current actions available to the Dark Side player. Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> GetDSAvailableActions() { return GetAvailableActions(DS); }
	/**
	 * @return Gets the text descriptions of all current actions available to the Light Side player. Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> GetLSAvailableActions() { return GetAvailableActions(LS); }
	/**
	 * @param playerID The player with a current decision
	 * @return Gets the text descriptions of all current actions available to the given player.  Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> GetAvailableActions(String playerID) {
		AwaitingDecision decision = GetAwaitingDecision(playerID);
		if(decision == null) {
			return new ArrayList<>();
		}
		return Arrays.asList(decision.getDecisionParameters().get("actionText"));
	}

	/**
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * Dark Side player has no pending decisions.
	 */
	default boolean DSAnyActionsAvailable() { return AnyActionsAvailable(DS); }

	/**
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * Light Side player has no pending decisions.
	 */
	default boolean LSAnyActionsAvailable() { return AnyActionsAvailable(LS); }

	/**
	 * Returns whether the given player has any action at all available as part of the currently pending decision.
	 * @param player The player to check for.
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * current player has no pending decisions.
	 */
	default boolean AnyActionsAvailable(String player) {
		List<String> actions = GetAvailableActions(player);
		return !actions.isEmpty();
	}

	/**
	 * Checks whether the Dark Side player has any action available containing the provided text.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean DSActionAvailable(String text) { return ActionAvailable(DS, null, text); }
	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean DSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, card, null); }
	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 *             @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean DSActionAvailable(PhysicalCardImpl card, String text) { return ActionAvailable(DS, card, text); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean LSActionAvailable(String text) { return ActionAvailable(LS, null, text); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean LSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, card, null); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default boolean LSActionAvailable(PhysicalCardImpl card, String text) { return ActionAvailable(LS, card, text); }
	/**
	 * Checks whether the given player has an action available containing the provided text.
	 * @param playerId The player to check for.
	 * @param card The card ID to search for.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching card and/or text, otherwise false.
	 */
	default boolean ActionAvailable(String playerId, PhysicalCardImpl card, String text) {
		return GetCardActionId(playerId, card, text) != null;
	}

	/**
	 * Causes the Dark Side player to choose the given action whose description contains the given search text.
	 * @param option The text to search for.
	 * @throws DecisionResultInvalidException
	 */
	default void DSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(DS, "actionText", option); }
	/**
	 * Causes the Light Side player to choose the given action whose description contains the given search text.
	 * @param option The text to search for.
	 * @throws DecisionResultInvalidException
	 */
	default void LSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(LS, "actionText", option); }



	/**
	 * Checks whether any action on the given card can be performed by the Dark Side player.  This is a catch-all
	 * that will catch any kind of action for that card--deploy, transfer, play, activate, etc.
	 * @param card The card being searched for.
	 * @return True if there is an available action for that card, false otherwise.
	 */
	default boolean DSCardActionAvailable(PhysicalCardImpl card) { return DSActionAvailable(card); }
	/**
	 * Checks whether any action on the given card can be performed by the Light Side player.  This is a catch-all
	 * that will catch any kind of action for that card--deploy, transfer, play, activate, etc.
	 * @param card The card being searched for.
	 * @return True if there is an available action for that card, false otherwise.
	 */
	default boolean LSCardActionAvailable(PhysicalCardImpl card) { return LSActionAvailable(card); }

	/**
	 * Causes the Dark Side player to execute an available action on the given card.
	 * @param card The card which is being used (played, deployed, activated, etc).
	 * @throws DecisionResultInvalidException Thrown if there is no legal action using that card (i.e. it would not be
	 * highlighted in Gemp).
	 */
	default void DSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card)); }
	/**
	 * Causes the Light Side player to execute an available action on the given card.
	 * @param card The card which is being used (played, deployed, activated, etc).
	 * @throws DecisionResultInvalidException Thrown if there is no legal action using that card (i.e. it would not be
	 * highlighted in Gemp).
	 */
	default void LSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card)); }


	/**
	 * Checks whether the given card can be played by the Dark Side player.  Technically this is a catch-all function
	 * that only looks for any action associated with the given card, but the use of this function communicates that
	 * the tester intended to check for a play action from hand.
	 * @param card The card being searched for.
	 * @return True if there is an available play action for that card, false otherwise.
	 */
	default boolean DSCardPlayAvailable(PhysicalCardImpl card) { return DSActionAvailable(card); }
	/**
	 * Checks whether the given card can be played by the Light Side player.  Technically this is a catch-all function
	 * that only looks for any action associated with the given card, but the use of this function communicates that
	 * the tester intended to check for a play action from hand.
	 * @param card The card being searched for.
	 * @return True if there is an available play action for that card, false otherwise.
	 */
	default boolean LSCardPlayAvailable(PhysicalCardImpl card) { return LSActionAvailable(card); }

	/**
	 * Causes the Dark Side player to select the given card and execute its legal action (i.e. plays that card from hand).
	 * Technically this is a catch-all that will activate any action on this card, but the use of this function
	 * communicates that the tester intended to play it from hand.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if the card does not have any available action.
	 */
	default void DSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card)); }
	/**
	 * Causes the Light Side player to select the given card and execute its legal action (i.e. plays that card from hand).
	 * Technically this is a catch-all that will activate any action on this card, but the use of this function
	 * communicates that the tester intended to play it from hand.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if the card does not have any available action.
	 */
	default void LSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card)); }



	/**
	 * Checks whether the given Lost Interrupt can be played by the Dark Side player.  This may only be useful for
	 * cards which have both a USED and LOST operation and may fail if checking against vanilla LOST Interrupts.
	 * @param card The interrupt being searched for.
	 * @return True if there is an available LOST play action for that card, false otherwise.
	 */
	default boolean DSPlayLostInterruptAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "LOST: "); }
	/**
	 * Checks whether the given Lost Interrupt can be played by the Light Side player.  This may only be useful for
	 * cards which have both a USED and LOST operation and may fail if checking against vanilla LOST Interrupts.
	 * @param card The interrupt being searched for.
	 * @return True if there is an available LOST play action for that card, false otherwise.
	 */
	default boolean LSPlayLostInterruptAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "LOST: "); }
	/**
	 * Checks whether the given Used Interrupt can be played by the Dark Side player.  This may only be useful for
	 * cards which have both a USED and LOST operation and may fail if checking against vanilla USED Interrupts.
	 * @param card The interrupt being searched for.
	 * @return True if there is an available USED play action for that card, false otherwise.
	 */
	default boolean DSPlayUsedInterruptAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "USED: "); }
	/**
	 * Checks whether the given Used Interrupt can be played by the Light Side player.  This may only be useful for
	 * cards which have both a USED and LOST operation and may fail if checking against vanilla USED Interrupts.
	 * @param card The interrupt being searched for.
	 * @return True if there is an available USED play action for that card, false otherwise.
	 */
	default boolean LSPlayUsedInterruptAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "USED: "); }


	/**
	 * Causes the Dark Side player to select the given Lost Interrupt and execute its legal action (i.e. plays that
	 * card from hand). This may only be useful for cards which have both a USED and LOST operation and may fail if
	 * using it for vanilla LOST Interrupts.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if a matching LOST play action is not found for
	 * this card.
	 */
	default void DSPlayLostInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "LOST: ")); }
	/**
	 * Causes the Light Side player to select the given Lost Interrupt and execute its legal action (i.e. plays that
	 * card from hand). This may only be useful for cards which have both a USED and LOST operation and may fail if
	 * using it for vanilla LOST Interrupts.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if a matching LOST play action is not found for
	 * this card.
	 */
	default void LSPlayLostInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "LOST: ")); }

	/**
	 * Causes the Dark Side player to select the given Used Interrupt and execute its legal action (i.e. plays that
	 * card from hand). This may only be useful for cards which have both a USED and LOST operation and may fail if
	 * using it for vanilla USED Interrupts.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if a matching USED play action is not found for
	 * this card.
	 */
	default void DSPlayUsedInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "USED: ")); }
	/**
	 * Causes the Light Side player to select the given Used Interrupt and execute its legal action (i.e. plays that
	 * card from hand). This may only be useful for cards which have both a USED and LOST operation and may fail if
	 * using it for vanilla USED Interrupts.
	 * @param card The card to play.
	 * @throws DecisionResultInvalidException This error will be thrown if a matching USED play action is not found for
	 * this card.
	 */
	default void LSPlayUsedInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "USED: ")); }


	/**
	 * Checks whether the given card can be deployed by the Dark Side player.
	 * @param card The card being searched for.
	 * @return True if there is an available Deploy action for that card, false otherwise.
	 */
	default boolean DSDeployAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "Deploy"); }
	/**
	 * Checks whether the given card can be deployed by the Light Side player.
	 * @param card The card being searched for.
	 * @return True if there is an available Deploy action for that card, false otherwise.
	 */
	default boolean LSDeployAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "Deploy"); }

	/**
	 * Causes the Dark Side player to perform  a legal deployment action of the given card (i.e. plays that card from hand).
	 * @param card The card to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void DSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Deploy")); }
	/**
	 * Causes the Light Side player to perform  a legal deployment action of the given card (i.e. plays that card from hand).
	 * @param card The card to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void LSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Deploy")); }

	/**
	 * Causes the Dark Side player to perform  a legal deployment action of the given location (i.e. plays that card
	 * from hand).  The site will be placed on the left automatically if necessary
	 * @param site The site to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void DSDeployLocation(PhysicalCardImpl site) throws DecisionResultInvalidException {
		DSDecided(GetCardActionId(DS, site, "Deploy"));
		if(DSDecisionAvailable("On which side")) {
			DSChoose("Left");
		}
	}
	/**
	 * Causes the Light Side player to perform  a legal deployment action of the given location (i.e. plays that card
	 * from hand).  The site will be placed on the left automatically if necessary
	 * @param site The site to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void LSDeployLocation(PhysicalCardImpl site) throws DecisionResultInvalidException {
		LSDecided(GetCardActionId(LS, site, "Deploy"));
		if(LSDecisionAvailable("On which side")) {
			LSChoose("Left");
		}
	}


	/**
	 * Checks whether the given card can be transferred by the Dark Side player.
	 * @param card The card being searched for.
	 * @return True if there is an available Transfer action for that card, false otherwise.
	 */
	default boolean DSTransferAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "Transfer"); }
	/**
	 * Checks whether the given card can be transferred by the Light Side player.
	 * @param card The card being searched for.
	 * @return True if there is an available Transfer action for that card, false otherwise.
	 */
	default boolean LSTransferAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "Transfer"); }

	/**
	 * Causes the Dark Side player to initiate a Transfer action on the given card.  Follow-up decisions will need to be
	 * made regarding the target.
	 * @param card The card to transfer.
	 * @throws DecisionResultInvalidException Thrown if the given card does not have a legal transfer action to perform.
	 */
	default void DSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Transfer")); }
	/**
	 * Causes the Light Side player to initiate a Transfer action on the given card.  Follow-up decisions will need to be
	 * made regarding the target.
	 * @param card The card to transfer.
	 * @throws DecisionResultInvalidException Thrown if the given card does not have a legal transfer action to perform.
	 */
	default void LSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Transfer ")); }




	/**
	 * Checks whether the Dark Side player has a legal Force Drain action available to make at the given site.
	 * @param site The site to check for a legal Force Drain action.
	 * @return True if a Force Drain can be performed at that site, false otherwise.
	 */
	default boolean DSForceDrainAvailable(PhysicalCardImpl site) { return DSActionAvailable(site, "Force drain"); }
	/**
	 * Checks whether the Light Side player has a legal Force Drain action available to make at the given site.
	 * @param site The site to check for a legal Force Drain action.
	 * @return True if a Force Drain can be performed at that site, false otherwise.
	 */
	default boolean LSForceDrainAvailable(PhysicalCardImpl site) { return LSActionAvailable(site, "Force drain"); }

	/**
	 * Causes the Dark Side player to initiate a legal Force Drain action at the given site.
	 * @param site The location to initiate the Force Drain at.
	 * @throws DecisionResultInvalidException Thrown if there is no legal Force Drain action to perform.
	 */
	default void DSForceDrainAt(PhysicalCardImpl site) throws DecisionResultInvalidException {
		DSDecided(GetCardActionId(DS, site, "Force drain"));
	}

	/**
	 * Causes the Dark Side player to initiate a legal Force Drain action at the given site.
	 * @param site The location to initiate the Force Drain at.
	 * @throws DecisionResultInvalidException Thrown if there is no legal Force Drain action to perform.
	 */
	default void LSForceDrainAt(PhysicalCardImpl site) throws DecisionResultInvalidException {
		LSDecided(GetCardActionId(LS, site, "Force drain"));
	}


	/**
	 * Searches the currently available actions on the current decision for the given player and returns the ID of an
	 * action which contains the provided text in its description.  This is a lower-level function used by the test rig
	 * which is unlikely to be needed for tests themselves.
	 * @param playerId The player which must have a currently active decision.
	 * @param text Constrains the result to only actions whose description contains the provided text
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, String text) { return GetCardActionId(playerId, null, text); }
	/**
	 * Searches the currently available actions on the current decision for the given player and returns the ID of an
	 * action which was sourced by the provided card's ID.  This is a lower-level function used by the test rig
	 * which is unlikely to be needed for tests themselves.
	 * @param playerId The player which must have a currently active decision.
	 * @param card Constrains the result to only actions which are source from this card.
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, PhysicalCardImpl card) { return GetCardActionId(playerId, card, null); }

	/**
	 * Searches the currently available actions on the current decision for the given player.  If card is provided, the
	 * card's ID must be the source of one of the given actions.  If text is provided, the action description must match
	 * the given text.  If both are provided, both are checked. This is a lower-level function used by the test rig
	 * which is unlikely to be needed for tests themselves.
	 * @param playerId The player which must have a currently active decision.
	 * @param card If provided, constrains the result to only actions which are source from this card.
	 * @param text If provided, constrains the result to only actions whose description contains the provided text
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, PhysicalCardImpl card, String text) {
		String id = card != null ? String.valueOf(card.getCardId()) : null;
		String[] cardIds = GetADParam(playerId, "cardId");
		String[] actionTexts = GetADParam(playerId, "actionText");

		for (int i = 0; i < cardIds.length; i++) {
			if ((id == null || cardIds[i].equals(id)) && (text == null || actionTexts[i].contains(text))) {
				return GetADParam(playerId, "actionId")[i];
			}
		}
		return null;
	}

}
