package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void 비밀번호_변경_성공() {
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPass", "newPass123");
        User user = new User("a@a.com", "encodedOld", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(false); // 새 비번이 기존과 다름
        given(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).willReturn(true); // 기존 비번 일치
        given(passwordEncoder.encode(request.getNewPassword())).willReturn("encodedNew");

        assertDoesNotThrow(() -> userService.changePassword(userId, request));
    }

    @Test
    void 비밀번호_변경_시_기존_비밀번호가_틀리면_예외가_발생한다() {
        long userId = 1L;
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongOld", "newPass123");
        User user = new User("a@a.com", "encodedOld", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(any(), any())).willReturn(false);

        assertThrows(InvalidRequestException.class, () -> userService.changePassword(userId, request));
    }

    @Test
    void 비밀번호_변경_시_새_비밀번호가_기존_비밀번호와_같으면_예외가_발생한다() {
        // given
        long userId = 1L;
        // 기존 비밀번호와 새 비밀번호를 동일하게 설정
        String samePassword = "samePassword123";
        UserChangePasswordRequest request = new UserChangePasswordRequest(samePassword, samePassword);

        User user = new User("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 💡 핵심: passwordEncoder.matches가 true를 리턴하도록 설정 (기존 비번과 새 비번이 같다는 상황)
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(true);

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 유저_단건_조회에_성공한다() {
        // given
        long userId = 1L;
        User user = new User("test@example.com", "password", UserRole.USER);
        // Entity에 ID가 없으면 Response 생성 시 null이 나오므로 Reflection으로 ID를 심어줍니다.
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse result = userService.getUser(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void 유저_조회_시_유저가_없으면_InvalidRequestException이_발생한다() {
        // given
        long userId = 1L;
        // 유저를 찾지 못하는 상황(Optional.empty)을 가정합니다.
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userService.getUser(userId);
        });

        assertEquals("User not found", exception.getMessage());
    }
}