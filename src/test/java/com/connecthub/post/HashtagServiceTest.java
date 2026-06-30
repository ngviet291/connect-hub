package com.connecthub.modules.features.post.service;

import com.connecthub.modules.features.post.entity.Hashtag;
import com.connecthub.modules.features.post.entity.Post;
import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.repository.HashtagRepository;
import com.connecthub.modules.features.post.repository.PostHashtagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @InjectMocks
    private HashtagService hashtagService;

    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void addHashtagsToPost_allNewHashtags_createsHashtagsAndPostHashtags() {
        // given
        List<String> inputTags = List.of("Java", "Spring");

        when(hashtagRepository.findAllByNameIn(List.of("java", "spring")))
                .thenReturn(List.of()); // chưa có hashtag nào tồn tại

        // saveAll trả về đúng các entity được tạo (giả lập DB gán id sẵn có)
        when(hashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(postHashtagRepository.findHashtagIdsByPostId(post.getId()))
                .thenReturn(Set.of()); // post chưa có hashtag nào

        when(postHashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<PostHashtag> result = hashtagService.addHashtagsToPost(post, inputTags);

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ph -> ph.getHashtag().getName())
                .containsExactlyInAnyOrder("java", "spring");
        result.forEach(ph -> assertThat(ph.getPost()).isEqualTo(post));

        ArgumentCaptor<List<Hashtag>> hashtagCaptor = ArgumentCaptor.forClass(List.class);
        verify(hashtagRepository).saveAll(hashtagCaptor.capture());
        assertThat(hashtagCaptor.getValue())
                .extracting(Hashtag::getName)
                .containsExactlyInAnyOrder("java", "spring");

        verify(postHashtagRepository).saveAll(anyList());
    }

    @Test
    void addHashtagsToPost_someHashtagsAlreadyExist_onlyCreatesMissingOnes() {
        // given
        List<String> inputTags = List.of("java", "spring", "aws");

        Hashtag existingJava = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

        when(hashtagRepository.findAllByNameIn(List.of("java", "spring", "aws")))
                .thenReturn(List.of(existingJava)); // chỉ "java" đã tồn tại

        when(hashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(postHashtagRepository.findHashtagIdsByPostId(post.getId()))
                .thenReturn(Set.of());

        when(postHashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<PostHashtag> result = hashtagService.addHashtagsToPost(post, inputTags);

        // then
        assertThat(result).hasSize(3);

        ArgumentCaptor<List<Hashtag>> hashtagCaptor = ArgumentCaptor.forClass(List.class);
        verify(hashtagRepository).saveAll(hashtagCaptor.capture());
        // chỉ "spring" và "aws" được tạo mới, "java" thì không
        assertThat(hashtagCaptor.getValue())
                .extracting(Hashtag::getName)
                .containsExactlyInAnyOrder("spring", "aws");
    }

    @Test
    void addHashtagsToPost_hashtagAlreadyLinkedToPost_doesNotDuplicatePostHashtag() {
        // given
        List<String> inputTags = List.of("java", "spring");

        Hashtag javaTag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();
        Hashtag springTag = Hashtag.builder().id(UUID.randomUUID()).name("spring").build();

        when(hashtagRepository.findAllByNameIn(List.of("java", "spring")))
                .thenReturn(List.of(javaTag, springTag)); // cả 2 đã tồn tại

        // post đã được gắn "java" rồi -> chỉ "spring" cần được thêm mới
        when(postHashtagRepository.findHashtagIdsByPostId(post.getId()))
                .thenReturn(Set.of(javaTag.getId()));

        when(postHashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<PostHashtag> result = hashtagService.addHashtagsToPost(post, inputTags);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHashtag().getName()).isEqualTo("spring");

        // không tạo hashtag mới nào vì cả 2 đã tồn tại
        verify(hashtagRepository, never()).saveAll(anyList());
    }

    @Test
    void addHashtagsToPost_allHashtagsAlreadyLinked_returnsEmptyListAndSkipsSave() {
        // given
        List<String> inputTags = List.of("java");

        Hashtag javaTag = Hashtag.builder().id(UUID.randomUUID()).name("java").build();

        when(hashtagRepository.findAllByNameIn(List.of("java")))
                .thenReturn(List.of(javaTag));

        when(postHashtagRepository.findHashtagIdsByPostId(post.getId()))
                .thenReturn(Set.of(javaTag.getId())); // đã gắn sẵn

        when(postHashtagRepository.saveAll(List.of()))
                .thenReturn(List.of());

        // when
        List<PostHashtag> result = hashtagService.addHashtagsToPost(post, inputTags);

        // then
        assertThat(result).isEmpty();
        verify(hashtagRepository, never()).saveAll(anyList());
        verify(postHashtagRepository).saveAll(List.of());
    }

    @Test
    void addHashtagsToPost_inputCaseInsensitive_normalizesToLowercase() {
        // given
        List<String> inputTags = List.of("JaVa", "SPRING");

        when(hashtagRepository.findAllByNameIn(List.of("java", "spring")))
                .thenReturn(List.of());

        when(hashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(postHashtagRepository.findHashtagIdsByPostId(post.getId()))
                .thenReturn(Set.of());

        when(postHashtagRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        hashtagService.addHashtagsToPost(post, inputTags);

        // then: repository được gọi với danh sách đã lowercase
        verify(hashtagRepository).findAllByNameIn(List.of("java", "spring"));
    }
}