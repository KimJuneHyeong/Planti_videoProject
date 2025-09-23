package com.metaverse.planti_be.photo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnalysisResponseDto {
    private String objectName;
    private Double confidence;
}