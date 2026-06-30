package com.connecthub.post;

import com.connecthub.common.dto.response.UploadMediaResponse;
import com.connecthub.common.exception.UploadMediaException;
import com.connecthub.common.service.MediaStorageService;
import com.connecthub.modules.features.post.entity.Media;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.enums.MediaType;
import com.connecthub.modules.features.post.repository.MediaRepository;
import com.connecthub.modules.features.post.service.MediaService;
import com.connecthub.modules.features.post.service.MediaService.UploadedMedia;
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

    // Helper: tạo MultipartFile mock với contentType, size, bytes tùy chỉnh
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

    // Helper: stub upload ảnh thành công
    private void stubUploadImageSuccess(String url, String publicId) {
        UploadMediaResponse uploadResponse = UploadMediaResponse.builder()
                .url(url).publicId(publicId).build();
        when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                .thenReturn(CompletableFuture.completedFuture(uploadResponse));
    }

    // Helper: stub upload video thành công
    private void stubUploadVideoSuccess(String url, String publicId) {
        UploadMediaResponse uploadResponse = UploadMediaResponse.builder()
                .url(url).publicId(publicId).build();
        when(mediaStorageService.uploadVideo(any(byte[].class), eq("post-media")))
                .thenReturn(CompletableFuture.completedFuture(uploadResponse));
    }

    // =====================================================================
    // uploadFiles() — input guards
    // =====================================================================
    @Nested
    @DisplayName("uploadFiles() — kiểm tra đầu vào")
    class InputGuardTest {

        @Test
        @DisplayName("Trả về list rỗng khi files = []")
        void uploadFiles_EmptyFileList_ReturnsEmptyList() {
            List<UploadedMedia> result = mediaService.uploadFiles(List.of());
            assertTrue(result.isEmpty());
            verifyNoInteractions(mediaStorageService);
        }

        @Test
        @DisplayName("Bỏ qua file rỗng (isEmpty = true), không upload")
        void uploadFiles_EmptyFile_SkipsUpload() {
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(emptyFile));

            assertTrue(result.isEmpty());
            verifyNoInteractions(mediaStorageService);
        }

        @Test
        @DisplayName("Ném NullPointerException khi files = null (không có null-guard)")
        void uploadFiles_NullFiles_ThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> mediaService.uploadFiles(null));
        }
    }

    // =====================================================================
    // uploadFiles() — upload ảnh
    // =====================================================================
    @Nested
    @DisplayName("uploadFiles() — upload ảnh (IMAGE)")
    class UploadImageTest {

        @Test
        @DisplayName("Thành công - Upload ảnh image/jpeg, trả về UploadedMedia đúng fields")
        void uploadImage_Jpeg_Success() throws Exception {
            byte[] bytes = new byte[]{1, 2, 3};
            MultipartFile file = mockFile("image/jpeg", 1024L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.jpg", "pub_jpg");

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(file));

            assertEquals(1, result.size());
            UploadedMedia media = result.get(0);
            assertEquals("http://cdn.com/img.jpg", media.url());
            assertEquals("pub_jpg", media.publicId());
            assertEquals(MediaType.IMAGE, media.type());
            assertEquals(1024L, media.size());

            verify(mediaStorageService).uploadImage(bytes, "post-media");
            verify(mediaStorageService, never()).uploadVideo(any(), any());
        }

        @Test
        @DisplayName("Thành công - Upload ảnh image/png")
        void uploadImage_Png_Success() throws Exception {
            byte[] bytes = new byte[]{4, 5, 6};
            MultipartFile file = mockFile("image/png", 2048L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.png", "pub_png");

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(file));

            assertEquals(1, result.size());
            assertEquals(MediaType.IMAGE, result.get(0).type());
            verify(mediaStorageService).uploadImage(bytes, "post-media");
        }

        @Test
        @DisplayName("Thành công - Upload ảnh image/webp")
        void uploadImage_Webp_Success() throws Exception {
            byte[] bytes = new byte[]{7, 8, 9};
            MultipartFile file = mockFile("image/webp", 512L, bytes);
            stubUploadImageSuccess("http://cdn.com/img.webp", "pub_webp");

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(file));

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

            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenReturn(CompletableFuture.completedFuture(
                            UploadMediaResponse.builder().url("http://cdn.com/1.jpg").publicId("pub1").build()))
                    .thenReturn(CompletableFuture.completedFuture(
                            UploadMediaResponse.builder().url("http://cdn.com/2.png").publicId("pub2").build()));

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(f1, f2));

            assertEquals(2, result.size());
            verify(mediaStorageService, times(2)).uploadImage(any(), eq("post-media"));
        }
    }

    // =====================================================================
    // uploadFiles() — upload video
    // =====================================================================
    @Nested
    @DisplayName("uploadFiles() — upload video (VIDEO)")
    class UploadVideoTest {

        @Test
        @DisplayName("Thành công - Upload video/mp4, dùng uploadVideo service")
        void uploadVideo_Mp4_Success() throws Exception {
            byte[] bytes = new byte[]{10, 20, 30};
            MultipartFile file = mockFile("video/mp4", 1024L * 1024L, bytes);
            stubUploadVideoSuccess("http://cdn.com/vid.mp4", "pub_mp4");

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(file));

            assertEquals(1, result.size());
            assertEquals(MediaType.VIDEO, result.get(0).type());
            verify(mediaStorageService).uploadVideo(bytes, "post-media");
            verify(mediaStorageService, never()).uploadImage(any(), any());
        }

        @Test
        @DisplayName("Thành công - Upload video/quicktime")
        void uploadVideo_Quicktime_Success() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("video/quicktime", 1024L, bytes);
            stubUploadVideoSuccess("http://cdn.com/vid.mov", "pub_mov");

            List<UploadedMedia> result = mediaService.uploadFiles(List.of(file));

            assertEquals(1, result.size());
            verify(mediaStorageService).uploadVideo(bytes, "post-media");
        }
    }

    // =====================================================================
    // uploadFiles() — content-type không hợp lệ
    // =====================================================================
    @Nested
    @DisplayName("uploadFiles() — content-type không hợp lệ")
    class ResolveMediaTypeTest {

        @Test
        @DisplayName("Thất bại - contentType = null → InvalidFileTypeException")
        void uploadFile_NullContentType_ThrowsInvalidFileTypeException() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn(null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
            verifyNoInteractions(mediaStorageService);
        }

        @Test
        @DisplayName("Thất bại - contentType = application/pdf → InvalidFileTypeException")
        void uploadFile_PdfContentType_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("application/pdf", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
            verifyNoInteractions(mediaStorageService);
        }

        @Test
        @DisplayName("Thất bại - contentType = text/plain → InvalidFileTypeException")
        void uploadFile_TextPlainContentType_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("text/plain", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
            verifyNoInteractions(mediaStorageService);
        }

        @Test
        @DisplayName("Thất bại - contentType = application/octet-stream → InvalidFileTypeException")
        void uploadFile_OctetStream_ThrowsInvalidFileTypeException() throws Exception {
            MultipartFile file = mockFile("application/octet-stream", 1024L, null);

            assertThrows(InvalidFileTypeException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
        }
    }

    // =====================================================================
    // uploadFiles() — lỗi khi upload lên cloud
    // =====================================================================
    @Nested
    @DisplayName("uploadFiles() — lỗi cloud upload")
    class CloudUploadFailureTest {

        @Test
        @DisplayName("Thất bại - uploadImage ném exception → bọc thành UploadMediaException")
        void uploadImage_StorageThrowsException_WrapsAsUploadMediaException() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("image/jpeg", 1024L, bytes);

            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenThrow(new RuntimeException("Cloudinary down"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
        }

        @Test
        @DisplayName("Thất bại - uploadVideo ném exception → bọc thành UploadMediaException")
        void uploadVideo_StorageThrowsException_WrapsAsUploadMediaException() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("video/mp4", 1024L, bytes);

            when(mediaStorageService.uploadVideo(any(byte[].class), eq("post-media")))
                    .thenThrow(new RuntimeException("Cloudinary down"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
        }

        @Test
        @DisplayName("Thất bại - getBytes() ném IOException → bọc thành UploadMediaException")
        void uploadImage_GetBytesThrowsIOException_WrapsAsUploadMediaException() throws Exception {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("image/jpeg");
            when(file.getBytes()).thenThrow(new java.io.IOException("Read error"));

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
        }

        @Test
        @DisplayName("Thất bại - CompletableFuture lỗi (get() ném ExecutionException) → bọc thành UploadMediaException")
        void uploadImage_FutureFailsExceptionally_WrapsAsUploadMediaException() throws Exception {
            byte[] bytes = new byte[]{1};
            MultipartFile file = mockFile("image/jpeg", 1024L, bytes);

            CompletableFuture<UploadMediaResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Upload failed"));
            when(mediaStorageService.uploadImage(any(byte[].class), eq("post-media")))
                    .thenReturn(failedFuture);

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadFiles(List.of(file)));
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

            assertThrows(UploadMediaException.class,
                    () -> mediaService.uploadFiles(List.of(f1, f2)));
        }
    }

    // =====================================================================
    // attachToPost() — gắn media đã upload vào post và lưu DB
    // =====================================================================
    @Nested
    @DisplayName("attachToPost() — lưu Media vào DB")
    class AttachToPostTest {

        @Test
        @DisplayName("Trả về list rỗng khi uploadedList rỗng")
        void attachToPost_EmptyUploadedList_ReturnsEmptyList() {
            when(mediaRepository.saveAll(anyList())).thenReturn(List.of());

            List<Media> result = mediaService.attachToPost(List.of(), mockPost);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Thành công - build Media đúng url/publicId/type/size/post rồi saveAll")
        void attachToPost_Success_BuildsCorrectMediaFields() {
            UploadedMedia uploaded = new UploadedMedia(
                    "https://res.cloudinary.com/demo/image.jpg", "connecthub/abc123", MediaType.IMAGE, 4096L);

            when(mediaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.attachToPost(List.of(uploaded), mockPost);

            assertEquals(1, result.size());
            Media saved = result.get(0);
            assertEquals("https://res.cloudinary.com/demo/image.jpg", saved.getUrl());
            assertEquals("connecthub/abc123", saved.getPublicAvtId());
            assertEquals(MediaType.IMAGE, saved.getType());
            assertEquals(java.math.BigInteger.valueOf(4096L), saved.getSize());
            assertEquals(mockPost, saved.getPost());
            assertNotNull(saved.getId());
        }

        @Test
        @DisplayName("Thành công - gắn nhiều media (ảnh + video) trong 1 lần gọi")
        void attachToPost_MultipleMedia_Success() {
            UploadedMedia img = new UploadedMedia("http://img.url", "img_pub", MediaType.IMAGE, 100L);
            UploadedMedia vid = new UploadedMedia("http://vid.url", "vid_pub", MediaType.VIDEO, 200L);

            when(mediaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<Media> result = mediaService.attachToPost(List.of(img, vid), mockPost);

            assertEquals(2, result.size());
            ArgumentCaptor<List<Media>> captor = ArgumentCaptor.forClass(List.class);
            verify(mediaRepository).saveAll(captor.capture());
            assertEquals(2, captor.getValue().size());
        }

        @Test
        @DisplayName("Gọi saveAll đúng 1 lần (batch, không save từng cái)")
        void attachToPost_CallsSaveAllOnce() {
            UploadedMedia img = new UploadedMedia("http://img.url", "img_pub", MediaType.IMAGE, 100L);
            when(mediaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            mediaService.attachToPost(List.of(img), mockPost);

            verify(mediaRepository, times(1)).saveAll(anyList());
        }
    }
}