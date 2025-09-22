package com.metaverse.planti_be.photo.controller;

import com.metaverse.planti_be.photo.dto.PhotoRequestDto;
import com.metaverse.planti_be.photo.dto.PhotoResponseDto;
import com.metaverse.planti_be.photo.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
