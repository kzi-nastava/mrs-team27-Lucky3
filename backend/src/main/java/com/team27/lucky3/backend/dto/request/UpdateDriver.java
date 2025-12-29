package com.team27.lucky3.backend.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UpdateDriver extends CreateDriver {
    private boolean isActive;
    private String active24h;
}

