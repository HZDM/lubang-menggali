package models.player;

import java.util.UUID;

/**
 * Base class representing a player in the game.
 */
public abstract class Player {

    protected String id = UUID.randomUUID().toString();

    public String getId() { return id; }

    @Override
    public String toString() { return String.format("Player[%s]", id); }

}
