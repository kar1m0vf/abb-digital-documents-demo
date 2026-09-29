package az.abb.embassyflow.auth.filter;

import az.abb.embassyflow.common.web.AuthAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DemoBearerTokenFilter extends OncePerRequestFilter {

    private static final String CUSTOMER_PREFIX = "Bearer demo-token-customer-";
    private static final String PORTAL_PREFIX = "Bearer demo-portal-token-";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (header.startsWith(CUSTOMER_PREFIX)) {
                parse(request, header.substring(CUSTOMER_PREFIX.length()), AuthAttributes.CUSTOMER_ID);
            } else if (header.startsWith(PORTAL_PREFIX)) {
                parse(request, header.substring(PORTAL_PREFIX.length()), AuthAttributes.PORTAL_USER_ID);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void parse(HttpServletRequest request, String tokenValue, String attribute) {
        try {
            request.setAttribute(attribute, Long.parseLong(tokenValue.trim()));
        } catch (NumberFormatException ignored) {
            // invalid demo token -> treated as anonymous
        }
    }
}