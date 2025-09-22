package com.metaverse.planti_be.photo.service;

import com.metaverse.planti_be.device.domain.Device;
import com.metaverse.planti_be.device.repository.DeviceRepository;
import com.metaverse.planti_be.photo.domain.Photo;
import com.metaverse.planti_be.photo.dto.PhotoRequestDto;
import com.metaverse.planti_be.photo.dto.PhotoResponseDto;
import com.metaverse.planti_be.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

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

        // PhotoResponseDto의 생성자를 사용하여 DTO로 변환 후 반환
        return new PhotoResponseDto(savedPhoto);
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