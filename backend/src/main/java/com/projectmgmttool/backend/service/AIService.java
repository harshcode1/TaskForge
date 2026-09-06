package com.projectmgmttool.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectmgmttool.backend.exception.CustomApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Thin wrapper around OpenAI's Chat Completions API — task-description
 * generation and project-status summaries. Deliberately graceful when no key
 * is configured: isEnabled() lets the frontend hide the AI affordances
 * entirely rather than show a button that always fails, the same pattern
 * BetterMind (a different project) uses for its own AI companion falling
 * back to keyword matching without an API key.
 *
 * A plain RestClient call rather than pulling in an OpenAI SDK — this is one
 * HTTP call with a small, stable request/response shape; a whole SDK
 * dependency for that is more surface area than the problem needs.
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final String MODEL = "gpt-4o-mini";

    @Value("${openai.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateTaskDescription(String title, String projectName) {
        String system = "You write concise, professional task descriptions for a project "
                + "management tool. One to three sentences, specific and actionable, no headers "
                + "or bullet points, no restating the title verbatim.";
        String user = "Project: " + projectName + "\nTask title: " + title
                + "\n\nWrite a description for this task.";
        return chat(system, user, 150);
    }

    public String generateProjectSummary(String projectName, long total, long completed, long inProgress,
                                          long overdue, int completionRate, List<String> overdueTaskTitles) {
        String system = "You are a project status assistant. Given task statistics, write a brief "
                + "(2-4 sentence) plain-English status summary a manager could read in five seconds. "
                + "Mention risk (overdue work) if there is any, otherwise don't manufacture concern. "
                + "Prose only, no bullet points, no headers.";
        StringBuilder user = new StringBuilder()
                .append("Project: ").append(projectName)
                .append("\nTotal tasks: ").append(total)
                .append("\nCompleted: ").append(completed)
                .append("\nIn progress: ").append(inProgress)
                .append("\nCompletion rate: ").append(completionRate).append("%")
                .append("\nOverdue: ").append(overdue);
        if (overdue > 0 && overdueTaskTitles != null && !overdueTaskTitles.isEmpty()) {
            user.append(" (").append(String.join(", ", overdueTaskTitles)).append(")");
        }
        return chat(system, user.toString(), 200);
    }

    private String chat(String systemPrompt, String userPrompt, int maxTokens) {
        if (!isEnabled()) {
            throw new CustomApiException("AI features require an OPENAI_API_KEY to be configured", 503);
        }
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(MODEL, List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", userPrompt)
                    ), maxTokens, 0.6))
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new CustomApiException("AI service returned an empty response", 502);
            }
            return response.choices().get(0).message().content().trim();
        } catch (CustomApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OpenAI call failed: {}", e.getMessage());
            throw new CustomApiException("AI service is temporarily unavailable", 502);
        }
    }

    private record ChatMessage(String role, String content) {}

    private record ChatRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {}

    private record ChatChoice(ChatMessage message) {}

    private record ChatCompletionResponse(List<ChatChoice> choices) {}
}
