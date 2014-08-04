package models.player;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.WebSocket;

/**
 * Represents a paired player along with the opponent and the board information.
 */
public class PairedPlayer extends ConnectedPlayer {

    protected final String opponentId;
    protected final int[] pits;

    protected PairedPlayer(
            WebSocket.In<JsonNode> inputSocket,
            WebSocket.Out<JsonNode> outputSocket,
            String opponentId) {
        super(inputSocket, outputSocket);
        this.opponentId = opponentId;
        this.pits = new int[] {6, 6, 6, 6, 6, 6, 0};
    }

    public String getOpponentId() { return opponentId; }

    public int[] getPits() { return pits; }

    public boolean isOver() {
        int size = 0;
        for (int i = 0; i < pits.length - 1; i++) size += pits[i];
        return (size == 0);
    }

    public int score() { return pits[pits.length - 1]; }

    @Override
    public String toString() { return String.format("PairedPlayer[%s]", id); }

}
