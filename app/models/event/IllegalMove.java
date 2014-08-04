package models.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

/**
 * Entity for describing illegal game moves.
 */
@ThreadSafe
public class IllegalMove extends Event {

    public final String type = "IllegalMove";

    @NotNull public final String reason;

    public IllegalMove(String reason, Object... args) {
        this.reason = String.format(reason, args);
    }

    @JsonCreator
    public IllegalMove(
            @JsonProperty("type") String type,
            @JsonProperty("reason") String reason) {
        this(reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllegalMove)) return false;
        IllegalMove that = (IllegalMove) o;
        return reason.equals(that.reason);
    }

    @Override
    public int hashCode() { return reason.hashCode(); }

}
