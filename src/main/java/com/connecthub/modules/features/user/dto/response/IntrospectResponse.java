
package com.connecthub.modules.features.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectResponse {
    private boolean active;
    private String scope;
    private String clientId;
    private String username;
    private long exp;
    private long iat;
    private String sub;
}
