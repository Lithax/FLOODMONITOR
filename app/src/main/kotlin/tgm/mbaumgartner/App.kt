package tgm.mbaumgartner

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Instant

@OpenAPIDefinition(
    info = Info(
        title = "Flood Monitor API",
        version = "v1",
        description = "REST API of the flood early-warning system. " +
            "Responses are JSON by default; send `Accept: application/xml` to get XML. " +
            "Unsupported formats return 406. Errors use the same ErrorResponse structure in both formats. " +
            "Authentication: none, all endpoints are public and read-only."
    )
)
@SpringBootApplication
@EnableScheduling
class App {
    init {
        // document kotlin.time.Instant as an ISO-8601 date-time string
        SpringDocUtils.getConfig().replaceWithClass(Instant::class.java, java.time.Instant::class.java)
    }
}

fun main(args: Array<String>) {
    
    SpringApplication.run(App::class.java, *args)
}