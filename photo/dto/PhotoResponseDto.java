package com.metaverse.planti_be.photo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.metaverse.planti_be.photo.domain.Photo;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PhotoResponseDto {

    private final Long id;
    private final String filePath;
    private final String fileName;
    private final String deviceSerialNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    // Photo 엔티티를 DTO로 변환하는 public 생성자
    public PhotoResponseDto(Photo photo) {
        this.id = photo.getId();
        this.filePath = photo.getFilePath();
        this.fileName = photo.getFileName();
        this.deviceSerialNumber = photo.getDevice().getId();
        this.createdAt = photo.getCreatedAt();
    }
}