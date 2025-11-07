package io.store.ua.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlResourceReader {
    private static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String getSQL(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("SQL resource name must not be blank");
        }

        return CACHE.computeIfAbsent("classpath:sql/%s.sql".formatted(name), file -> {
            Resource resource = RESOURCE_LOADER.getResource(file);

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("SQL resource not found or unreadable: " + file);
            }

            try (Reader inputStreamReader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(inputStreamReader);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read SQL resource: " + file, e);
            }
        });
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
