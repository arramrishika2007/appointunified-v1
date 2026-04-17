package com.appointunified.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private UUID appointmentId;
    private UUID senderId;
    private String senderName;
    private UUID receiverId;
    private String receiverName;
    private String message;
    private Boolean isRead;
    private OffsetDateTime sentAt;
    private Boolean filtered;
    private String filterReason;
}
