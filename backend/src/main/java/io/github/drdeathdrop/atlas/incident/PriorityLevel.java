package io.github.drdeathdrop.atlas.incident;

public enum PriorityLevel {
    LOW,
    ELEVATED,
    HIGH,
    URGENT;

    public static PriorityLevel of(int score) {
        if (score >= 75) {
            return URGENT;
        }
        if (score >= 50) {
            return HIGH;
        }
        if (score >= 25) {
            return ELEVATED;
        }
        return LOW;
    }
}
