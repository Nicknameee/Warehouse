package io.store.ua.repository;

import io.store.ua.entity.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
    Optional<ProductPhoto> findByPhotoUrl(String photoUrl);
}
