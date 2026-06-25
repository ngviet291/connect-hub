package com.connecthub.modules.features.post.enums;

import lombok.Getter;

@Getter
public enum PostResponseCode {
    CREATE_POST_SUCCESS("Post created successfully", 2000),
    GET_POST_SUCCESS("Post retrieved successfully", 2001),
    UPDATE_POST_SUCCESS("Post updated successfully", 2002),
    DELETE_POST_SUCCESS("Post deleted successfully", 2003),
    GET_FEED_SUCCESS("Feed retrieved successfully", 2004),
    GET_HASHTAG_POSTS_SUCCESS("Posts by hashtag retrieved successfully", 2005),
    ADD_HASHTAG_SUCCESS("Hashtag added to post successfully", 2006),
    CREATE_REPLY_SUCCESS("Reply created successfully", 2007),
    GET_REPLIES_SUCCESS("Replies retrieved successfully", 2008),
    REACTION_SUCCESS("Reaction updated successfully", 2009),
    BOOKMARK_SUCCESS("Bookmark updated successfully", 2010),
    GET_BOOKMARKS_SUCCESS("Bookmarks retrieved successfully", 2011),
    REPOST_SUCCESS("Repost updated successfully", 2012),
    VIEW_RECORDED_SUCCESS("View recorded successfully", 2013),
    VIEW_SUCCESS("View count retrieved successfully", 2014);
    private final String message;
    private final int code;

    PostResponseCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}

