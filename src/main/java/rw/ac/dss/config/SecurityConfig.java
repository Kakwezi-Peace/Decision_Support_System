package rw.ac.dss.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import rw.ac.dss.security.CustomUserDetailsService;
import rw.ac.dss.security.JwtAuthenticationFilter;

import java.io.IOException;

/**
 * Stateless JWT-based security with role-based access control.
 *
 * Read access (GET) is open to any authenticated user regardless of role - every
 * OCC role needs visibility across the operation to do their job. Write access
 * (POST/PUT/DELETE) is scoped to the role that owns that data domain, matching the
 * OCC staff categories in the research proposal's Table 1:
 *   - Aircraft            -> MAINTENANCE_CONTROLLER
 *   - Crew                -> CREW_SCHEDULER
 *   - Flights, Delay      -> OPERATIONS_CONTROLLER
 *     Events, Recovery
 *     workflow (optimize/select)
 *   - Passengers          -> COMMERCIAL_SERVICES (rebooking, compensation, connections)
 *   - User accounts/roles -> ADMIN only ("the admin should be the one giving them
 *     permissions")
 * SENIOR_MANAGEMENT has no write domain in the current screens, so it gets
 * read-only access everywhere - matching its oversight/informational role in the
 * proposal rather than an operational one.
 * ADMIN can do everything, including managing accounts and roles.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        .requestMatchers("/api/auth/users", "/api/auth/users/**").hasRole("ADMIN")

                        // Aircraft - owned by MAINTENANCE_CONTROLLER
                        .requestMatchers(HttpMethod.POST, "/api/aircraft").hasAnyRole("ADMIN", "MAINTENANCE_CONTROLLER")
                        .requestMatchers(HttpMethod.PUT, "/api/aircraft/**").hasAnyRole("ADMIN", "MAINTENANCE_CONTROLLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/aircraft/**").hasAnyRole("ADMIN", "MAINTENANCE_CONTROLLER")

                        // Crew - owned by CREW_SCHEDULER
                        .requestMatchers(HttpMethod.POST, "/api/crew").hasAnyRole("ADMIN", "CREW_SCHEDULER")
                        .requestMatchers(HttpMethod.PUT, "/api/crew/**").hasAnyRole("ADMIN", "CREW_SCHEDULER")
                        .requestMatchers(HttpMethod.DELETE, "/api/crew/**").hasAnyRole("ADMIN", "CREW_SCHEDULER")

                        // Flights, delay events, and the recovery workflow - owned by OPERATIONS_CONTROLLER
                        .requestMatchers(HttpMethod.POST, "/api/flights").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.PUT, "/api/flights/**").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/flights/**").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.POST, "/api/delay-events").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.PUT, "/api/delay-events/**").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/delay-events/**").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")
                        .requestMatchers(HttpMethod.POST, "/api/recovery/**").hasAnyRole("ADMIN", "OPERATIONS_CONTROLLER")

                        // Passengers - owned by COMMERCIAL_SERVICES
                        .requestMatchers(HttpMethod.POST, "/api/passengers").hasAnyRole("ADMIN", "COMMERCIAL_SERVICES")
                        .requestMatchers(HttpMethod.PUT, "/api/passengers/**").hasAnyRole("ADMIN", "COMMERCIAL_SERVICES")
                        .requestMatchers(HttpMethod.DELETE, "/api/passengers/**").hasAnyRole("ADMIN", "COMMERCIAL_SERVICES")

                        // Everything else (all GET reads) just requires being logged in
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Authentication required."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.")))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Writes the status directly via setStatus (not sendError) so the container does
     * NOT perform its default forward to /error - that forward re-enters the security
     * filter chain on the same request without re-running the once-per-request JWT
     * filter, which was silently overwriting a correct 403 with a stray 401.
     */
    private static void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\"}");
    }
}
