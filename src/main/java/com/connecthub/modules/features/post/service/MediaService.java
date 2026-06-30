package com.connecthub.modules.features.post.service;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.common.util.AppUtil;
import com.connecthub.modules.features.post.dto.response.UploadedMedia;
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

    // I/O thuần, gọi storage bên ngoài (Cloudinary...), KHÔNG đụng DB, KHÔNG @Transactional
    public List<UploadedMedia> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(this::upload)
                .toList();
    }

    // Thuần DB: build entity Media từ kết quả đã upload sẵn rồi save, gọi bên trong transaction
    public List<Media> attachToPost(List<UploadedMedia> uploadedList, Post post) {
        List<Media> mediaList = uploadedList.stream()
                .map(u -> Media.builder()
                        .id(AppUtil.generateUUID())
                        .url(u.url())
                        .publicAvtId(u.publicId())
                        .type(u.type())
                        .size(BigInteger.valueOf(u.size()))
                        .post(post)
                        .build())
                .toList();

        List<Media> saved = mediaRepository.saveAll(mediaList);
        log.info("Media attached to post: {} -> {} file(s)", post.getId(), saved.size());
        return saved;
    }

    private UploadedMedia upload(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null)
            throw new InvalidFileTypeException();

        MediaType mediaType = resolveMediaType(contentType);

        try {
            byte[] bytes = file.getBytes();
            UploadMediaResponse uploadResponse = (mediaType == MediaType.VIDEO
                    ? mediaStorageService.uploadVideo(bytes, POST_MEDIA_FOLDER)
                    : mediaStorageService.uploadImage(bytes, POST_MEDIA_FOLDER)).get();

            return new UploadedMedia(uploadResponse.getUrl(), uploadResponse.getPublicId(),
                    mediaType, file.getSize());

        } catch (Exception e) {
            log.error("Upload media failed", e);
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