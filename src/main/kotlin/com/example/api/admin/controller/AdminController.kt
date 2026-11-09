package com.example.api.admin.controller

import com.example.api.admin.dto.AdminMemberResponse
import com.example.api.admin.service.AdminService
import com.example.api.common.response.BaseResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 API [예제: admin]
 *
 * 권한은 메서드마다 `@PreAuthorize` 로 건다. 거부되면 ExceptionAdvice 가 403 PERMISSION_FORBIDDEN 으로 응답한다.
 */
@RestController
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService,
) : AdminControllerDocs {
    @GetMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    override fun getMemberList(): BaseResponse<List<AdminMemberResponse>> =
        BaseResponse.ok(adminService.getMemberList())
}
