package io.store.ua.service.external;

import io.store.ua.models.api.external.response.CloudinaryImageUploadResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CloudinaryAPIService.class)
@ActiveProfiles("external")
@TestPropertySource(properties = {
        "cloudinary.cloud=${CLOUDINARY_CLOUD_NAME}",
        "cloudinary.credentials.apiKey=${CLOUDINARY_API_KEY}",
        "cloudinary.credentials.apiSecret=${CLOUDINARY_API_SECRET}"
})
@Disabled("IT for real Cloudinary API invocation for testing service setup flow")
class CloudinaryAPIServiceIT {
    @Autowired
    private CloudinaryAPIService cloudinaryAPIService;

    @Test
    void uploadAndDeleteRealImage() throws Exception {
        byte[] imageBytes;
        BufferedImage bufferedImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(Color.ORANGE);
        graphics2D.fillRect(0, 0, 50, 50);
        graphics2D.dispose();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        imageBytes = byteArrayOutputStream.toByteArray();

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "file.png",
                "image/png",
                imageBytes
        );

        CloudinaryImageUploadResponse cloudinaryImage = cloudinaryAPIService.uploadImage(multipartFile, "junie")
                .orTimeout(30, TimeUnit.SECONDS)
                .join();

        assertNotNull(cloudinaryImage);
        assertNotNull(cloudinaryImage.getSecureUrl());
        assertNotNull(cloudinaryImage.getUrl());
        assertNotNull(cloudinaryImage.getPublicId());
        assertTrue(cloudinaryImage.getSecureUrl().startsWith("https://"));
        assertTrue(cloudinaryImage.getUrl().startsWith("http://"));

        Boolean deleted = cloudinaryAPIService.deleteImage(cloudinaryImage.getPublicId())
                .orTimeout(30, TimeUnit.SECONDS)
                .join();

        assertEquals(Boolean.TRUE, deleted);
    }

    @Test
    void deleteNonExistingImage() {
        Boolean deleted = cloudinaryAPIService.deleteImage(RandomStringUtils.secure().nextAlphanumeric(333))
                .orTimeout(30, TimeUnit.SECONDS)
                .join();

        assertEquals(Boolean.TRUE, deleted);
    }
}

