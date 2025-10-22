package io.store.ua.service;

import io.store.ua.entity.ProductPhoto;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.repository.ProductPhotoRepository;
import io.store.ua.repository.ProductRepository;
import io.store.ua.service.external.CloudinaryAPIService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class ProductPhotoService {
    private final ProductPhotoRepository productPhotoRepository;
    private final ProductRepository productRepository;
    private final CloudinaryAPIService cloudinaryAPIService;

    public List<ProductPhoto> saveAll(@NotNull(message = "A productId can't be null")
                                      @Min(value = 1, message = "A productId can't be less than 1")
                                      Long productId,
                                      @NotEmpty(message = "A list of photos can't be empty") List<@NotNull(message = "A photo can't be null") MultipartFile> photos) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product with ID '%s' was not found".formatted(productId));
        }

        List<ProductPhoto> entities = cloudinaryAPIService.uploadAllImages(photos)
                .join()
                .stream()
                .map(upload -> ProductPhoto.builder()
                        .productId(productId)
                        .photoUrl(StringUtils.defaultIfBlank(upload.getSecureUrl(), upload.getUrl()))
                        .externalReference(upload.getPublicId())
                        .build())
                .toList();

        return productPhotoRepository.saveAll(entities);
    }
}
