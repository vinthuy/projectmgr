package com.workitem.agent;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class IntentResult {
    private String intent;
    private Map<String, Object> entities = new HashMap<>();
    private Double confidence;
    private String originalInput;


    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
