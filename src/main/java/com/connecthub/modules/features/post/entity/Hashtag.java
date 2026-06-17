package com.connecthub.modules.features.post.entity;

import com.connecthub.common.entity.BaseEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "hashtags")
public class Hashtag extends BaseEntity {
    @Id
    private UUID id;

    private String name;
    @OneToMany(mappedBy = "hashtag")
    private Set<PostHashtag> postHashtags;
}
