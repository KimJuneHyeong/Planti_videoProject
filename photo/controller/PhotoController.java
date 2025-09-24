package com.metaverse.planti_be.photo.controller;

import com.metaverse.planti_be.photo.dto.PhotoRequestDto;
import com.metaverse.planti_be.photo.dto.PhotoResponseDto;
import com.metaverse.planti_be.photo.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // 생성자 주입을 위한 Lombok 어노테이션
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/photos")
    public ResponseEntity<PhotoResponseDto> uploadPhoto(
            @ModelAttribute PhotoRequestDto requestDto) throws IOException {

        // 서비스 호출
        PhotoResponseDto responseDto = photoService.savePhoto(requestDto);

        // 생성 성공 시, 201 CREATED 상태 코드와 생성된 리소스의 DTO를 함께 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/photos/analyze-detailed")
    public ResponseEntity<PhotoResponseDto> analyzePhotoDetailed(
            @ModelAttribute PhotoRequestDto requestDto) throws IOException {

        // 상세 분석 서비스 호출 (DB 저장 없이 분석 결과만 반환)
        PhotoResponseDto responseDto = photoService.analyzePhotoDetailed(requestDto);

        // 분석 성공 시, 200 OK 상태 코드와 분석 결과 반환
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/photos/latest")
    public ResponseEntity<PhotoResponseDto> getLatestPhoto() {
        PhotoResponseDto responseDto = photoService.findLatestPhoto();
        return ResponseEntity.ok(responseDto);
    }
}