package org.example.expert.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminAccessLoggingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository; // 유저 저장을 위해 추가

    @BeforeEach
    void setUp() {
        // 테스트 실행 전, ID가 1인 유저를 실제 DB(H2)에 넣어줍니다.
        User user = new User("admin@test.com", "password", UserRole.USER);
        // 만약 ID가 자동으로 1이 부여되지 않을까봐 걱정된다면
        // 기존 데이터를 다 지우고 새로 저장하게 합니다.
        userRepository.deleteAll();
        userRepository.save(user);
    }

    @Test
    void 어드민_API_호출_시_AOP_로그가_남는지_확인한다() throws Exception {
        // given
        // DB에 방금 저장된 유저의 실제 ID를 가져옵니다.
        User savedUser = userRepository.findAll().get(0);
        long targetUserId = savedUser.getId();

        UserRoleChangeRequest requestDto = new UserRoleChangeRequest("ADMIN");
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // when & then
        mockMvc.perform(patch("/admin/users/" + targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .requestAttr("userId", 100L)) // AOP 로깅용
                .andExpect(status().isOk());
    }

    @Test
    void 어드민_댓글_삭제_API_호출_시_AOP_로그가_남는지_확인한다() throws Exception {
        // given
        long commentId = 1L;

        // when & then
        mockMvc.perform(delete("/admin/comments/{commentId}", commentId)
                        .requestAttr("userId", 100L)      // AOP에서 읽어갈 요청자 ID
                        .requestAttr("userRole", "ADMIN")) // 어드민 권한 설정
                .andExpect(status().isOk());

        // 💡 실행 후 콘솔에서 ::: Admin API Access Log ::: 가 찍혔는지 확인하세요!
    }
}