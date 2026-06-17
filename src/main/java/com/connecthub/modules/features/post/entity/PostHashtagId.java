package com.connecthub.modules.features.post.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostHashtagId implements Serializable {
    private UUID post;
    private UUID hashtag;
}
