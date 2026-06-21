package  com.connecthub.modules.features.chat.mapper;
import com.connecthub.modules.features.chat.dto.response.MessageResponse;
import com.connecthub.modules.features.chat.entity.Conversation;
import com.connecthub.modules.features.chat.entity.Message;
import com.connecthub.modules.features.chat.entity.MessageMedia;
import com.connecthub.modules.features.chat.enums.MemberStatus;
import com.connecthub.modules.features.chat.enums.MessageStatus;
import com.connecthub.modules.features.post.dto.response.MediaResponse;
import com.connecthub.modules.features.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "messageId",          source = "message.id")
    @Mapping(target = "content",            source = "message.content")
    @Mapping(target = "sentAt",             source = "message.createdAt")
    @Mapping(target = "conversationId",     source = "conversation.id")
    @Mapping(target = "conversationStatus", source = "conversationStatus")
    @Mapping(target = "senderId",           source = "sender.id")
    @Mapping(target = "senderUsername",     source = "sender.username")
    @Mapping(target = "senderAvatarUrl",    source = "sender.avatarUrl")
    @Mapping(target = "media",              source = "message.messageMedia")
    @Mapping(target = "status",             source = "status")
    MessageResponse toResponse(
            Message message,
            User sender,
            Conversation conversation,
            MemberStatus conversationStatus,
            MessageStatus status
    );

    @Mapping(target = "mediaId", source = "id")
    MediaResponse toMediaResponse(MessageMedia media);
}