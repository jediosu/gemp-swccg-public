package com.gempukku.swccgo.logic.timing;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardCounts;
import com.gempukku.swccgo.common.GameEndReason;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.communication.GameStateListener;
import com.gempukku.swccgo.communication.InGameStatisticsListener;
import com.gempukku.swccgo.communication.UserFeedback;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.layout.LocationsLayout;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.DefaultActionsEnvironment;
import com.gempukku.swccgo.logic.PlayerOrder;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersEnvironment;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersLogic;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.vo.SwccgDeck;

import java.util.*;

/**
 * The implementation of an SwccgGame.
 */
public class DefaultSwccgGame implements SwccgGame {
    private GameState _gameState;
    private ModifiersLogic _modifiersLogic;
    private ActionsEnvironment _actionsEnvironment;
    private UserFeedback _userFeedback;
    private TurnProcedure _turnProcedure;

    private SwccgFormat _format;
    private boolean _useBonusAbilities;
    private Set<String> _allPlayers;
    private String _lightPlayerId;
    private String _darkPlayerId;
    private SwccgCardBlueprintLibrary _library;
    private Map<String, String> _decks;
    private Map<String, List<String>> _cards;
    private Map<String, List<String>> _outSideOfDeckCards;
    private String _winnerPlayerId;
    private Map<String, String> _losers = new HashMap<String, String>();
    private boolean _cancelled;
    private Set<String> _requestedCancel = new HashSet<String>();
    private Map<String, Integer> _requestedExtendGameTimer = new HashMap<String, Integer>();
    private Set<String> _requestedDisableActionTimer = new HashSet<String>();
    private boolean _finished;
    private Map<String, Integer> _playerClocks;

    private Map<String, Set<Phase>> _autoPassConfiguration = new HashMap<String, Set<Phase>>();
    private Set<GameStateListener> _gameStateListeners = new HashSet<GameStateListener>();
    private Set<GameResultListener> _gameResultListeners = new HashSet<GameResultListener>();
    private Set<InGameStatisticsListener> _inGameStatisticsListeners = new HashSet<InGameStatisticsListener>();

    private int _nextShapshotId;
    private GameSnapshot _snapshotToRestore;
    private List<GameSnapshot> _snapshots = new LinkedList<GameSnapshot>();
    private static final int NUM_PREV_TURN_SNAPSHOTS_TO_KEEPS = 1;

    private boolean _testEnv = false;

    /**
     * Creates a game.
     * @param format the format of the game
     * @param decks the decks
     * @param userFeedback the user feedback
     * @param library the library of all cards
     */
    public DefaultSwccgGame(SwccgFormat format, Map<String, SwccgDeck> decks, UserFeedback userFeedback, final SwccgCardBlueprintLibrary library, Map<String, Integer> playerClocks, boolean useBonusAbilities) {
        _format = format;
        _library = library;
        _allPlayers = decks.keySet();
        _playerClocks = playerClocks;
        _useBonusAbilities = useBonusAbilities;

        // Sets the "cards in deck" and "cards outside of deck" for each player
        _cards = new HashMap<String, List<String>>();
        _outSideOfDeckCards = new HashMap<String, List<String>>();
        _decks = new HashMap<String, String>();
        for (String playerId : _allPlayers) {
            List<String> deck = new LinkedList<String>();
            List<String> outsideOfDeck = new LinkedList<String>();

            SwccgDeck swccgDeck = decks.get(playerId);

            // if this is a playtesting game, add in any playtesting defensive shields that are not in the deck
            if ("Playtesting".equals(format.getName())) {
                loadPlaytestingShields(swccgDeck);
            }


            _decks.put(playerId, swccgDeck.toString());
            deck.addAll(swccgDeck.getCards());
            outsideOfDeck.addAll(swccgDeck.getCardsOutsideDeck());

            if (swccgDeck.getSide(library)==(Side.DARK))
                _darkPlayerId = playerId;
            else
                _lightPlayerId = playerId;

            _cards.put(playerId, deck);
            _outSideOfDeckCards.put(playerId, outsideOfDeck);
        }

        _gameState = new GameState(this);
        _modifiersLogic = new ModifiersLogic(this);
        _userFeedback = userFeedback;

        ActionStack actionStack = new ActionStack();
        _turnProcedure = new TurnProcedure(this, userFeedback, actionStack);
        _actionsEnvironment = new DefaultActionsEnvironment(this, actionStack);

        // Apply any additional rules
        RuleSet ruleSet = new RuleSet(_actionsEnvironment, _modifiersLogic);
        ruleSet.applyRuleSet(this);
    }

    @Override
    public SwccgFormat getFormat() {
        return _format;
    }

    @Override
    public void startGame() {
        if (!_cancelled) {
            // Initially set dark side to go first
            List<String> orderedPlayers = new LinkedList<String>();
            orderedPlayers.add(_darkPlayerId);
            orderedPlayers.add(_lightPlayerId);
            _gameState.init(new PlayerOrder(orderedPlayers), _darkPlayerId, _lightPlayerId, _cards, _outSideOfDeckCards, _library);

            // Generate locations layout
            _gameState.setLocationsLayout(new LocationsLayout(_library));

            // begin the game
            _turnProcedure.carryOutPendingActionsUntilDecisionNeeded();
        }
    }

    @Override
    public void carryOutPendingActionsUntilDecisionNeeded() {
        if (!_cancelled) {
           _turnProcedure.carryOutPendingActionsUntilDecisionNeeded();

            // Restore snapshot if it was requested
            while (_snapshotToRestore != null) {
                restoreSnapshot();
                carryOutPendingActionsUntilDecisionNeeded();
            }
        }
    }

    @Override
    public String getLightPlayer() {
        return _lightPlayerId;
    }

    @Override
    public String getDarkPlayer() {
        return _darkPlayerId;
    }

    @Override
    public String getPlayer(Side side) {
        if (side==Side.DARK)
            return _darkPlayerId;
        else
            return _lightPlayerId;
    }

    @Override
    public Side getSide(String playerId) {
        if (playerId.equals(_darkPlayerId))
            return Side.DARK;
        else
            return Side.LIGHT;
    }

    @Override
    public String getOpponent(String playerId) {
        if (playerId.equals(_darkPlayerId))
            return _lightPlayerId;
        else
            return _darkPlayerId;
    }

    @Override
    public PhysicalCard findCardByPermanentId(Integer permanentCardId) {
        return _gameState.findCardByPermanentId(permanentCardId);
    }

    @Override
    public String getWinner() {
        return _winnerPlayerId;
    }

    @Override
    public boolean isFinished() {
        return _finished;
    }

    @Override
    public void abortGame() {
        if (!_finished) {
            _cancelled = true;

            if (_gameState != null) {
                _gameState.sendMessage("Game was canceled due to an error. Please report this error.");
            }

            for (GameResultListener gameResultListener : _gameResultListeners)
                gameResultListener.gameCancelled();

            for(InGameStatisticsListener gameStatisticsListener: _inGameStatisticsListeners) {
                gameStatisticsListener.writePileCounts(this,true);
            }

            _finished = true;
        }
    }

    @Override
    public void performAutoCancelGame() {
        performCancelGameByRequest(true);
    }

    private void performCancelGameByRequest(boolean automatically) {
        if (!_finished) {
            _cancelled = true;

            if (_gameState != null) {
                if (automatically)
                    _gameState.sendMessage("Game was cancelled, since not all players performed at least one action.");
                else
                    _gameState.sendMessage("Game was cancelled, as requested by all parties.");
            }

            for (GameResultListener gameResultListener : _gameResultListeners)
                gameResultListener.gameCancelled();

            for(InGameStatisticsListener gameStatisticsListener: _inGameStatisticsListeners) {
                gameStatisticsListener.writePileCounts(this,true);
            }

            _finished = true;
        }
    }

    @Override
    public boolean isCancelled() {
        return _cancelled;
    }

    private void gameWon(String winner, String reason) {
        _winnerPlayerId = winner;

        if (_gameState != null)
            _gameState.sendMessage(_winnerPlayerId + " is the winner due to: " + reason);

        String winnerSide = (getSide(winner)==Side.DARK) ? "Dark" : "Light";
        String loserSide = (getSide(winner)==Side.DARK) ? "Light" : "Dark";

        for (GameResultListener gameResultListener : _gameResultListeners)
            gameResultListener.gameFinished(_winnerPlayerId, reason, _losers, winnerSide, loserSide);

        for(InGameStatisticsListener gameStatisticsListener: _inGameStatisticsListeners) {
            gameStatisticsListener.writePileCounts(this, true);
        }

        _finished = true;
    }

    @Override
    public void playerLost(String playerId, GameEndReason reason) {
        if (!_finished) {
            if (_losers.get(playerId) == null) {
                _losers.put(playerId, reason.getHumanReadable());
                if (_gameState != null) {
                    _gameState.sendMessage(playerId + " lost due to: " + reason.getHumanReadable());
                }
                gameWon(getOpponent(playerId), GameEndReason.getOpponentsReason(reason).getHumanReadable());
            }
        }
    }

    @Override
    public void requestCancel(String playerId) {
        _gameState.sendMessage(playerId + " has requested the game to be canceled. Game will be canceled if requested by all players");
        _requestedCancel.add(playerId);
        if (_requestedCancel.size() >= _allPlayers.size()) {
            performCancelGameByRequest(false);
        }
    }

    @Override
    public void requestExtendGameTimer(String playerId, int minutes) {
        _gameState.sendMessage(playerId + " has requested the game timer to be extended by " + minutes + " minutes. The game timer will be extended if requested by all players");
        Integer currentPlayerTimer = _requestedExtendGameTimer.get(playerId);
        if (currentPlayerTimer == null) {
            currentPlayerTimer = 0;
        }
        _requestedExtendGameTimer.put(playerId, currentPlayerTimer + minutes);
    }

    @Override
    public int getGameTimerExtendedInMinutes() {
        if (_requestedExtendGameTimer.size() >= _allPlayers.size()) {
            return Collections.min(_requestedExtendGameTimer.values());
        }
        return 0;
    }

    @Override
    public void requestDisableActionTimer(String playerId) {
        _gameState.sendMessage(playerId + " has requested the action timer to be disabled. The action timer will be disabled if requested by all players.");
        _requestedDisableActionTimer.add(playerId);
    }

    @Override
    public boolean isActionTimerDisabled() {
        return _requestedDisableActionTimer.size() >= _allPlayers.size();
    }


    @Override
    public GameState getGameState() {
        return _gameState;
    }

    @Override
    public GameStats getGameStats() {
        return _turnProcedure.getGameStats();
    }

    @Override
    public ActionsEnvironment getActionsEnvironment() {
        return _actionsEnvironment;
    }

    @Override
    public ModifiersEnvironment getModifiersEnvironment() {
        return _modifiersLogic;
    }

    @Override
    public ModifiersQuerying getModifiersQuerying() {
        return _modifiersLogic;
    }

    @Override
    public UserFeedback getUserFeedback() {
        return _userFeedback;
    }

    @Override
    public void checkLifeForceDepleted() {
        GameState gameState = getGameState();
        if (gameState != null && gameState.getCurrentPhase() != Phase.PLAY_STARTING_CARDS && gameState.getCurrentPhase() != Phase.BETWEEN_TURNS) {
            // No remaining life force
            // Check current player first
            if (gameState.getPlayerLifeForce(gameState.getCurrentPlayerId()) <= 0)
                playerLost(getGameState().getCurrentPlayerId(), GameEndReason.LOSS__FORCE_DEPLETED);
            else if (gameState.getCurrentPlayerId().equals(_lightPlayerId) && gameState.getPlayerLifeForce(_darkPlayerId) <= 0)
                playerLost(_darkPlayerId, GameEndReason.LOSS__FORCE_DEPLETED);
            else if (gameState.getCurrentPlayerId().equals(_darkPlayerId) && gameState.getPlayerLifeForce(_lightPlayerId) <= 0)
                playerLost(_lightPlayerId, GameEndReason.LOSS__FORCE_DEPLETED);
        }
    }

    @Override
    public boolean shouldAutoPass(String playerId, Phase phase) {
        final Set<Phase> passablePhases = _autoPassConfiguration.get(playerId);
        return passablePhases != null && passablePhases.contains(phase);
    }


    @Override
    public void setPlayerAutoPassSettings(String playerId, Set<Phase> phases) {
        _autoPassConfiguration.put(playerId, phases);
    }

    @Override
    public void addGameResultListener(GameResultListener listener) {
        _gameResultListeners.add(listener);
    }

    @Override
    public void removeGameResultListener(GameResultListener listener) {
        _gameResultListeners.remove(listener);
    }

    @Override
    public void addGameStateListener(String playerId, GameStateListener gameStateListener) {
        _gameStateListeners.add(gameStateListener);
        _gameState.sendStateToPlayer(playerId, gameStateListener, getGameStats(), false);
    }

    @Override
    public void removeGameStateListener(GameStateListener gameStateListener) {
        _gameStateListeners.remove(gameStateListener);
    }

    @Override
    public Collection<GameStateListener> getAllGameStateListeners() {
        return Collections.unmodifiableSet(_gameStateListeners);
    }

    @Override
    public void requestRevert(final String playerId, Runnable onAbort) {
        this.requestRevert(playerId, onAbort, onAbort);
    }

    @Override
    public void requestRevert(final String playerId, Runnable onInvalid, Runnable onRejected) {
        final List<Integer> snapshotIds = new ArrayList<Integer>();
        final List<String> snapshotDescriptions = new ArrayList<String>();
        for (GameSnapshot gameSnapshot : getSnapshots()) {
            snapshotIds.add(gameSnapshot.getId());
            snapshotDescriptions.add(gameSnapshot.getDescription());
        }
        int numSnapshots = snapshotDescriptions.size();
        if (numSnapshots == 0) {
            onInvalid.run();
            return;
        }
        snapshotIds.add(-1);
        snapshotDescriptions.add("Do not revert");

        // Ask player to choose snapshot to revert back to
        getUserFeedback().sendAwaitingDecision(playerId,
                new MultipleChoiceAwaitingDecision("Choose game state to revert prior to", snapshotDescriptions.toArray(new String[0]), snapshotDescriptions.size() - 1) {
                    @Override
                    public void validDecisionMade(int index, String result) {
                        final int snapshotIdChosen = snapshotIds.get(index);
                        if (snapshotIdChosen == -1) {
                            onRejected.run();
                            return;
                        }

                        getGameState().sendMessage(playerId + " attempts to revert game to a previous state");

                        // Confirm with the other player if it is acceptable to revert to the game state
                        final String opponent = getOpponent(playerId);
                        StringBuilder snapshotDescMsg = new StringBuilder("</br>");
                        for (int i=0; i<snapshotDescriptions.size() - 1; ++i) {
                            if (i == index) {
                                snapshotDescMsg.append("</br>").append(">>> Revert to here <<<");
                            }
                            if ((index - i) < 3) {
                                snapshotDescMsg.append("</br>").append(snapshotDescriptions.get(i));
                            }
                        }
                        snapshotDescMsg.append("</br>");

                        getUserFeedback().sendAwaitingDecision(opponent,
                                new YesNoDecision("Do you want to allow game to be reverted to the following game state?" + snapshotDescMsg) {
                                    @Override
                                    protected void yes() {
                                        getGameState().sendMessage(opponent + " allows game to revert to a previous state");
                                        requestRestoreSnapshot(snapshotIdChosen);
                                    }
                                    @Override
                                    protected void no() {
                                        getGameState().sendMessage(opponent + " denies attempt to revert game to a previous state");
                                        onRejected.run();
                                    }
                                });
                    }
                });
    }

    /**
     * Gets the game snapshots.
     * @return the game snapshots
     */
    @Override
    public List<GameSnapshot> getSnapshots() {
        return Collections.unmodifiableList(_snapshots);
    }

    /**
     * Requests a game snapshot to be restored as current state of the game.
     * @param snapshotId the snapshot ID
     */
    @Override
    public void requestRestoreSnapshot(int snapshotId) {
        if (_snapshotToRestore == null) {
            for (Iterator<GameSnapshot> iterator = _snapshots.iterator(); iterator.hasNext();) {
                GameSnapshot gameSnapshot = iterator.next();
                if (gameSnapshot.getId() == snapshotId) {
                    _snapshotToRestore = gameSnapshot;
                }
                // After snapshot to restore is found, remove any snapshots after it from list
                if (_snapshotToRestore != null) {
                    // Remove the current snapshot from the iterator and the list.
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Determines if a snapshot is pending to be restored.
     * @return true or false
     */
    @Override
    public boolean isRestoreSnapshotPending() {
        return _snapshotToRestore != null;
    }

    /**
     * Restores the snapshot as the current state of the game.
     */
    private void restoreSnapshot() {
        if (_snapshotToRestore != null) {
            _gameState = _snapshotToRestore.getGameState();
            _modifiersLogic = _snapshotToRestore.getModifiersLogic();
            _actionsEnvironment = _snapshotToRestore.getActionsEnvironment();
            _turnProcedure = _snapshotToRestore.getTurnProcedure();
            _gameState.sendMessage("Reverted to previous game state");
            _snapshotToRestore = null;

            // Send game state to all listeners
            for (GameStateListener gameStateListener : _gameStateListeners) {
                _gameState.sendStateToPlayer(gameStateListener.getPlayerId(), gameStateListener, getGameStats(), true);
            }
        }
    }

    /**
     * Gets the next ID to use for a snapshot.
     * @return the ID
     */
    private int getNextSnapshotId() {
        return ++_nextShapshotId;
    }

    /**
     * Creates a snapshot of the current state of the game.
     * @param description the description
     */
    @Override
    public void takeSnapshot(String description) {
        pruneSnapshots();
        // need to specifically exclude when getPlayCardStates() is not empty to allow for battles to be initiated by interrupts
        if (_gameState.getPlayCardStates().isEmpty())
            _snapshots.add(GameSnapshot.createGameSnapshot(getNextSnapshotId(), description, _gameState, _modifiersLogic, _actionsEnvironment, _turnProcedure));
    }

    /**
     * Prunes older snapshots.
     */
    private void pruneSnapshots() {
        // Remove old snapshots until reaching snapshots to keep
        for (Iterator<GameSnapshot> iterator = _snapshots.iterator(); iterator.hasNext();) {
            GameSnapshot gameSnapshot = iterator.next();
            String snapshotCurrentPlayer = gameSnapshot.getCurrentPlayerId();
            int snapshotCurrentTurnNumber = gameSnapshot.getCurrentTurnNumber();
            if (snapshotCurrentTurnNumber <= 1 && _gameState.getPlayersLatestTurnNumber(snapshotCurrentPlayer) <= 1) {
                break;
            }
            int pruneOlderThanTurn = _gameState.getPlayersLatestTurnNumber(snapshotCurrentPlayer) - (NUM_PREV_TURN_SNAPSHOTS_TO_KEEPS / 2);
            if (snapshotCurrentTurnNumber >= pruneOlderThanTurn) {
                break;
            }
            // Remove the current snapshot from the iterator and the list.
            iterator.remove();
        }
    }

    public String getDeckString(Side side) {
        if(side.equals(Side.DARK)) {
            return _decks.get(_darkPlayerId);
        } else if(side.equals(Side.LIGHT)) {
            return _decks.get(_lightPlayerId);
        }

        return "";
    }

    @Override
    public void addInGameStatisticsListener(InGameStatisticsListener listener) {
        _inGameStatisticsListeners.add(listener);
    }

    @Override
    public void removeInGameStatisticsListener(InGameStatisticsListener listener) {
        _inGameStatisticsListeners.remove(listener);
    }

    @Override
    public Collection<InGameStatisticsListener> getAllInGameStatisticsListeners() {
        return Collections.unmodifiableSet(_inGameStatisticsListeners);
    }

    @Override
    public void removeAllInGameStatisticsListeners() {
        _inGameStatisticsListeners.clear();
    }

    public Integer getSecondsElapsed(String player) {
        return _playerClocks.get(player);
    }

    public boolean useBonusAbilities() {
        return _useBonusAbilities;
    }

    private void loadPlaytestingShields(SwccgDeck swccgDeck) {
        List<String> playtestingShields = findPlaytestingDefensiveShields(_library, CardCounts.PLAYTESTING_SETS_CARD_COUNTS, 501, swccgDeck.getSide(_library));
        for(String shield:playtestingShields) {
            if (!swccgDeck.getCardsOutsideDeck().contains(shield)) {
                swccgDeck.addCardOutsideDeck(shield);
            }
        }
    }

    private List<String> findPlaytestingDefensiveShields(SwccgCardBlueprintLibrary library, int[] cardSetCounts, int setIndexOffset, Side side) {
        List<String> defensiveShields = new LinkedList<>();
        for (int i = 0; i < cardSetCounts.length; i++) {
            int setNum = setIndexOffset + i;
            for (int j = 1; j <= cardSetCounts[i]; j++) {
                String blueprintId = setNum + "_" + j;
                try {
                    if (library.getBaseBlueprintId(blueprintId).equals(blueprintId)) {
                        SwccgCardBlueprint cardBlueprint = library.getSwccgoCardBlueprint(blueprintId);
                        if (cardBlueprint != null
                                && !cardBlueprint.excludeFromDeckBuilder()
                                && cardBlueprint.getSide().equals(side)) {
                            CardCategory cardCategory = cardBlueprint.getCardCategory();
                            if (cardCategory == CardCategory.DEFENSIVE_SHIELD) {
                                defensiveShields.add(blueprintId);
                            }
                        }
                    }
                } catch (IllegalArgumentException exp) {
                }
            }
        }

        return defensiveShields;
    }

    public void setTestEnvironment(boolean value) { _testEnv = value; }

    @Override
    public boolean isTestEnvironment() { return _testEnv; }
}
