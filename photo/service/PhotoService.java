package com.metaverse.planti_be.photo.service;

import com.metaverse.planti_be.device.domain.Device;
import com.metaverse.planti_be.device.repository.DeviceRepository;
import com.metaverse.planti_be.photo.domain.Photo;
import com.metaverse.planti_be.photo.dto.AnalysisResponseDto;
import com.metaverse.planti_be.photo.dto.PhotoRequestDto;
import com.metaverse.planti_be.photo.dto.PhotoResponseDto;
import com.metaverse.planti_be.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final DeviceRepository deviceRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto requestDto) throws IOException {
        MultipartFile imageFile = requestDto.getImageFile();
        String serialNumber = requestDto.getSerialNumber();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + serialNumber));

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID().toString() + "." + extension;

        String filePath = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        // 제공해주신 Photo.java의 public 생성자를 사용하여 엔티티 생성
        Photo photo = new Photo(device, filePath, fileName);

        Photo savedPhoto = photoRepository.save(photo);
        // AI 분석 요청 로직 - 파일을 직접 전송하는 방식으로 변경
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // MultiValueMap을 사용하여 파일을 전송
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(filePath)));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // Python API 서버에 POST 요청 보내고 결과를 AnalysisResponseDto로 받음
            AnalysisResponseDto analysisResponse = restTemplate.postForObject(aiServerUrl, entity, AnalysisResponseDto.class);

            // 응답이 있고, 분석된 객체 이름이 있다면 DB에 업데이트
            if (analysisResponse != null && analysisResponse.getObjectName() != null) {
                savedPhoto.updateAnalysis(analysisResponse.getObjectName(), analysisResponse.getConfidence());
            }

        } catch (Exception e) {
            System.err.println("AI 서버 호출 실패: " + e.getMessage());
            e.printStackTrace(); // 상세한 오류 정보 출력
        }

        return new PhotoResponseDto(savedPhoto);
    }

    // React가 호출할 최신 사진 정보 조회 메소드
    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        // ID가 가장 큰 (가장 최근의) 사진을 가져옴
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new) // Photo -> PhotoResponseDto 변환
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        try {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }
}