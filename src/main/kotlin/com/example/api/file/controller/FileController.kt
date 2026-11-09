package com.example.api.file.controller

import com.example.api.common.response.BaseResponse
import com.example.api.file.dto.PresignedUrlResponse
import com.example.api.file.service.FileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 파일 API [예제: file-upload]
 */
@RestController
@RequestMapping("/files")
class FileController(
    private val fileService: FileService,
) : FileControllerDocs {
    @GetMapping("/presigned-url")
    override fun getPresignedUrl(
        @RequestParam fileName: String,
        @RequestParam contentType: String,
    ): BaseResponse<PresignedUrlResponse> = BaseResponse.ok(fileService.getPresignedUrl(fileName, contentType))
}
