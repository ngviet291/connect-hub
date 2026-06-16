package com.connecthub.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(PostHashtagId.class)
public class PostHashtag {
    @Id
    @ManyToOne
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;
    @Id
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}
