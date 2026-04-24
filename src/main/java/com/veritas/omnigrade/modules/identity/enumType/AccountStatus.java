package com.veritas.omnigrade.modules.identity.enumType;

public enum AccountStatus {
  NOT_ACTIVATED,
  UNVERIFIED, // Mới đăng ký, chưa đổi mật khẩu
  ACTIVE, // Đã duyệt, hoạt động bình thường
  BLOCKED, // Bị khóa (do vi phạm nội quy,...)
  INACTIVE // Tự khóa tài khoản (User request)
}
