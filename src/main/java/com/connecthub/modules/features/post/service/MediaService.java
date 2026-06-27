package com.connecthub.modules.features.post.service;

import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.user.exception.FileSizeExceededException;
import com.connecthub.modules.features.user.exception.FileNotFoundException;
import com.connecthub.modules.features.user.exception.InvalidFileTypeException;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100L * 1024 * 1024;
    private static final String POST_MEDIA_FOLDER = "post-media";

    private final MediaStorageService mediaStorageService;
    private final MediaRepository mediaRepository;

    //Upload files và trả về list Media đã lưu (có post gắn sẵn).
     //PostService dùng list này để set vào post.media trước khi map response.

    public List<Media> uploadAndAttachToPost(List<MultipartFile> files, Post post) {
        List<Media> result = new ArrayList<>();
        if (files == null || files.isEmpty()) return result;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String contentType = file.getContentType();
            if (contentType == null) throw new InvalidFileTypeException();

            MediaType mediaType = resolveMediaType(contentType);
            validateFileSize(file.getSize(), mediaType);

            try {
                var uploadResponse = (mediaType == MediaType.VIDEO
                        ? mediaStorageService.uploadVideo(file.getBytes(), POST_MEDIA_FOLDER)
                        : mediaStorageService.uploadImage(file.getBytes(), POST_MEDIA_FOLDER)
                ).join();

                Media media = mediaRepository.save(Media.builder()
                        .id(UuidCreator.getTimeOrderedEpoch())
                        .url(uploadResponse.getUrl())
                        .publicAvtId(uploadResponse.getPublicId())
                        .type(mediaType)
                        .size(BigInteger.valueOf(file.getSize()))
                        .post(post)
                        .build());

                result.add(media);
                log.info("Media uploaded: {} -> post: {}", media.getId(), post.getId());

            } catch (Exception e) {
                log.error("Upload media failed for post: {}", post.getId(), e);
                throw new UploadMediaException();
            }
        }
        return result;
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType.startsWith("image/")) return MediaType.IMAGE;
        if (contentType.startsWith("video/")) return MediaType.VIDEO;
        throw new InvalidFileTypeException();
    }

    private void validateFileSize(long size, MediaType type) {
        long max = (type == MediaType.VIDEO) ? MAX_VIDEO_SIZE : MAX_IMAGE_SIZE;
        if (size > max) throw new FileSizeExceededException();
    }
}
