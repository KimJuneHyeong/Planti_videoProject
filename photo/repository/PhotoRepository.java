package com.metaverse.planti_be.photo.repository;

import com.metaverse.planti_be.photo.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    Optional<Photo> findTopByOrderByIdDesc();// JpaRepository<관리할 엔티티, 엔티티의 ID 타입>
}