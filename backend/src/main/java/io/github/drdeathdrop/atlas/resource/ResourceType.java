package io.github.drdeathdrop.atlas.resource;

public enum ResourceType {
    MEDICAL_TEAM(ResourceKind.TEAM),
    FIRE_TEAM(ResourceKind.TEAM),
    RESCUE_TEAM(ResourceKind.TEAM),
    POLICE_UNIT(ResourceKind.TEAM),
    TECHNICAL_TEAM(ResourceKind.TEAM),
    AMBULANCE(ResourceKind.VEHICLE),
    FIRE_TRUCK(ResourceKind.VEHICLE),
    RESCUE_HELICOPTER(ResourceKind.VEHICLE),
    RESCUE_BOAT(ResourceKind.VEHICLE),
    POLICE_CAR(ResourceKind.VEHICLE),
    UTILITY_VEHICLE(ResourceKind.VEHICLE);

    private final ResourceKind kind;

    ResourceType(ResourceKind kind) {
        this.kind = kind;
    }

    public ResourceKind kind() {
        return kind;
    }
}
