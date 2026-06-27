package com.connecthub.post;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.post.service.MediaService;
import com.connecthub.modules.features.user.exception.FileSizeExceededException;
import com.connecthub.modules.features.user.exception.InvalidFileTypeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private MediaStorageService mediaStorageService;
    @Mock private MediaRepository mediaRepository;

    @InjectMocks
    private MediaService mediaService;

    private Post mockPost;

    @BeforeEach
    void setUp() {
        mockPost = Post.builder().id(UUID.randomUUID()).build();
    }

    // Helper: tạo MultipartFile mock với contentType và size tùy chỉnh
    private MultipartFile mockFile(String contentType, long size, byte[] bytes) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(size);
        if (bytes != null) {
            when(file.getBytes()).thenReturn(bytes);
        }
        return file;
    }

    // Helper: stub upload thành công
    private void stubUploadImageSuccess(String url, String publicId) {
        UploadMediaResponse uploadResponse = UploadMediaResponse.builder()
                .url(url).publicId(publicId).build();
        when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                .thenReturn(CompletableFuture.completedFuture(uploadResponse));
    }

    private void stubUploadVideoSuccess(String url, String publicId) {
        UploadMediaResponse uploadResponse = UploadMediaResponse.builder()
                .url(url).publicId(publicId).build();
        when(mediaStorageService.uploadVideo(any(byte[].class), eq("post-media")))
                .thenReturn(CompletableFuture.completedFuture(uploadResponse));
    }

    // =====================================================================
    // uploadAndAttachToPost() — input guards
    // =====================================================================
    @Nested
    @DisplayName("uploadAndAttachToPost() — kiểm tra đầu vào")
    class InputGuardTest {

        @Test
        @DisplayName("Trả về list rỗng khi files = null")
        void uploadAndAttachToPost_NullFiles_ReturnsEmptyList() {
            List<Media> result = mediaService.uploadAndAttachToPost(null, mockPost);
            assertTrue(result.isEmpty());
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Trả về list rỗng khi files = []")
        void uploadAndAttachToPost_EmptyFileList_ReturnsEmptyList() {
            List<Media> result = mediaService.uploadAndAttachToPost(List.of(), mockPost);
            assertTrue(result.isEmpty());
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Bỏ qua file null trong list, tiếp tục xử lý file hợp lệ")
        void uploadAndAttachToPost_ListContainsNullFile_SkipsNull() throws Exception {
            MultipartFile validFile = mockFile("image/jpeg", 1024L, new byte[]{1, 2, 3});
            stubUploadImageSuccess("http://img.url", "pub123");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            // null ở vị trí đầu, file hợp lệ ở sau
            List<Media> result = mediaService.uploadAndAttachToPost(
                    java.util.Arrays.asList(null, validFile), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadImage(any(), eq("post-media"));
        }

        @Test
        @DisplayName("Bỏ qua file rỗng (isEmpty = true), không upload")
        void uploadAndAttachToPost_EmptyFile_SkipsUpload() throws Exception {
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(emptyFile), mockPost);

            assertTrue(result.isEmpty());
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }
    }

    // =====================================================================
    // uploadAndAttachToPost() — upload ảnh
    // =====================================================================
    @Nested
    @DisplayName("uploadAndAttachToPost() — upload ảnh (IMAGE)")
    class UploadImageTest {

        @Test
        @DisplayName("Thành công - Upload ảnh image/jpeg, lưu Media đúng fields")
        void uploadImage_Jpeg_Success() throws Exception {
            byte[] bytes = new byte[]{1, 2, 3};
            MultipartFile file = mockFile("image/jpeg", 1024L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.jpg", "pub_jpg");

            Media savedMedia = Media.builder()
                    .id(UUID.randomUUID())
                    .url("http://cdn.com/img.jpg")
                    .type(MediaType.IMAGE)
                    .post(mockPost)
                    .build();
            when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadImage(bytes, "post-media");
            verify(mediaStorageService, never()).uploadVideo(any(), any());

            ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository).save(captor.capture());
            Media captured = captor.getValue();
            assertEquals("http://cdn.com/img.jpg", captured.getUrl());
            assertEquals("pub_jpg", captured.getPublicAvtId());
            assertEquals(MediaType.IMAGE, captured.getType());
            assertEquals(mockPost, captured.getPost());
        }

        @Test
        @DisplayName("Thành công - Upload ảnh image/png")
        void uploadImage_Png_Success() throws Exception {
            byte[] bytes = new byte[]{4, 5, 6};
            MultipartFile file = mockFile("image/png", 2048L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.png", "pub_png");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadImage(bytes, "post-media");
        }

        @Test
        @DisplayName("Thành công - Upload ảnh image/webp")
        void uploadImage_Webp_Success() throws Exception {
            byte[] bytes = new byte[]{7, 8, 9};
            MultipartFile file = mockFile("image/webp", 512L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.webp", "pub_webp");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadImage(bytes, "post-media");
        }

        @Test
        @DisplayName("Thành công - Upload nhiều ảnh trong 1 lần gọi")
        void uploadMultipleImages_Success() throws Exception {
            byte[] b1 = new byte[]{1};
            byte[] b2 = new byte[]{2};
            MultipartFile f1 = mockFile("image/jpeg", 1000L, b1);
            MultipartFile f2 = mockFile("image/png", 2000L, b2);

            stubUploadImageSuccess("http://cdn.com/1.jpg", "pub1");
            // stub lần 2 (cùng method, lần gọi thứ 2)
            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenReturn(CompletableFuture.completedFuture(
                            UploadMediaResponse.builder().url("http://cdn.com/1.jpg").publicId("pub1").build()))
                    .thenReturn(CompletableFuture.completedFuture(
                            UploadMediaResponse.builder().url("http://cdn.com/2.png").publicId("pub2").build()));
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(f1, f2), mockPost);

            assertEquals(2, result.size());
            verify(mediaStorageService, times(2)).uploadImage(any(), eq("post-media"));
        }

        @Test
        @DisplayName("Thất bại - Ảnh vượt quá 10MB → FileSizeExceededException")
        void uploadImage_ExceedsMaxSize_ThrowsFileSizeExceededException() throws Exception {
            long overLimit = 10L * 1024 * 1024 + 1; // 10MB + 1 byte
            MultipartFile file = mockFile("image/jpeg", overLimit, null);

            assertThrows(FileSizeExceededException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Thành công - Ảnh đúng 10MB (boundary) → không throw")
        void uploadImage_ExactlyMaxSize_Success() throws Exception {
            long exactly10MB = 10L * 1024 * 1024;
            byte[] bytes = new byte[0];
            MultipartFile file = mockFile("image/jpeg", exactly10MB, bytes);
            stubUploadImageSuccess("http://cdn.com/img.jpg", "pub");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
        }
    }

    // =====================================================================
    // uploadAndAttachToPost() — upload video
    // =====================================================================
    @Nested
    @DisplayName("uploadAndAttachToPost() — upload video (VIDEO)")
    class UploadVideoTest {

        @Test
        @DisplayName("Thành công - Upload video/mp4, dùng uploadVideo service")
        void uploadVideo_Mp4_Success() throws Exception {
            byte[] bytes = new byte[]{10, 20, 30};
            MultipartFile file = mockFile("video/mp4", 1024L * 1024L, bytes);
            stubUploadVideoSuccess("http://cdn.com/vid.mp4", "pub_mp4");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadVideo(bytes, "post-media");
            verify(mediaStorageService, never()).uploadImage(any(), any());

            ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
            verify(mediaRepository).save(captor.capture());
            assertEquals(MediaType.VIDEO, captor.getValue().getType());
        }

        @Test
        @DisplayName("Thành công - Upload video/quicktime")
        void uploadVideo_Quicktime_Success() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("video/quicktime", 1024L, bytes);
            stubUploadVideoSuccess("http://cdn.com/vid.mov", "pub_mov");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadVideo(bytes, "post-media");
        }

        @Test
        @DisplayName("Thất bại - Video vượt quá 100MB → FileSizeExceededException")
        void uploadVideo_ExceedsMaxSize_ThrowsFileSizeExceededException() throws Exception {
            long overLimit = 100L * 1024 * 1024 + 1; // 100MB + 1 byte
            MultipartFile file = mockFile("video/mp4", overLimit, null);

            assertThrows(FileSizeExceededException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Thành công - Video đúng 100MB (boundary) → không throw")
        void uploadVideo_ExactlyMaxSize_Success() throws Exception {
            long exactly100MB = 100L * 1024 * 1024;
            byte[] bytes = new byte[0];
            MultipartFile file = mockFile("video/mp4", exactly100MB, bytes);
            stubUploadVideoSuccess("http://cdn.com/vid.mp4", "pub");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
        }
    }

    // =====================================================================
    // resolveMediaType() — content-type không hợp lệ
    // =====================================================================
    @Nested
    @DisplayName("resolveMediaType() — content-type không hợp lệ")
    class ResolveMediaTypeTest {

        @Test
        @DisplayName("Thất bại - contentType = null → InvalidFileTypeException")
        void uploadFile_NullContentType_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn(null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Thất bại - contentType = application/pdf → InvalidFileTypeException")
        void uploadFile_PdfContentType_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("application/pdf", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Thất bại - contentType = text/plain → InvalidFileTypeException")
        void uploadFile_TextPlainContentType_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("text/plain", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verifyNoInteractions(mediaStorageService, mediaRepository);
        }

        @Test
        @DisplayName("Thất bại - contentType = application/octet-stream → InvalidFileTypeException")
        void uploadFile_OctetStream_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("application/octet-stream", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
        }
    }

    // =====================================================================
    // uploadAndAttachToPost() — lỗi khi upload lên cloud
    // =====================================================================
    @Nested
    @DisplayName("uploadAndAttachToPost() — lỗi cloud upload")
    class CloudUploadFailureTest {

        @Test
        @DisplayName("Thất bại - uploadImage ném exception → bọc thành UploadMediaException")
        void uploadImage_StorageThrowsException_WrapsAsUploadMediaException() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("image/jpeg", 1024L, bytes);

            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenThrow(new RuntimeException("Cloudinary down"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verify(mediaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - uploadVideo ném exception → bọc thành UploadMediaException")
        void uploadVideo_StorageThrowsException_WrapsAsUploadMediaException() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("video/mp4", 1024L, bytes);

            when(mediaStorageService.uploadVideo(any(byte[].class), eq("post-media")))
                    .thenThrow(new RuntimeException("Cloudinary down"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verify(mediaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - getBytes() ném IOException → bọc thành UploadMediaException")
        void uploadImage_GetBytesThrowsIOException_WrapsAsUploadMediaException() throws Exception {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("image/jpeg");
            when(file.getSize()).thenReturn(1024L);
            when(file.getBytes()).thenThrow(new java.io.IOException("Read error"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(file), mockPost));
            verify(mediaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Thất bại - Upload thứ nhất thành công, thứ hai fail → throw UploadMediaException")
        void uploadMultipleFiles_SecondFileFails_ThrowsUploadMediaException() throws Exception {
            byte[] b1 = new byte[]{1};
            byte[] b2 = new byte[]{2};
            MultipartFile f1 = mockFile("image/jpeg", 1000L, b1);
            MultipartFile f2 = mockFile("image/png", 2000L, b2);

            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenReturn(CompletableFuture.completedFuture(
                            UploadMediaResponse.builder().url("http://ok.com").publicId("p1").build()))
                    .thenThrow(new RuntimeException("Fail on second"));
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadAndAttachToPost(List.of(f1, f2), mockPost));
        }
    }

    // =====================================================================
    // uploadAndAttachToPost() — kiểm tra Media được lưu đúng
    // =====================================================================
    @Nested
    @DisplayName("uploadAndAttachToPost() — kiểm tra Media entity được lưu")
    class SavedMediaTest {

        @Test
        @DisplayName("Media được gắn đúng post")
        void uploadImage_SavedMedia_HasCorrectPost() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("image/jpeg", 512L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.jpg", "pub123");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(mockPost, result.get(0).getPost());
        }

        @Test
        @DisplayName("Media lưu đúng url và publicAvtId từ cloud response")
        void uploadImage_SavedMedia_HasCorrectUrlAndPublicId() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("image/jpeg", 512L, bytes);
            stubUploadImageSuccess("https://res.cloudinary.com/demo/image.jpg", "connecthub/abc123");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals("https://res.cloudinary.com/demo/image.jpg", result.get(0).getUrl());
            assertEquals("connecthub/abc123", result.get(0).getPublicAvtId());
        }

        @Test
        @DisplayName("Media lưu đúng size từ file")
        void uploadImage_SavedMedia_HasCorrectSize() throws Exception {
            long fileSize = 4096L;
            byte[] bytes = new byte[0];
            MultipartFile file = mockFile("image/jpeg", fileSize, bytes);
            stubUploadImageSuccess("http://cdn.com/img.jpg", "pub");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.uploadAndAttachToPost(List.of(file), mockPost);

            assertEquals(java.math.BigInteger.valueOf(fileSize), result.get(0).getSize());
        }

        @Test
        @DisplayName("Folder upload luôn là 'post-media'")
        void upload_AlwaysUsesPostMediaFolder() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile imageFile = mockFile("image/jpeg", 512L, bytes);
            MultipartFile videoFile = mockFile("video/mp4", 512L, bytes);

            stubUploadImageSuccess("http://img.url", "img_pub");
            stubUploadVideoSuccess("http://vid.url", "vid_pub");
            when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

            mediaService.uploadAndAttachToPost(List.of(imageFile, videoFile), mockPost);

            verify(mediaStorageService).uploadImage(any(), eq("post-media"));
            verify(mediaStorageService).uploadVideo(any(), eq("post-media"));
        }
    }
}