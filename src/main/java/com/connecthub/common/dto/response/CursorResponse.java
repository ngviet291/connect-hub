package com.connecthub.common.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CursorResponse<T> {
    private List<T> content;
    private boolean hasNext;
    private String nextCursor;

}
