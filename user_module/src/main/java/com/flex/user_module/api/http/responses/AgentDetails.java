package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentDetails {
    private Integer agentId;
    private String agentName;
    private String agentEmail;
    private Integer centerId;
    private String centerName;
}
