package com.workitem.agent;

import java.util.Map;

public interface Tool {
    Object execute(Map<String, Object> params) throws Exception;
}
