package com.example.api.config

import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Jasypt 설정값 암호화
 *
 * yml 에 `ENC(암호문)` 으로 적은 값을 시작할 때 복호화한다. 암호문은 `./gradlew jasyptEncrypt --args="<비밀번호> <평문>"` 으로 만든다.
 * 비밀번호는 `JASYPT_ENCRYPTOR_PASSWORD` 환경변수로 주입한다(local 은 environment-local.yml 의 local-default).
 * 랜덤 Salt/IV 를 쓰므로 같은 평문도 매번 다른 암호문이 나오고, 모두 복호화된다.
 */
@Configuration(proxyBeanMethods = false)
class JasyptConfig {
    @Bean(BEAN_NAME)
    fun jasyptStringEncryptor(
        @Value("\${jasypt.encryptor.password:}") password: String,
    ): StringEncryptor =
        PooledPBEStringEncryptor().apply {
            setConfig(
                SimpleStringPBEConfig().apply {
                    setPassword(password)
                    algorithm = "PBEWITHHMACSHA512ANDAES_256"
                    setKeyObtentionIterations("1000")
                    setPoolSize("1")
                    setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
                    setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator")
                    stringOutputType = "base64"
                },
            )
        }

    companion object {
        /** application.yml 의 jasypt.encryptor.bean 과 같아야 한다 */
        const val BEAN_NAME = "jasyptStringEncryptor"
    }
}
