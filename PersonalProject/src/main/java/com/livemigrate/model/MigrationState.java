package com.livemigrate.model;

// Migration state enum
public enum MigrationState {
    INITIALIZED,
    PREPARING,
    MIGRATING,
    VALIDATING,
    SWITCHING,
    COMPLETED,
    PAUSED,
    ERROR
}
