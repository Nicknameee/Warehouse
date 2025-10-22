package io.store.ua.utilities;

import io.store.ua.utility.SqlResourceReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlResourceReaderTest {
    @AfterEach
    void cleanup() {
        SqlResourceReader.clearCache();
    }

    @Test
    void readsTempClasspathResource_normalizesNewlines(@TempDir Path temporaryDirectory)
            throws Exception {
        Path sqlDirectory = Files.createDirectories(temporaryDirectory.resolve("sql"));
        Path sampleSqlFile = sqlDirectory.resolve("sample.sql");
        Files.writeString(sampleSqlFile, "SELECT 1;", StandardCharsets.UTF_8);

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader temporaryClassLoader =
                     new URLClassLoader(
                             new URL[]{temporaryDirectory.toUri().toURL()}, originalContextClassLoader)) {
            Thread.currentThread().setContextClassLoader(temporaryClassLoader);

            String sqlText = SqlResourceReader.getSQL("sample");

            assertEquals("SELECT 1;", sqlText);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    @Test
    void throwsForMissingResource(@TempDir Path temporaryDirectory) throws Exception {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader temporaryClassLoader =
                     new URLClassLoader(
                             new URL[]{temporaryDirectory.toUri().toURL()}, originalContextClassLoader)) {
            Thread.currentThread().setContextClassLoader(temporaryClassLoader);

            assertThrows(IllegalArgumentException.class, () -> SqlResourceReader.getSQL("no_such_sql"));
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    @Test
    void cacheHoldsFirstRead_evenIfFileChanges(@TempDir Path temporaryDirectory) throws Exception {
        Path sqlDirectory = Files.createDirectories(temporaryDirectory.resolve("sql"));
        Path cachedSqlFile = sqlDirectory.resolve("cached.sql");
        Files.writeString(cachedSqlFile, "SELECT 1;", StandardCharsets.UTF_8);

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader temporaryClassLoader =
                     new URLClassLoader(
                             new URL[]{temporaryDirectory.toUri().toURL()}, originalContextClassLoader)) {
            Thread.currentThread().setContextClassLoader(temporaryClassLoader);

            String firstRead = SqlResourceReader.getSQL("cached");
            assertEquals("SELECT 1;", firstRead);

            Files.writeString(cachedSqlFile, "SELECT 2;", StandardCharsets.UTF_8);

            String secondRead = SqlResourceReader.getSQL("cached");
            assertEquals("SELECT 1;", secondRead);

            SqlResourceReader.clearCache();
            String thirdRead = SqlResourceReader.getSQL("cached");
            assertEquals("SELECT 2;", thirdRead);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    @Test
    void concurrentReadsComputeOnce(@TempDir Path temporaryDirectory) throws Exception {
        Path sqlDirectory = Files.createDirectories(temporaryDirectory.resolve("sql"));
        Path concurrentSqlFile = sqlDirectory.resolve("concurrent.sql");
        Files.writeString(concurrentSqlFile, "SELECT 7;", StandardCharsets.UTF_8);

        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader temporaryClassLoader =
                     new URLClassLoader(
                             new URL[]{temporaryDirectory.toUri().toURL()}, originalContextClassLoader)) {
            Thread.currentThread().setContextClassLoader(temporaryClassLoader);

            int threadCount = 8;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicReference<String> firstResultRef = new AtomicReference<>();
            AtomicReference<Throwable> firstErrorRef = new AtomicReference<>();

            Runnable readTask =
                    () -> {
                        try {
                            startLatch.await();
                            String sqlText = SqlResourceReader.getSQL("concurrent");
                            firstResultRef.compareAndSet(null, sqlText);
                            assertEquals(firstResultRef.get(), sqlText);
                            assertEquals("SELECT 7;", sqlText);
                        } catch (Throwable throwable) {
                            firstErrorRef.compareAndSet(null, throwable);
                        } finally {
                            doneLatch.countDown();
                        }
                    };

            for (int i = 0; i < threadCount; i++) new Thread(readTask).start();
            startLatch.countDown();
            doneLatch.await();

            if (firstErrorRef.get() != null) {
                Throwable t = firstErrorRef.get();
                if (t instanceof RuntimeException re) throw re;
                if (t instanceof Error err) throw err;
                throw new UncheckedIOException(new IOException(t));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }
}
