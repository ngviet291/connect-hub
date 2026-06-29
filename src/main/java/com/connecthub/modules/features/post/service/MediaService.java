package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.user.exception.InvalidFileTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
    private static final String POST_MEDIA_FOLDER = "post-media";

    private final MediaStorageService mediaStorageService;
    private final MediaRepository mediaRepository;

    public List<Media> uploadAndAttachToPost(List<MultipartFile> files, Post post) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> upload(file, post))
                .toList();
    }

    private Media upload(MultipartFile file, Post post) {
        String contentType = file.getContentType();
        if (contentType == null)
            throw new InvalidFileTypeException();

        MediaType mediaType = resolveMediaType(contentType);

        try {
            byte[] bytes = file.getBytes();
            UploadMediaResponse uploadResponse = (mediaType == MediaType.VIDEO
                    ? mediaStorageService.uploadVideo(bytes, POST_MEDIA_FOLDER)
                    : mediaStorageService.uploadImage(bytes, POST_MEDIA_FOLDER)).get();

            Media media = mediaRepository.save(Media.builder()
                    .id(AppUtil.generateUUID())
                    .url(uploadResponse.getUrl())
                    .publicAvtId(uploadResponse.getPublicId())
                    .type(mediaType)
                    .size(BigInteger.valueOf(file.getSize()))
                    .post(post)
                    .build());

            log.info("Media uploaded: {} -> post: {}", media.getId(), post.getId());
            return media;

        } catch (Exception e) {
            log.error("Upload media failed for post: {}", post.getId(), e);
            throw new UploadMediaException();
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType.startsWith("image/"))
            return MediaType.IMAGE;
        if (contentType.startsWith("video/"))
            return MediaType.VIDEO;
        throw new InvalidFileTypeException();
    }

}