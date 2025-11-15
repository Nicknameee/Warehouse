package io.store.ua.controllers;

import io.store.ua.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll(@RequestParam(name = "pageSize") int pageSize,
                                     @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(tagService.findAll(pageSize, page));
    }

    @GetMapping("/findBy")
    public ResponseEntity<?> findBy(@RequestParam(name = "name", required = false) String name,
                                    @RequestParam(name = "isActive", required = false) Boolean isActive,
                                    @RequestParam(name = "pageSize") int pageSize,
                                    @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(tagService.findBy(name, isActive, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestParam(name = "name") String tagName) {
        return ResponseEntity.ok(tagService.save(tagName));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestParam(name = "id") Long tagId,
                                    @RequestParam(name = "name", required = false) String tagName,
                                    @RequestParam(name = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(tagService.update(tagId, tagName, isActive));
    }

    @PutMapping("/clearAll")
    public ResponseEntity<?> clearUnusedTags() {
        tagService.clearUnusedTags();

        return ResponseEntity.ok().build();
    }
}
