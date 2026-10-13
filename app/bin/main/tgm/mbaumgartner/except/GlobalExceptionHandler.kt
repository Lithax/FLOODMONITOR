package tgm.mbaumgartner.except

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import kotlin.time.Clock
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(
        ex: NoSuchElementException, 
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Resource not found",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException, 
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val targetType = ex.requiredType?.simpleName ?: "valid value"
        val message = "Parameter '${ex.name}' has invalid value '${ex.value}'. Expected type: $targetType."
        
        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(BindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBind(
        ex: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val message = ex.fieldErrors
            .joinToString("; ") { "Parameter '${it.field}' has invalid value '${it.rejectedValue}'." }
            .ifEmpty { "Invalid request parameters" }

        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(
        ex: IllegalArgumentException, 
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Invalid request parameters",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    fun handleNotAcceptable(
        ex: HttpMediaTypeNotAcceptableException, 
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_ACCEPTABLE.value(),
            error = HttpStatus.NOT_ACCEPTABLE.reasonPhrase,
            message = "Requested format is not supported. Use application/json or application/xml.",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(error)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneralException(
        ex: Exception, 
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}

@JacksonXmlRootElement(localName = "errorResponse")
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)
