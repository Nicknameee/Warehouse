package io.store.ua.repository;

import io.store.ua.entity.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
}
