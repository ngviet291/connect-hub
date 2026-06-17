package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.PostHashtag;
import com.connecthub.modules.features.post.entity.PostHashtagId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtagId> {
}
