package io.github.drdeathdrop.atlas.resource.inventory;

import java.util.UUID;

public class InvalidTeamException extends RuntimeException {
    public InvalidTeamException(UUID teamId) {
        super("Resource " + teamId + " is not a team");
    }
}
