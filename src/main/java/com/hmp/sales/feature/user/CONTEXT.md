# User & Identity Feature Context

## 1. Overview
Phân hệ quản lý định danh, người dùng, vai trò và phân quyền (Identity, Users, Roles & Permissions) trong hệ thống bán hàng HMP.

## 2. Entities & Schema Mapping
Các thực thể JPA đã triển khai tại `com.hmp.sales.feature.user.entity`:
- `Role`: Đại diện bảng `roles`. Định danh các vai trò chính: `ADMIN`, `MANAGER`, `CUSTOMER`.
- `Permission`: Đại diện bảng `permissions`. Quản lý quyền hạn động dựa trên `api_path`, `method` và đặc thù như `PRODUCT_PRICE_VIEW`.
- `RolePermission`: Đại diện bảng liên kết `role_permissions` với composite key `RolePermissionId`.
- `User`: Đại diện bảng `users`. Quản lý tài khoản hỗ trợ cả xác thực Google OAuth (`GOOGLE`) và xác thực nội bộ (`LOCAL`), trạng thái `ACTIVE` / `LOCKED`.

## 3. Enums
- `RoleCode`: `ADMIN`, `MANAGER`, `CUSTOMER`.
- `AuthProvider`: `GOOGLE`, `LOCAL`.
- `UserStatus`: `ACTIVE`, `LOCKED`.

## 4. Repositories
Các repository tại `com.hmp.sales.feature.user.repository`:
- `RoleRepository`: Truy vấn vai trò theo mã định danh `RoleCode`, lọc xóa mềm `isDeleted`.
- `PermissionRepository`: Truy vấn quyền hạn theo mã tên quyền, `apiPath` + `method`, và lọc theo module.
- `RolePermissionRepository`: Truy vấn quyền hạn gán cho vai trò (`findPermissionsByRoleId`), kiểm tra nhanh quyền hạn của vai trò (`hasPermission`), và lấy toàn bộ liên kết quyền hợp lệ eager load để nạp cache RAM (`findAllActiveRolePermissions`).
- `UserRepository`: Truy vấn người dùng theo `email`, `authProvider` + `providerSubject` (Google sub), eager fetch vai trò (`findByIdWithRole`, `findByEmailWithRole`).

## 5. Security & Authorization Integration
- `PermissionAuthorizationManager`: Singleton quản lý cache `rolePermissionsCache` trên RAM, kiểm tra quyền truy cập động bằng `AntPathMatcher` trên `(apiPath, method)`.
- `PermissionEvaluationServiceImpl`: Tích hợp tra cứu quyền từ cache RAM qua `PermissionAuthorizationManager`.
- `RoleServiceImpl`: Tự động gọi `loadCache()` khi Admin cập nhật quyền hạn cho vai trò.

## 6. Testing
Tất cả các repository và service được kiểm thử tự động với `@DataJpaTest` và Mockito:
- `RoleRepositoryTest` (3 tests)
- `PermissionRepositoryTest` (3 tests)
- `RolePermissionRepositoryTest` (3 tests)
- `UserRepositoryTest` (4 tests)
- `AdminUserServiceTest` (10 tests)
- `AdminRoleServiceTest` (3 tests)
- `PermissionAuthorizationManagerTest` (9 tests)
- `PermissionEvaluationServiceTest` (7 tests)
Toàn bộ 66/66 tests trong dự án đều PASS 100%.
