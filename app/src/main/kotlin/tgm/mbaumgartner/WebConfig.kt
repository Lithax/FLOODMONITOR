package tgm.mbaumgartner

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Instant

// Spring has no built-in String -> kotlin.time.Instant conversion for query parameters (from, to).
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(String::class.java, Instant::class.java) { Instant.parse(it) }
    }
}
