package com.gather.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLocationRequest {

    @NotBlank(message = "cityId is required")
    private String cityId;
}
