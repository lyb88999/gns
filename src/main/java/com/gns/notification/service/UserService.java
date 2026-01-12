package com.gns.notification.service;

import com.gns.notification.dto.PageResult;
import com.gns.notification.dto.UserRequest;
import com.gns.notification.dto.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    UserResponse getUser(Long id);

    PageResult<UserResponse> listUsers(Pageable pageable);

    void deleteUser(Long id);
}
