package models.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

/**
 * Entity for storing the game over event along with the winning player.
 */
@ThreadSafe
public class GameOver extends Event {

    public final String type = "GameOver";

    @NotNull public final String winnerId;

    public GameOver(String winnerId) { this.winnerId = winnerId; }

    @JsonCreator
    public GameOver(
            @JsonProperty("type") String type,
            @JsonProperty("winnerId") String winnerId) {
        this(winnerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameOver)) return false;
        GameOver gameOver = (GameOver) o;
        return winnerId.equals(gameOver.winnerId);
    }

    @Override
    public int hashCode() { return winnerId.hashCode(); }

}
