package ai.typesafe.voicebrowser.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Filter that protects the Voice Browser control dashboard (/, /index.html) and portal (/portal).
 * If the user has not authenticated, they are automatically redirected to /login.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        int semicolon = path.indexOf(';');
        if (semicolon != -1) {
            path = path.substring(0, semicolon);
        }

        // Protect root dashboard, dashboard, index.html, and portal
        boolean isProtected = "/".equals(path) || "/dashboard".equals(path) || "/index.html".equals(path) || "/portal".equals(path);

        if (isProtected) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("currentUser") == null) {
                response.sendRedirect("/login");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
