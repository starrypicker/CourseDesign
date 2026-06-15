package com.sports.sales.mapper;

import com.sports.sales.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    /** 获取与指定用户的历史消息 */
    List<ChatMessage> selectHistory(@Param("userCode1") String userCode1,
                                    @Param("userCode2") String userCode2,
                                    @Param("limit") int limit);

    /** 管理员获取所有聊过天的顾客列表 */
    List<String> selectContacts();

    /** 标记某顾客的消息为已读 */
    int markAsRead(@Param("senderCode") String senderCode);

    /** 获取未读消息数 */
    int countUnread();
}
