package models.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

/**
 * Entity for describing the start of a game along with the next and opponent player information.
 */
@ThreadSafe
public class ReadyToStart extends Event {

    public final String type = "ReadyToStart";

    @NotNull public final String opponentId;
    @NotNull public final String nextPlayerId;

    public ReadyToStart(String opponentId, String nextPlayerId) {
        this.opponentId = opponentId;
        this.nextPlayerId = nextPlayerId;
    }

    @JsonCreator
    public ReadyToStart(
            @JsonProperty("type") String type,
            @JsonProperty("opponentId") String opponentId,
            @JsonProperty("nextPlayerId") String nextPlayerId) {
        this(opponentId, nextPlayerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadyToStart)) return false;
        ReadyToStart that = (ReadyToStart) o;
        return (nextPlayerId.equals(that.nextPlayerId) &&
                opponentId.equals(that.opponentId));
    }

    @Override
    public int hashCode() {
        return opponentId.hashCode() * 31 + nextPlayerId.hashCode();
    }

}
