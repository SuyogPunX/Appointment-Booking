package com.spring.appointment.config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.spring.appointment.service.JwtService;

import java.io.IOException;

//2nd step filter extends Once per request it filters  on every request fired by user
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    private final UserDetailsService userDetailsService; //interface in spring (next applicationConfig to implement our own methods)

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        //extracting user email by jwt service 3rd a step
        jwt = authHeader.substring(7);

        userEmail=jwtService.extractUserName(jwt);
        //check if user is not null so i have usename or i can extract email out
        //of my JWT token and i want to check if user is not authenticated yet if so i dont
        //need to  perform all the checks and updating security context , if it is authenticated i dont need to do all the process like So you need to perform authentication steps, e.g.,
        // validate token, load user details, create authentication object., then pass to dispatcher servlet (controllers)


        if(userEmail!=null && SecurityContextHolder.getContext().getAuthentication()==null ) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            if(jwtService.isTokenValid(jwt, userDetails)) {
               // Create an Authentication object
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
              // this   Adds information about the current request (e.g., IP address, session ID) Useful for logging or security checks.
                authenticationToken.setDetails(new WebAuthenticationDetails(request));
            // Store authentication in the SecurityContex
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);


    }
}
