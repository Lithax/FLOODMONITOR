# API documentation (OpenAPI)

The API is documented with OpenAPI (springdoc). Start the app (`./gradlew bootRun`) and open:

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| OpenAPI spec (JSON) | http://localhost:8080/api/v1/api-docs |

The spec covers all endpoints, parameters, JSON and XML response bodies, status codes (200, 400, 404, 406, 500)
and the `ErrorResponse` structure. Authentication: none, all endpoints are public and read-only.
