package models.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import models.player.PairedPlayer;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Entity for storing the board state and the next player.
 */
@ThreadSafe
public class BoardState extends Event {

    public final String type = "BoardState";

    @NotNull public final Map<String, int[]> board;
    @NotNull public final String nextPlayerId;

    @JsonCreator
    public BoardState(
            @JsonProperty("board") Map<String, int[]> board,
            @JsonProperty("nextPlayerId") String nextPlayerId) {
        this.board = board;
        this.nextPlayerId = nextPlayerId;
    }

    public BoardState(Collection<PairedPlayer> players, String nextPlayerId) {
        Map<String, int[]> board = new HashMap<>();
        for (PairedPlayer player : players) board.put(player.getId(), player.getPits());
        this.board = Collections.unmodifiableMap(board);
        this.nextPlayerId = nextPlayerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardState)) return false;
        BoardState that = (BoardState) o;
        if (!nextPlayerId.equals(that.nextPlayerId)) return false;
        if (!board.keySet().equals(that.board.keySet())) return false;
        for (String playerId : board.keySet())
            if (!Arrays.equals(board.get(playerId), that.board.get(playerId)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        return (board.hashCode() * 31 + nextPlayerId.hashCode());
    }

}
