package org.example.expert.domain.auth.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    @Test
    void 회원가입_성공() {
        SignupRequest request = new SignupRequest("test@test.com", "password", "USER");
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(request);
        assertNotNull(response);
    }

    @Test
    void 이미_존재하는_이메일로_가입하면_예외가_발생한다() {
        SignupRequest request = new SignupRequest("test@test.com", "password", "USER");
        given(userRepository.existsByEmail(any())).willReturn(true);

        assertThrows(InvalidRequestException.class, () -> authService.signup(request));
    }

    @Test
    void 로그인_성공() {
        SigninRequest request = new SigninRequest("test@test.com", "password");
        User user = new User("test@test.com", "encoded", UserRole.USER);

        given(userRepository.findByEmail(any())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(any(), any())).willReturn(true);
        given(jwtUtil.createToken(any(), any(), any())).willReturn("token");

        SigninResponse response = authService.signin(request);
        assertNotNull(response);
    }

    @Test
    void 로그인_시_가입되지_않은_이메일이면_InvalidRequestException이_발생한다() {
        // given
        SigninRequest request = new SigninRequest("nonexistent@test.com", "password");
        // 이메일로 유저를 찾았을 때 결과가 없음(empty)을 리턴하도록 설정
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                authService.signin(request)
        );
        assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
    }

    @Test
    void 로그인_시_비밀번호가_틀리면_AuthException이_발생한다() {
        // given
        SigninRequest request = new SigninRequest("test@test.com", "wrongPassword");
        User user = new User("test@test.com", "encodedPassword", UserRole.USER);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        // passwordEncoder.matches가 false를 리턴하도록 설정
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

        // when & then
        // AuthException이 발생하는지 확인 (import 주의: org.example.expert.domain.common.exception.AuthException)
        AuthException exception = assertThrows(AuthException.class, () ->
                authService.signin(request)
        );
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }
}