package com.metaverse.planti_be.photo.domain;

import com.metaverse.planti_be.common.TimeStamped;
import com.metaverse.planti_be.device.domain.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Photo extends TimeStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_serial", referencedColumnName = "serial_number")
    private Device device;

    @Column(nullable = false, length = 512)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String analysisResult; // 예: "level 3", "no_detection"

    @Column
    private Double confidence; // 예: 0.95 (95% 신뢰도)

    @Column
    private Integer totalDetected; // 총 검출된 객체 수

    @Column(length = 2000) // JSON 형태로 저장
    private String detailedResults; // 모든 검출 결과를 JSON으로 저장

    // 직접 선언
    public Photo(Device device, String filePath, String fileName) {
        this.device = device;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    // 분석 결과 업데이트 (기존 방식 유지 - 호환성)
    public void updateAnalysis(String result, Double confidence) {
        this.analysisResult = result;
        this.confidence = confidence;
    }

    // 상세 분석 결과 업데이트
    public void updateDetailedAnalysis(String bestResult, Double bestConfidence,
                                       Integer totalDetected, String detailedResults) {
        this.analysisResult = bestResult;
        this.confidence = bestConfidence;
        this.totalDetected = totalDetected;
        this.detailedResults = detailedResults;
    }
}