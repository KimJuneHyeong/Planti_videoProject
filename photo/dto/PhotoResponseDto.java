package com.metaverse.planti_be.photo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.planti_be.photo.domain.Photo;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
public class PhotoResponseDto {

    private final Long id;
    private final String filePath;
    private final String fileName;
    private final String deviceSerialNumber;

    private final String analysisResult;
    private final Double confidence;
    private final Integer totalDetected;

    // 상세 분석 결과
    private final Map<String, Integer> classSummary;
    private final List<Map<String, Object>> detections;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    // Photo 엔티티를 DTO로 변환하는 public 생성자
    public PhotoResponseDto(Photo photo) {
        this.id = photo.getId();
        this.filePath = photo.getFilePath();
        this.fileName = photo.getFileName();
        // Device가 null인 경우를 대비한 안전한 처리
        this.deviceSerialNumber = photo.getDevice() != null ?
                photo.getDevice().getId() : null;
        this.createdAt = photo.getCreatedAt();
        this.analysisResult = photo.getAnalysisResult();
        this.confidence = photo.getConfidence();
        this.totalDetected = photo.getTotalDetected();

        // JSON 문자열을 객체로 변환
        DetailedAnalysisData detailedData = parseDetailedResults(photo.getDetailedResults());
        this.classSummary = detailedData.classSummary;
        this.detections = detailedData.detections;
    }

    // JSON 문자열을 파싱하는 헬퍼 메소드
    private DetailedAnalysisData parseDetailedResults(String detailedResultsJson) {
        if (detailedResultsJson == null || detailedResultsJson.isEmpty()) {
            return new DetailedAnalysisData();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(detailedResultsJson, DetailedAnalysisData.class);
        } catch (JsonProcessingException e) {
            System.err.println("상세 결과 파싱 오류: " + e.getMessage());
            return new DetailedAnalysisData();
        }
    }

    // 내부 클래스로 상세 데이터 구조 정의
    private static class DetailedAnalysisData {
        public Map<String, Integer> classSummary;
        public List<Map<String, Object>> detections;

        public DetailedAnalysisData() {
            this.classSummary = Map.of();
            this.detections = List.of();
        }
    }
}