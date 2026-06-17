package com.connecthub.modules.features.post.repository;

import com.connecthub.modules.features.post.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtag.PostHashtagId> {
}
