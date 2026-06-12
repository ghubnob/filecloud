package dev.vivim.filecloud.mock;

import dev.vivim.filecloud.configuration.SecurityConfig;
import dev.vivim.filecloud.controller.ResourceController;
import dev.vivim.filecloud.service.FileService;
import dev.vivim.filecloud.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResourceController.class)
@Import(SecurityConfig.class)
public class UsersTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;
    @MockitoBean FileService fileService;

    @Test
    @WithAnonymousUser
    void shouldThrowWhenHaveNotSession() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user")
    void shouldSuccessWhenHaveSession() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk());
    }
}
