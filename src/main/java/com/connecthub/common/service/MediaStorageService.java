package com.connecthub.common.service;

import com.connecthub.common.dto.response.UploadMediaResponse;

import java.util.concurrent.CompletableFuture;

public interface MediaStorageService {

    CompletableFuture<UploadMediaResponse> uploadImage(byte[] data, String folder);

    CompletableFuture<UploadMediaResponse> uploadVideo(byte[] data, String folder);

    void delete(String publicId);
}
