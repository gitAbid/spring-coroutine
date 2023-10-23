package com.abid.springcoroutine

import jakarta.annotation.PostConstruct
import jakarta.persistence.Entity
import jakarta.persistence.Id
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SpringBootApplication
class SpringCoroutineApplication

fun main(args: Array<String>) {
    runApplication<SpringCoroutineApplication>(*args)
}


@Configuration
class Config {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    fun jdbcTemplate(): JdbcTemplate {
        return JdbcTemplate()
    }

}

@RestController
@RequestMapping("/")
class Controller(
    val restTemplate: RestTemplate,
    val repository: UserRepository,
    val jdbcTemplate: JdbcTemplate
) {
    val logger = LoggerFactory.getLogger(Controller::class.java)

    @PostConstruct
    fun init() {
//        jdbcTemplate.update(
//            """
//            CREATE TABLE "user" (
//                name VARCHAR(255)  NOT NULL,
//                age INT NOT NULL
//            );
//            insert into "user" (name, age) values ('abid', 30);
//            insert into "user" (name, age) values ('jeba', 30);
//        """.trimIndent()
//        )
    }

    @GetMapping
    suspend fun index() {
        val value = withContext(Dispatchers.IO) {
            restTemplate.getForObject("https://www.google.com", String::class.java)
        }
        logger.info(value)
        logger.info("Hello World")
    }

    @GetMapping("/callback")
    suspend fun handleCallback(): String {
        logger.info("inside handleCallback")
        return suspendCancellableCoroutine { continuation ->
            val callback = object : Callback {
                override fun success() {
                    logger.info("inside callback onSuccess continuation.resume")
                    continuation.resume("success")
                }

                override fun error() {
                    logger.info("inside callback onError continuation.resumeWithException")
                    continuation.resumeWithException(Exception("error"))
                }
            }
            restTemplate.getForObject("https://www.google.com", String::class.java).let {
                it?.let {
                    logger.info("inside callback onSuccess")
                    callback.success()
                } ?: run {
                    logger.info("inside callback onError")
                    callback.error()
                }
            }

        }
    }

    @GetMapping("/repository")
    suspend fun handleRepository(): User? {
        return runBlocking(Dispatchers.IO) {
            repository.findByName("abid")
        }
    }


}

@Repository
interface UserRepository : CrudRepository<User, String> {
    suspend fun findByName(name: String): User?
}

@Entity
data class User(
    @Id
    val name: String? = "",
    val age: Int? = 0
)

interface Callback {
    fun success()
    fun error()
}