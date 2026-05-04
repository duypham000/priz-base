package com.priz.base.application.integration.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramApiResponse<T> {

    private boolean ok;
    private T result;
    private String description;

    @JsonProperty("error_code")
    private Integer errorCode;
}
