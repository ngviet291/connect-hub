package com.connecthub.modules.features.post.enums;

import lombok.Getter;

@Getter
public enum PostResponseCode {
    CREATE_POST_SUCCESS("success.post.create", 2000),
    GET_POST_SUCCESS("success.post.get", 2001),
    UPDATE_POST_SUCCESS("success.post.update", 2002),
    DELETE_POST_SUCCESS("success.post.delete", 2003),
    GET_FEED_SUCCESS("success.post.get_feed", 2004),
    GET_HASHTAG_POSTS_SUCCESS("success.post.get_hashtag_posts", 2005),
    ADD_HASHTAG_SUCCESS("success.post.add_hashtag", 2006),
    CREATE_REPLY_SUCCESS("success.post.create_reply", 2007),
    GET_REPLIES_SUCCESS("success.post.get_replies", 2008),
    REACTION_SUCCESS("success.post.reaction", 2009),
    BOOKMARK_SUCCESS("success.post.bookmark", 2010),
    GET_BOOKMARKS_SUCCESS("success.post.get_bookmarks", 2011),
    REPOST_SUCCESS("success.post.repost", 2012),
    VIEW_RECORDED_SUCCESS("success.post.view_recorded", 2013),
    UPLOAD_MEDIA_SUCCESS("success.post.upload_media", 2014),
    GET_POST_MENTIONS_SUCCESS("success.post.get_mentions", 2015),
    GET_MY_MENTIONS_SUCCESS("success.mention.get_my", 2016);

    private final String message;
    private final int code;

    PostResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}

