package org.example.expert.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void 유저_조회_성공() throws Exception {
        // given
        long userId = 1L;
        UserResponse response = new UserResponse(userId, "test@test.com");
        given(userService.getUser(anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void 비밀번호_변경_성공() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword123", "newPassword123");

        // @Auth 어노테이션이 ArgumentResolver에 의해 처리되므로
        // 테스트 시에는 request attribute에 "authUser"를 심어줘야 합니다.
        AuthUser authUser = new AuthUser(1L, "test@test.com", UserRole.USER);

        // when & then
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("userId", authUser.getId())
                        .requestAttr("email", authUser.getEmail())
                        .requestAttr("userRole", authUser.getUserRole().name()))
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호_변경_시_입력값이_유효하지_않으면_400을_반환한다() throws Exception {
        // given: 빈 비밀번호 전송
        UserChangePasswordRequest request = new UserChangePasswordRequest("", "");

        // when & then
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}