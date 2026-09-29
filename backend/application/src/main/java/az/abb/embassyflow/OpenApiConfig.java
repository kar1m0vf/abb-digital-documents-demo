package az.abb.embassyflow;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final Map<String, Set<HttpMethod>> PROTECTED = Map.of(
        "/api/v1/orders/{orderId}/identity", Set.of(HttpMethod.PUT),
        "/api/v1/customers/{customerId}/accounts", Set.of(HttpMethod.GET),
        "/api/v1/orders/{orderId}/items", Set.of(HttpMethod.POST),
        "/api/v1/orders/{orderId}", Set.of(HttpMethod.GET),
        "/api/v1/orders/{orderId}/preview", Set.of(HttpMethod.POST),
        "/api/v1/orders/{orderId}/pay", Set.of(HttpMethod.POST),
        "/api/v1/orders", Set.of(HttpMethod.GET)
    );

    private static final Map<String, Set<HttpMethod>> PORTAL_PROTECTED = Map.of(
        "/api/v1/portal/stats", Set.of(HttpMethod.GET),
        "/api/v1/portal/documents", Set.of(HttpMethod.GET),
        "/api/v1/portal/documents/{documentNumber}", Set.of(HttpMethod.GET),
        "/api/v1/portal/documents/{documentNumber}/status", Set.of(HttpMethod.PUT),
        "/api/v1/portal/documents/{documentNumber}/download", Set.of(HttpMethod.GET)
    );

    @Bean
    public OpenApiCustomizer bearerSecurityCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.components(components);
            }
            components.addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("demo-token-customer-{id}")
                    .description("OTP yoxlanmasından (POST /auth/otp/validate) qayıdan "
                        + "accessToken-ı yapışdırın, məs. demo-token-customer-1. "
                        + "'Bearer ' prefiksi özü əlavə olunur."));
            components.addSecuritySchemes("portalAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("demo-portal-token-{id}")
                    .description("Portal login-dən (POST /portal/auth/login) qayıdan "
                        + "accessToken-ı yapışdırın, məs. demo-portal-token-1."));
            openApi.getPaths().forEach((path, item) -> protect(item, PROTECTED.get(path), "bearerAuth"));
            openApi.getPaths().forEach((path, item) -> protect(item, PORTAL_PROTECTED.get(path), "portalAuth"));
        };
    }

    private static void protect(io.swagger.v3.oas.models.PathItem item, Set<HttpMethod> methods, String scheme) {
        if (methods == null) {
            return;
        }
        item.readOperationsMap().forEach((method, op) -> {
            if (methods.contains(method)) {
                op.addSecurityItem(new SecurityRequirement().addList(scheme));
            }
        });
    }
}
