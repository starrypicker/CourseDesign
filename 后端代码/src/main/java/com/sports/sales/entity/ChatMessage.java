package com.sports.sales.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String senderCode;
    private String senderName;
    private String senderRole;    // 'customer' 或 'admin'
    private String receiverCode;
    private String content;
    private Integer isRead;       // 0-未读, 1-已读
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderCode() { return senderCode; }
    public void setSenderCode(String senderCode) { this.senderCode = senderCode; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getReceiverCode() { return receiverCode; }
    public void setReceiverCode(String receiverCode) { this.receiverCode = receiverCode; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
