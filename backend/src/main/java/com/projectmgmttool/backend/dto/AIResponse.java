package com.projectmgmttool.backend.dto;

/**
 * Shared shape for every AI endpoint's text output — a description and a
 * project summary are both "here is a string the model wrote," no reason
 * for two near-identical DTOs.
 */
public class AIResponse {
    private String text;

    public AIResponse() {}
    public AIResponse(String text) { this.text = text; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
