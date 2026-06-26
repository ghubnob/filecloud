package dev.vivim.filecloud.mock;

import dev.vivim.filecloud.configuration.SecurityConfig;
import dev.vivim.filecloud.controller.UserController;
import dev.vivim.filecloud.dto.AuthenticatedUser;
import dev.vivim.filecloud.service.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UsersTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean DatabaseUserDetailsService userDetailsService;
    @MockitoBean PasswordEncoder passwordEncoder;

    @Test
    @WithAnonymousUser
    void shouldThrowWhenHaveNotSession() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSuccessWhenHaveSession() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(null, "user", "password");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        mockMvc.perform(get("/api/user/me")
                        .with(securityContext(context)))
                .andExpect(status().isOk());
    }
}
