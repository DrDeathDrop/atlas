package io.github.drdeathdrop.atlas.resource.inventory;

import java.util.UUID;

public interface NearbyResourceRow {
    UUID getId();

    String getCallSign();

    String getType();

    String getStatus();

    Double getLatitude();

    Double getLongitude();

    UUID getTeamId();

    Double getDistanceMeters();
}
