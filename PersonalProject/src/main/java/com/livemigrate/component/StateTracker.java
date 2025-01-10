package com.livemigrate.component;

import com.livemigrate.model.MigrationState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StateTracker {
    private static final String MIGRATION_STATE_KEY = "migration:state";
    private static final String MIGRATION_PROGRESS_KEY = "migration:progress";
    private static final String LAST_PROCESSED_ID_KEY = "migration:last_processed_id";

    private final RedisTemplate<String, Object> redisTemplate;

    // State management methods
    public void setState(MigrationState state) {
        redisTemplate.opsForValue().set(MIGRATION_STATE_KEY, state.name());
    }

    public MigrationState getState() {
        String state = (String) redisTemplate.opsForValue().get(MIGRATION_STATE_KEY);
        return state != null ? MigrationState.valueOf(state) : MigrationState.INITIALIZED;
    }

    // Progress tracking methods
    public void updateProgress(long processed, long total) {
        double progress = (double) processed / total * 100;
        redisTemplate.opsForValue().set(MIGRATION_PROGRESS_KEY, progress);
    }

    public double getProgress() {
        Double progress = (Double) redisTemplate.opsForValue().get(MIGRATION_PROGRESS_KEY);
        return progress != null ? progress : 0.0;
    }

    // Checkpoint management
    public void saveCheckpoint(UUID lastProcessedId) {
        redisTemplate.opsForValue().set(LAST_PROCESSED_ID_KEY, lastProcessedId.toString());
    }

    public Optional<UUID> getLastCheckpoint() {
        String lastId = (String) redisTemplate.opsForValue().get(LAST_PROCESSED_ID_KEY);
        return lastId != null ? Optional.of(UUID.fromString(lastId)) : Optional.empty();
    }

    // Reset state tracker
    public void reset() {
        redisTemplate.delete(MIGRATION_STATE_KEY);
        redisTemplate.delete(MIGRATION_PROGRESS_KEY);
        redisTemplate.delete(LAST_PROCESSED_ID_KEY);
    }
}