package com.example.api.config

import com.ulisesbocchio.jasyptspringbootstarter.JasyptSpringBootAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.jasypt.exceptions.EncryptionOperationNotPossibleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class JasyptConfigTest {
    private val encryptor = JasyptConfig().jasyptStringEncryptor(PASSWORD)

    @Test
    fun `같은 평문도 매번 다른 암호문이 나오고 모두 복호화된다`() {
        val first = encryptor.encrypt(PLAIN)
        val second = encryptor.encrypt(PLAIN)

        assertThat(first).isNotEqualTo(second).isNotEqualTo(PLAIN)
        assertThat(encryptor.decrypt(first)).isEqualTo(PLAIN)
        assertThat(encryptor.decrypt(second)).isEqualTo(PLAIN)
    }

    @Test
    fun `다른 비밀번호로는 복호화할 수 없다`() {
        val encrypted = encryptor.encrypt(PLAIN)

        assertThrows<EncryptionOperationNotPossibleException> {
            JasyptConfig().jasyptStringEncryptor("wrong-password").decrypt(encrypted)
        }
    }

    @Test
    fun `설정의 ENC 값은 스프링 환경에서 평문으로 읽힌다`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JasyptSpringBootAutoConfiguration::class.java))
            .withUserConfiguration(JasyptConfig::class.java)
            .withPropertyValues(
                "jasypt.encryptor.bean=${JasyptConfig.BEAN_NAME}",
                "jasypt.encryptor.password=$PASSWORD",
                "app.secret=ENC(${encryptor.encrypt(PLAIN)})",
            ).run { context ->
                assertThat(context.environment.getProperty("app.secret")).isEqualTo(PLAIN)
            }
    }

    companion object {
        private const val PASSWORD = "local-default"
        private const val PLAIN = "my-db-password"
    }
}
