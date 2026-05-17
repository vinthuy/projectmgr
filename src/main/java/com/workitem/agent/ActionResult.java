package com.workitem.agent;

import lombok.Data;

@Data
public class ActionResult {
    private boolean success;
    private Object data;
    private String errorMessage;
    
    public static ActionResult success(Object data) {
        ActionResult result = new ActionResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }
    
    public static ActionResult error(String message) {
        ActionResult result = new ActionResult();
        result.setSuccess(false);
        result.setErrorMessage(message);
        return result;
    }
}
