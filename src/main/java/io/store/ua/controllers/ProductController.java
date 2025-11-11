package io.store.ua.controllers;

import io.store.ua.entity.Product;
import io.store.ua.models.dto.ProductDTO;
import io.store.ua.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/findAll")
    public ResponseEntity<List<Product>> findAll(@RequestParam("pageSize") int pageSize,
                                                 @RequestParam("page") int page) {
        return ResponseEntity.ok(productService.findAll(pageSize, page));
    }

    @GetMapping("/findBy/code")
    public ResponseEntity<Product> findByCode(@RequestParam("code") String code) {
        return ResponseEntity.ok(productService.findByCode(code));
    }

    @GetMapping("/findWith/tags")
    public ResponseEntity<List<Product>> findWithTags(@RequestParam("tagId") List<Long> tagIds,
                                                      @RequestParam("pageSize") int pageSize,
                                                      @RequestParam("page") int page) {
        return ResponseEntity.ok(productService.findWithTags(tagIds, pageSize, page));
    }

    @GetMapping("/findBy")
    public ResponseEntity<List<Product>> findBy(@RequestParam(value = "titlePart", required = false) String titlePart,
                                                @RequestParam(value = "minimumPrice", required = false) BigDecimal minimumPrice,
                                                @RequestParam(value = "maximumPrice", required = false) BigDecimal maximumPrice,
                                                @RequestParam(value = "tagId", required = false) List<Long> tagIds,
                                                @RequestParam(value = "from", required = false)
                                                @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime from,
                                                @RequestParam(value = "to", required = false)
                                                @DateTimeFormat(pattern = "dd-MM-yyyy'At'HH:mm:ss") LocalDateTime to,
                                                @RequestParam("pageSize") int pageSize,
                                                @RequestParam("page") int pageNumber) {
        return ResponseEntity.ok(productService.findBy(titlePart,
                minimumPrice,
                maximumPrice,
                tagIds,
                from,
                to,
                pageSize,
                pageNumber));
    }

    @PostMapping
    public ResponseEntity<Product> save(@RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.save(productDTO));
    }

    @PutMapping
    public ResponseEntity<Product> update(@RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.update(productDTO));
    }
}
