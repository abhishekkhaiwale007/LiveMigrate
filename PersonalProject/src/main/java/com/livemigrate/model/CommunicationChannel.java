package com.livemigrate.model;

import lombok.Data;

import java.time.Instant;

@Data
public class CommunicationChannel {
    private boolean enabled;
    private String preferredTime;  // e.g., "morning", "afternoon", "evening"
    private String frequency;      // e.g., "daily", "weekly", "monthly"
    private Instant lastContact;
    private boolean hasOptedOut;
}