package models.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

/**
 * Entity for describing the pending state of a player along with its id.
 */
@ThreadSafe
public class WaitingForOpponent extends Event {

    public final String type = "WaitingForOpponent";

    @NotNull public final String playerId;

    public WaitingForOpponent(String playerId) { this.playerId = playerId; }

    @JsonCreator
    public WaitingForOpponent(
            @JsonProperty("type") String type,
            @JsonProperty("playerId") String playerId) {
        this(playerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WaitingForOpponent)) return false;
        WaitingForOpponent that = (WaitingForOpponent) o;
        return playerId.equals(that.playerId);
    }

    @Override
    public int hashCode() { return playerId.hashCode(); }

}
