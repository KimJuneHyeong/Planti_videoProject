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

    @Column(nullable = false, length = 512) // 파일 경로 길이를 고려해 넉넉하게 설정
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    // 직접 선언
    public Photo(Device device, String filePath, String fileName) {
        this.device = device;
        this.filePath = filePath;
        this.fileName = fileName;
    }
}
