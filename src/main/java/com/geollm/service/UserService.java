package com.geollm.service;

import com.geollm.dto.user.ChangePassParam;
import com.geollm.dto.user.LoginParam;
import com.geollm.dto.user.SignUpParam;

public interface UserService {
    int signUp(SignUpParam p);

    String login(LoginParam p);

    int changePassword(ChangePassParam p);
}

