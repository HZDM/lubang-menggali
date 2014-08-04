import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import controllers.Application;
import models.event.BoardState;
import models.event.IllegalMove;
import models.event.ReadyToStart;
import models.event.WaitingForOpponent;
import org.junit.Before;
import org.junit.Test;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.twirl.api.Content;

import java.util.HashMap;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;

/**
 * Tests index template and game logic by mocking a {@code WebSocket}.
 */
public class ApplicationTest {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Provides the {@code Context} required by template rendering.
     */
    @Before
    public void setContext() {
        Http.Context.current.set(new Http.Context(
                0L,
                mock(RequestHeader.class),
                mock(Http.Request.class),
                new HashMap <String, String>(),
                new HashMap<String, String>(),
                new HashMap<String, Object>()));
    }

    @Test
    public void testIndexTemplate() {
        Content html = views.html.index.render(0, 0);
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html))
                .contains("There are 0 game(s) and 0 pending player(s) online.");
    }

    private static <T> T readPojo(MockWebSocketWrapper socket, Class<T> clazz)
            throws InterruptedException, JsonProcessingException {
        JsonNode data = socket.read();
        assertThat(data).isNotNull();
        try { return objectMapper.convertValue(data, clazz); }
        catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(
                    "Invalid JSON: " + objectMapper.writeValueAsString(data), iae);
        }
    }

    private static void writeMove(MockWebSocketWrapper socket, Object pit) throws Throwable {
        socket.write(objectMapper.valueToTree(pit));
    }

    @Test
    public void testJoin() throws Throwable {
        // Introduce the first user and read the "WaitingForOpponent" message.
        MockWebSocketWrapper fstSocket = new MockWebSocketWrapper(Application.join());
        WaitingForOpponent fstWfo = readPojo(fstSocket, WaitingForOpponent.class);
        assertThat(Application.getPendingPlayers().size()).isEqualTo(1);
        assertThat(Application.getGames().size()).isEqualTo(0);

        // Introduce the second user and read the."WaitingForOpponent" message.
        MockWebSocketWrapper sndSocket = new MockWebSocketWrapper(Application.join());
        WaitingForOpponent sndWfo = readPojo(sndSocket, WaitingForOpponent.class);
        assertThat(fstWfo.playerId.equals(sndWfo.playerId)).isFalse();

        // Validate "ReadyToStart" messages.
        ReadyToStart fstRts = readPojo(fstSocket, ReadyToStart.class);
        ReadyToStart sndRts = readPojo(sndSocket, ReadyToStart.class);
        assertThat(Application.getGames().size()).isEqualTo(1);
        assertThat(Application.getPendingPlayers().size()).isEqualTo(0);
        assertThat(fstRts.nextPlayerId).isEqualTo(sndRts.nextPlayerId);
        assertThat(fstRts.opponentId).isEqualTo(sndWfo.playerId);
        assertThat(sndRts.opponentId).isEqualTo(fstWfo.playerId);

        // Let 2nd player make a move, while this is not his turn.
        writeMove(sndSocket, 0);
        IllegalMove im = readPojo(sndSocket, IllegalMove.class);
        assertThat(im.reason).isEqualTo("It is opponent's turn.");

        // Let 1st player make a move to an invalid pit index.
        writeMove(fstSocket, "n/a");
        im = readPojo(fstSocket, IllegalMove.class);
        assertThat(im.reason).matches("^Invalid pit index: .*");

        // Let 1st player make a move with a negative pit index.
        writeMove(fstSocket, -1);
        im = readPojo(fstSocket, IllegalMove.class);
        assertThat(im.reason).matches("^Invalid pit index: .*");

        // Let 1st player make a move to a pit with index larger than 5.
        writeMove(fstSocket, 6);
        im = readPojo(fstSocket, IllegalMove.class);
        assertThat(im.reason).matches("^Invalid pit index: .*");

        // Let 1st player make a move for the 1st pit.
        writeMove(fstSocket, 0);
        BoardState fstBs = readPojo(fstSocket, BoardState.class);
        BoardState sndBs = readPojo(sndSocket, BoardState.class);
        Set<String> playerIds = Sets.newHashSet(fstWfo.playerId, sndWfo.playerId);
        assertThat(fstBs.board.keySet()).isEqualTo(playerIds);
        assertThat(fstBs).isEqualTo(sndBs);
        // Since the last stone landed on the Lubang Menggali,
        // it is first player's turn again.
        assertThat(fstBs.nextPlayerId).isEqualTo(fstWfo.playerId);
        assertThat(fstBs.board.get(fstWfo.playerId)).isEqualTo(new int[]{0, 7, 7, 7, 7, 7, 1});

        // Let 1st player make a move for the 2nd pit this time. We aim to
        // sow the last stone to the pit we started, which is empty. Hence, we
        // will also capture the stones in the opposite pit.
        writeMove(fstSocket, 1);
        fstBs = readPojo(fstSocket, BoardState.class);
        sndBs = readPojo(sndSocket, BoardState.class);
        assertThat(fstBs).isEqualTo(sndBs);
        assertThat(fstBs.nextPlayerId).isEqualTo(sndWfo.playerId);
        assertThat(fstBs.board.get(fstWfo.playerId)).isEqualTo(new int[] {1, 0, 8, 8, 8, 8, 9});
        assertThat(fstBs.board.get(sndWfo.playerId)).isEqualTo(new int[] {6, 6, 6, 6, 0, 6, 0});

        // Now it is 2nd players turn. First let's try to make a move on an empty pit.
        writeMove(sndSocket, 4);
        im = readPojo(sndSocket, IllegalMove.class);
        assertThat(im.reason).matches("^No stones available at pit .*");

        // Fine. Sow the stones in the first pit, where the last stone will hit
        // to Lubang Menggali and give us a second chance.
        writeMove(sndSocket, 0);
        fstBs = readPojo(fstSocket, BoardState.class);
        sndBs = readPojo(sndSocket, BoardState.class);
        assertThat(fstBs).isEqualTo(sndBs);
        assertThat(fstBs.nextPlayerId).isEqualTo(sndWfo.playerId);
        assertThat(fstBs.board.get(sndWfo.playerId)).isEqualTo(new int[] {0, 7, 7, 7, 1, 7, 1});

        // Let 1st player close the connection. 2nd player is expected to
        // observe a socket close as well.
        fstSocket.close();
        JsonNode closeEvent = sndSocket.read();
        assertThat(closeEvent).isNotNull();
        assertThat(closeEvent.has("closed")).isTrue();
        assertThat(closeEvent.get("closed").asBoolean()).isTrue();
    }

}
