package com.example.api.user.controller

import com.example.api.common.response.BaseResponse
import com.example.api.user.dto.MeResponse
import com.example.api.user.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 회원 API
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) : UserControllerDocs {
    @GetMapping("/me")
    override fun getMe(authentication: Authentication): BaseResponse<MeResponse> =
        BaseResponse.ok(userService.getMe(authentication.name))
}
