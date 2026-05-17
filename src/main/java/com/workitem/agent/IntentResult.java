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
}
