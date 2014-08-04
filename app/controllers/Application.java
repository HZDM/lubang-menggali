package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Game;
import models.event.WaitingForOpponent;
import models.player.ConnectedPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@ThreadSafe
public class Application extends Controller {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    /**
     * {@link Queue} of players waiting to be paired.
     */
    private static final Queue<ConnectedPlayer> pendingPlayers = new ConcurrentLinkedQueue<>();

    /**
     * {@link Map} of active {@link Game}s keyed by game ids.
     */
    private static final Map<String, Game> games = new ConcurrentHashMap<>();

    public static Queue<ConnectedPlayer> getPendingPlayers() { return pendingPlayers; }

    public static Map<String, Game> getGames() { return games; }

    public static Result index() {
        int gameCount = games.size();
        int playerCount = pendingPlayers.size() + 2 * gameCount;
        return ok(index.render(gameCount, playerCount));
    }

    /**
     * Accepts incoming join requests.
     *
     * Function either pairs the connection with a pending player, or pushes
     * it to the pending players queue. Requests are handled in parallel, no
     * synchronization is necessary.
     */
    public static WebSocket<JsonNode> join() {
        return new WebSocket<JsonNode>() {
            @Override
            public void onReady(In<JsonNode> in, Out<JsonNode> out) {
                ConnectedPlayer lowerPlayer = new ConnectedPlayer(in, out);
                log.trace("Incoming {}.", lowerPlayer);

                // Tell the player that we are trying to find a pair.
                new WaitingForOpponent(lowerPlayer.getId()).write(out);

                // Try to pair the incoming player with another player.
                ConnectedPlayer upperPlayer = pendingPlayers.poll();
                if (upperPlayer != null) {
                    Game game = new Game(
                            upperPlayer, lowerPlayer,
                            new Game.ShutdownListener() {
                                @Override
                                public void onGameShutdown(String gameId) {
                                    Application.onGameShutdown(gameId);
                                }
                            });
                    game.start();
                    games.put(game.getId(), game);
                    log.trace("Started {} with {} and {}.", game, upperPlayer, lowerPlayer);
                }

                // Else, queue the player into the waiting list.
                else {
                    pendingPlayers.add(lowerPlayer);
                    log.trace("Queued {}.", lowerPlayer);
                }
            }
        };
    }

    /**
     * Closes player connections and cleans up data structures.
     */
    public static void onGameShutdown(String gameId) {
        Game game = games.remove(gameId);
        for (ConnectedPlayer player : game.getPlayers())
            player.getOutputSocket().close();
        log.trace("Closed {}.", game);
    }

}
