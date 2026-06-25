package com.connecthub.modules.features.chat.exception;

import com.connecthub.common.exception.ParameterizedException;

import java.util.Map;


public class GroupMemberLimitExceededException extends ParameterizedException {
    private static final String MAX_GROUP_MEMBER = "maxGroupMember";


    public GroupMemberLimitExceededException(String maxGroupMember) {
        super(ChatErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED, Map.of(MAX_GROUP_MEMBER, maxGroupMember));
    }
}