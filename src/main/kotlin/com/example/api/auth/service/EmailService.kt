package com.example.api.auth.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 메일 내용 설정 (`mail.*`). SMTP 접속 정보는 Spring Boot 의 `spring.mail.*`
 *
 * @property verifyUrl 이메일 인증 링크. FE 라우트와 같아야 한다
 * @property passwordResetUrl 비밀번호 재설정 링크. FE 라우트와 같아야 한다
 */
@ConfigurationProperties("mail")
data class MailProperties(
    val from: String,
    val verifyUrl: String,
    val passwordResetUrl: String,
)

/**
 * 메일 발송
 *
 * 응답을 기다리게 하지 않도록 비동기로 보내고, 발송 실패는 로그만 남긴다(가입·재설정 요청 자체는 성공).
 * 로그에는 이메일 대신 메일 종류만 남긴다.
 */
@Service
@EnableConfigurationProperties(MailProperties::class)
class EmailService(
    private val mailSender: JavaMailSender,
    private val properties: MailProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendVerificationEmail(
        email: String,
        token: String,
    ) {
        send(
            to = email,
            subject = "[Boilerplate] 이메일 인증을 완료해주세요",
            body =
                """
                <p>아래 링크를 클릭하여 이메일 인증을 완료해주세요.</p>
                <p><a href="${properties.verifyUrl}?token=$token">이메일 인증하기</a></p>
                <p>링크는 24시간 동안 유효합니다.</p>
                """.trimIndent(),
        )
    }

    @Async
    fun sendPasswordResetEmail(
        email: String,
        token: String,
    ) {
        send(
            to = email,
            subject = "[Boilerplate] 비밀번호를 재설정해주세요",
            body =
                """
                <p>아래 링크를 클릭하여 비밀번호를 재설정해주세요.</p>
                <p><a href="${properties.passwordResetUrl}?token=$token">비밀번호 재설정하기</a></p>
                <p>링크는 30분 동안 유효합니다.</p>
                """.trimIndent(),
        )
    }

    private fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        try {
            val message = mailSender.createMimeMessage()
            MimeMessageHelper(message, false, Charsets.UTF_8.name()).apply {
                setFrom(properties.from)
                setTo(to)
                setSubject(subject)
                setText(body, true)
            }
            mailSender.send(message)
            log.debug("메일 발송 완료: {}", subject)
        } catch (e: MailException) {
            log.error("메일 발송 실패: {} - {}", subject, e.message)
        }
    }
}
