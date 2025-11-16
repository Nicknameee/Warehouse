package io.store.ua.controllers;

import io.store.ua.entity.ProductPhoto;
import io.store.ua.service.ProductPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productPhotos")
@RequiredArgsConstructor
public class ProductPhotoController {
    private final ProductPhotoService productPhotoService;

    @PostMapping("/saveAll")
    public ResponseEntity<List<ProductPhoto>> saveAll(@RequestParam("productId") Long productId,
                                                      @RequestPart("photo") List<MultipartFile> photos) {
        return ResponseEntity.ok(productPhotoService.saveAll(productId, photos));
    }

    @DeleteMapping("/removeAll")
    public ResponseEntity<Void> removeAll(@RequestParam("photoId") List<Long> photoIds) {
        productPhotoService.removeAll(photoIds);

        return ResponseEntity.ok().build();
    }
}
