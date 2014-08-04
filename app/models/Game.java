package models;

import com.fasterxml.jackson.databind.JsonNode;
import models.event.BoardState;
import models.event.GameOver;
import models.event.IllegalMove;
import models.event.ReadyToStart;
import models.player.ConnectedPlayer;
import models.player.PairedPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F;
import play.mvc.WebSocket;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;

/**
 * Notifies players of the game start and performs coordination of the moves between peers.
 *
 * The caller is expected to call {@link Game#start} to notify peers of the
 * game start and install {@link WebSocket} message handlers. Next, each peer
 * move is directed to {@link Game#onMove(String, com.fasterxml.jackson.databind.JsonNode)},
 * where the move is validated, board state is updated, game completion is checked,
 * and finally the board state update is sent back to the peers.
 *
 * Client-server messaging is performed in JSON messages described by
 * {@link models.event.Event} classes.
 *
 * @see models.event.Event
 */
@ThreadSafe
public class Game {

    private final static Logger log = LoggerFactory.getLogger(Game.class);
    protected final String id = UUID.randomUUID().toString();
    protected final Map<String, PairedPlayer> players;
    protected final ShutdownListener shutdownListener;
    @GuardedBy("this") protected String nextPlayerId;
    @GuardedBy("this") protected boolean started;

    public interface ShutdownListener {
        public void onGameShutdown(String gameId);
    }

    public Game(
            ConnectedPlayer upperPlayer,
            ConnectedPlayer lowerPlayer,
            ShutdownListener shutdownListener) {
        // Initialize players.
        PairedPlayer upperPairedPlayer = upperPlayer.upgrade(lowerPlayer.getId());
        PairedPlayer lowerPairedPlayer = lowerPlayer.upgrade(upperPlayer.getId());
        Map<String, PairedPlayer> players = new HashMap<>();
        players.put(upperPairedPlayer.getId(), upperPairedPlayer);
        players.put(lowerPairedPlayer.getId(), lowerPairedPlayer);
        this.players = Collections.unmodifiableMap(players);

        // Set shutdown listener.
        this.shutdownListener = shutdownListener;

        // Initialize the next player id.
        this.nextPlayerId = upperPairedPlayer.getId();

        // Turn started flag off.
        this.started = false;
    }

    public String getId() { return id; }

    public Collection<PairedPlayer> getPlayers() { return players.values(); }

    /**
     * Notifies peers of the game start and installs {@link WebSocket} message handlers.
     */
    public synchronized void start() {
        if (!started) {
            for (final PairedPlayer player : players.values()) {
                new ReadyToStart(player.getOpponentId(), nextPlayerId)
                        .write(player.getOutputSocket());
                player.getInputSocket().onMessage(new F.Callback<JsonNode>() {
                    @Override
                    public void invoke(JsonNode move) throws Throwable {
                        onMove(player.getId(), move);
                    }
                });
                player.getInputSocket().onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        shutdownListener.onGameShutdown(id);
                    }
                });
            }
            started = true;
            log.trace("{} is started. (Pair information is pushed.)", this);
        }
    }

    /**
     * Checks if game is over and invokes {@link Game#shutdownListener} on success.
     */
    private synchronized void complete() {
        boolean over = false;
        for (PairedPlayer player : players.values())
            if (player.isOver()) {
                over = true;
                break;
            }
        if (over) {
            int winnerScore = 0;
            String winnerId = null;
            for (PairedPlayer player : players.values()) {
                int score = player.score();
                if (winnerScore < score) {
                    winnerScore = score;
                    winnerId = player.getId();
                }
            }
            GameOver go = new GameOver(winnerId);
            for (PairedPlayer player : players.values()) {
                go.write(player.getOutputSocket());
            }
            log.trace("{} is completed. Calling shutdown listener...", this);
            shutdownListener.onGameShutdown(id);
        }
    }

    /**
     * Validates the given move, updates board state, checks if game is over, and notifies peers.
     */
    private synchronized void onMove(String playerId, int pos) {
        PairedPlayer player = players.get(playerId);
        WebSocket.Out<JsonNode> out = player.getOutputSocket();
        int[] pits = player.getPits();
        if (pos < 0 || pos > pits.length - 2)
            new IllegalMove("Invalid pit index: %d", pos).write(out);
        else if (pits[pos] < 1)
            new IllegalMove("No stones available at pit %d.", pos).write(out);
        else {
            int size = pits[pos];
            pits[pos] = 0;
            for (int i = 0; i < size; i++)
                pits[(pos + i + 1) % pits.length]++;
            int lastPos = (pos + size) % pits.length;
            if (lastPos != pits.length - 1) {
                nextPlayerId = player.getOpponentId();
                if (pits[lastPos] == 1) {
                    int[] opponentsPits = players.get(player.getOpponentId()).getPits();
                    int opponentsPos = pits.length - lastPos - 2;
                    int opponentsSize = opponentsPits[opponentsPos];
                    opponentsPits[opponentsPos] = 0;
                    pits[lastPos] = 0;
                    pits[pits.length - 1] += opponentsSize + 1;
                }
            }
            BoardState boardState = new BoardState(players.values(), nextPlayerId);
            for (PairedPlayer pairedPlayer : players.values())
                boardState.write(pairedPlayer.getOutputSocket());
        }
        complete();
    }

    /**
     * Validates the given move and passes the control to {@link Game#onMove(String, int).}
     */
    private synchronized void onMove(String playerId, JsonNode move) {
        ConnectedPlayer player = players.get(playerId);
        log.trace("New move from {}: {}", player, move);
        WebSocket.Out<JsonNode> out = player.getOutputSocket();
        if (!nextPlayerId.equals(playerId))
            new IllegalMove("It is opponent's turn.").write(out);
        else try { onMove(playerId, Integer.parseInt(move.asText())); }
        catch (NumberFormatException nfe) {
            new IllegalMove("Invalid pit index: %s", move).write(out);
        }
    }

    @Override
    public String toString() { return String.format("Game[%s]", id); }

}
