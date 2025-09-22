package com.metaverse.planti_be.photo.repository;

import com.metaverse.planti_be.photo.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    // JpaRepository<관리할 엔티티, 엔티티의 ID 타입>
}