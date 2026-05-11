package com.priz.base.application.features.apikey;

import com.priz.base.application.features.apikey.dto.ApiKeyAuthResult;

public interface ApiKeyAuthService {

    /**
     * Xác thực API key từ request.
     *
     * @param rawApiKey  Giá trị header X-API-KEY ({code}.{rawKey})
     * @param clientIp   IP của client
     * @param signature  HMAC signature (null nếu không có)
     * @param timestamp  Timestamp gửi kèm signature (ms)
     * @param recvWindow Cửa sổ thời gian chấp nhận (ms)
     * @param method     HTTP method (cho HMAC)
     * @param path       Request path (cho HMAC)
     * @param queryStr   Query string đã sort (cho HMAC)
     * @param body       Request body raw (cho HMAC)
     */
    ApiKeyAuthResult validate(
            String rawApiKey,
            String clientIp,
            String signature,
            Long timestamp,
            long recvWindow,
            String method,
            String path,
            String queryStr,
            String body
    );
}
