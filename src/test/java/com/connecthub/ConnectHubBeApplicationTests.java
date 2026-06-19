package com.connecthub;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ConnectHubBeApplicationTests {

    @Autowired
    private MediaStorageService mediaStorageService;

    @Test
    void uploadImage() throws Exception {

        byte[] imageBytes = Files.readAllBytes(
                java.nio.file.Path.of("D:/MyLove/z6950808297538_d46b8a7c07a8a7b6f1c60887cc5a2b97.jpg")
        );

        UploadMediaResponse response =
                mediaStorageService
                        .uploadImage(imageBytes, "connect-hub-test")
                        .get();

        assertNotNull(response);
        assertNotNull(response.getPublicId());
        assertNotNull(response.getUrl());

        System.out.println(response);
    }

}
