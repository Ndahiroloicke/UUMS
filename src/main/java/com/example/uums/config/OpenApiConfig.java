package com.example.uums.config;

/**
 * OpenAPI / Swagger UI configuration for API documentation.
 * Defines API title, description (WASAC/REG Utility Billing System), server URL,
 * and JWT Bearer authentication scheme so endpoints can be tested from Swagger UI.
 */
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "WASAC/REG Utility Billing System API",
                version = "1.0",
                description = "Backend API for Water and Sanitation Corporation (WASAC) and Rwanda Energy Group (REG) " +
                              "utility billing system. Manages customers, meters, readings, tariffs, bills, payments, " +
                              "and notifications.",
                contact = @Contact(name = "WASAC/REG IT Team", email = "it@wasac.rw")
        ),
        servers = @Server(url = "http://localhost:8080", description = "Local Development Server"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT token obtained from /api/auth/login"
)
public class OpenApiConfig {
}
