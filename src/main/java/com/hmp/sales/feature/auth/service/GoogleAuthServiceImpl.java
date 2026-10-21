package com.hmp.sales.feature.auth.service;

import com.hmp.sales.common.exception.UnauthorizedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Triển khai xác minh Google ID Token bằng cách gọi Google TokenInfo endpoint.
 * <p>
 * Sử dụng Spring Boot {@link RestClient} — nhẹ, không cần thư viện Google API Client.
 * Gọi {@code https://oauth2.googleapis.com/tokeninfo?id_token={credential}}.
 * <p>
 * Không cache kết quả — mỗi lần đăng nhập đều xác minh trực tiếp với Google.
 * Tuyệt đối không log giá trị idToken trong bất kỳ tình huống nào.
 */
@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthServiceImpl.class);
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestClient restClient;

    /**
     * Khởi tạo service với RestClient mặc định trỏ tới Google TokenInfo endpoint.
     */
    public GoogleAuthServiceImpl() {
        this.restClient = RestClient.builder().baseUrl(GOOGLE_TOKENINFO_URL).build();
    }

    /**
     * Khởi tạo service với RestClient tùy chỉnh (dùng cho testing hoặc mở rộng).
     *
     * @param restClient RestClient instance đã cấu hình
     */
    public GoogleAuthServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gọi Google TokenInfo API, trích xuất các field: {@code sub}, {@code email}, {@code name}.
     * Nếu response thiếu {@code sub} hoặc {@code email}, coi là token không hợp lệ.
     */
    @Override
    @SuppressWarnings("unchecked")
    public GoogleUserPayload verifyGoogleToken(String idToken) {
        try {
            Map<String, Object> tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(Map.class);

            if (tokenInfo == null) {
                throw new UnauthorizedOperationException("Google TokenInfo trả về phản hồi trống");
            }

            String sub = (String) tokenInfo.get("sub");
            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.getOrDefault("name", email);

            if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
                throw new UnauthorizedOperationException("Google ID Token thiếu thông tin sub hoặc email");
            }

            return new GoogleUserPayload(sub, email, name);
        } catch (UnauthorizedOperationException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Lỗi khi gọi Google TokenInfo API: {}", e.getMessage());
            throw new UnauthorizedOperationException("Không thể xác minh Google ID Token. Vui lòng thử lại.");
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi xác minh Google token", e);
            throw new UnauthorizedOperationException("Lỗi hệ thống khi xác minh Google token");
        }
    }
}
