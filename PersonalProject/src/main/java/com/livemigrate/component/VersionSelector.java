package com.livemigrate.component;

import com.livemigrate.model.MigrationState;
import com.livemigrate.model.RecordVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VersionSelector {
    private static final String MIGRATED_RECORDS_KEY = "migration:migrated_records";

    private final StateTracker stateTracker;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecordVersion getVersion(UUID recordId) {
        MigrationState currentState = stateTracker.getState();

        // If migration hasn't started or is completed, return appropriate version
        if (currentState == MigrationState.INITIALIZED) {
            return RecordVersion.V1;
        } else if (currentState == MigrationState.COMPLETED) {
            return RecordVersion.V2;
        }

        // During migration, check if this record has been migrated
        Boolean isMigrated = redisTemplate.opsForSet().isMember(
                MIGRATED_RECORDS_KEY,
                recordId.toString()
        );

        if (Boolean.TRUE.equals(isMigrated)) {
            return RecordVersion.V2;
        } else if (currentState == MigrationState.MIGRATING ||
                currentState == MigrationState.VALIDATING) {
            return RecordVersion.IN_MIGRATION;
        } else {
            return RecordVersion.V1;
        }
    }

    public void markAsMigrated(UUID recordId) {
        redisTemplate.opsForSet().add(MIGRATED_RECORDS_KEY, recordId.toString());
    }

    /**
     * Retrieves the count of records that have been successfully migrated.
     * This is used to track migration progress and support resume functionality.
     *
     * @return The number of records that have been migrated to V2 format
     */
    public long getProcessedCount() {
        Long size = redisTemplate.opsForSet().size(MIGRATED_RECORDS_KEY);
        return size != null ? size : 0L;
    }

    /**
     * Resets the migration tracking by clearing the set of migrated record IDs.
     * This is typically used when restarting a migration from scratch.
     */
    public void reset() {
        redisTemplate.delete(MIGRATED_RECORDS_KEY);
    }
}