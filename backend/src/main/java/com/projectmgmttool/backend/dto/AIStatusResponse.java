package com.projectmgmttool.backend.dto;

public class AIStatusResponse {
    private boolean enabled;

    public AIStatusResponse() {}
    public AIStatusResponse(boolean enabled) { this.enabled = enabled; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
