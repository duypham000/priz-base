package com.priz.base.application.features.users;

import com.priz.base.application.features.users.dto.ChangePasswordRequest;
import com.priz.base.application.features.users.dto.UpdateProfileRequest;
import com.priz.base.application.features.users.dto.UserDetailResponse;

public interface UserService {

    UserDetailResponse getUserDetail();

    UserDetailResponse updateProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

    void deleteAccount();
}
