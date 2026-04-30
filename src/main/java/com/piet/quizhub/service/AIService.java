package com.piet.quizhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;
import java.util.List;

@Service
public class AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String getAIQuestions(String topic, int count, String difficulty) {
        RestTemplate restTemplate = new RestTemplate();

        // Jaisa tere document mein tha
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt = String.format(
            "Generate exactly %d MCQ questions on the topic '%s' with '%s' difficulty. " +
            "Return ONLY a raw JSON array of objects. Keys: \"content\", \"optionA\", \"optionB\", \"optionC\", \"optionD\", \"correctAns\". " +
            "For \"correctAns\", use only a single letter: A, B, C, or D. No markdown, no extra text.",
            count, topic, difficulty
        );

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geminiUrl, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                Map candidate = (Map) candidates.get(0);
                Map content = (Map) candidate.get("content");
                List parts = (List) content.get("parts");
                String rawText = (String) ((Map) parts.get(0)).get("text");

                // Cleaning Markdown
                return rawText.replace("```json", "").replace("```", "").trim();
            }
            return "Error: Received status " + response.getStatusCode();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}