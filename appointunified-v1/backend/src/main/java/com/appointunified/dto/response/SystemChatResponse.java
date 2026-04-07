package com.appointunified.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemChatResponse {
    private String question;
    private String answer;
    private String status;  // 'SUCCESS', 'ERROR', 'OFFLINE'
}
