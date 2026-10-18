package com.hmp.sales.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Implementation của {@link PermissionEvaluationService}.
 * <p>
 * Đọc quyền trực tiếp từ danh sách {@link GrantedAuthority} trong {@link Authentication}.
 * Quyền được embedded vào JWT claims khi đăng nhập — không query DB mỗi request.
 * <p>
 * Khi Admin thay đổi quyền của một vai trò, hệ thống sẽ revoke toàn bộ access token
 * và refresh token của các user thuộc vai trò đó, buộc họ đăng nhập lại để nhận JWT
 * mới với quyền cập nhật. Xem {@code architecture.md} section 18.
 */
@Service("permEval")
public class PermissionEvaluationServiceImpl implements PermissionEvaluationService {

    private static final String PRODUCT_PRICE_VIEW = "PRODUCT_PRICE_VIEW";

    /**
     * {@inheritDoc}
     * <p>
     * Tìm kiếm {@code permissionName} trong danh sách {@code GrantedAuthority} của authentication.
     * Spring Security lưu authorities dưới dạng chuỗi (ví dụ: {@code "PRODUCT_PRICE_VIEW"}).
     *
     * @param authentication đối tượng xác thực hiện tại
     * @param permissionName tên quyền cần kiểm tra
     * @return {@code true} nếu quyền có trong danh sách authorities
     */
    @Override
    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permissionName::equals);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ủy thác kiểm tra cho {@link #hasPermission(Authentication, String)}
     * với quyền {@code PRODUCT_PRICE_VIEW}.
     */
    @Override
    public boolean canViewProductPrice(Authentication authentication) {
        return hasPermission(authentication, PRODUCT_PRICE_VIEW);
    }
}
