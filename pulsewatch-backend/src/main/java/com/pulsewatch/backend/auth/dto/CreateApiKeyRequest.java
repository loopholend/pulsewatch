package com.pulsewatch.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateApiKeyRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    private int durationDays = 0; // 0 = never expires

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
}
