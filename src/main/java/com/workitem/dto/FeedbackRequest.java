package com.workitem.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
public class FeedbackRequest {
    @NotBlank(message = "消息ID不能为空")
    private String messageId;
    
    @Min(1)
    @Max(5)
    private Integer score;
    
    private String comment;
}
