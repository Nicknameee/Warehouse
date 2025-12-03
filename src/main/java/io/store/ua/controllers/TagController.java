package io.store.ua.controllers;

import io.store.ua.entity.Tag;
import io.store.ua.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping("/findBy")
    public ResponseEntity<List<Tag>> findBy(@RequestParam(name = "name", required = false) String name,
                                            @RequestParam(name = "isActive", required = false) Boolean isActive,
                                            @RequestParam(name = "pageSize") int pageSize,
                                            @RequestParam(name = "page") int page) {
        return ResponseEntity.ok(tagService.findBy(name, isActive, pageSize, page));
    }

    @PostMapping
    public ResponseEntity<Tag> save(@RequestParam(name = "name") String tagName) {
        return ResponseEntity.ok(tagService.save(tagName));
    }

    @PutMapping
    public ResponseEntity<Tag> update(@RequestParam(name = "id") Long tagId,
                                      @RequestParam(name = "name", required = false) String tagName,
                                      @RequestParam(name = "isActive", required = false) Boolean isActive) {
        return ResponseEntity.ok(tagService.update(tagId, tagName, isActive));
    }

    @PutMapping("/clearAll")
    public ResponseEntity<List<Long>> clearUnusedTags() {
        return ResponseEntity.ok(tagService.clearUnusedTags());
    }
}
