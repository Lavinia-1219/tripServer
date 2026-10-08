package com.tripcompanion.auth;

import com.tripcompanion.common.ApiException;
import com.tripcompanion.poi.PoiScorer;
import com.tripcompanion.user.User;
import com.tripcompanion.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long tokenTtlDays;

    public AuthService(UserRepository userRepository,
                       AuthTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.auth.token-ttl-days:30}") long tokenTtlDays) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenTtlDays = tokenTtlDays;
    }

    @Transactional
    public AuthDtos.AuthResult register(String username, String rawPassword, String nickname) {
        String name = username.trim();
        if (userRepository.existsByUsername(name)) {
            throw ApiException.badRequest("USERNAME_TAKEN", "这个用户名已经被注册了");
        }
        String display = (nickname == null || nickname.isBlank()) ? name : nickname.trim();
        User user = new User(name, passwordEncoder.encode(rawPassword), display);
        userRepository.save(user);
        return issueToken(user);
    }

    @Transactional
    public AuthDtos.AuthResult login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> ApiException.badRequest("BAD_CREDENTIALS", "用户名或密码不对"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("BAD_CREDENTIALS", "用户名或密码不对");
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public Long resolveUserId(String token) {
        return tokenRepository.findById(token)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(AuthToken::getUserId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("用户不存在"));
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokenRepository.deleteById(token);
        }
    }

    @Transactional
    public AuthDtos.UserView updatePreferences(Long userId, String rawPreferences) {
        User user = requireUser(userId);

        List<String> tags = new ArrayList<>();
        if (rawPreferences != null && !rawPreferences.isBlank()) {
            for (String part : rawPreferences.split(",")) {
                String tag = part.trim();
                if (tag.isEmpty()) {
                    continue;
                }
                if (!PoiScorer.allPreferences().contains(tag)) {
                    throw ApiException.badRequest("BAD_PREFERENCE",
                            "不认识的偏好标签：" + tag + "。可用：" + PoiScorer.allPreferences());
                }
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }

        user.setPreferences(tags.isEmpty() ? null : String.join(",", tags));
        return AuthDtos.UserView.from(userRepository.save(user));
    }

    private AuthDtos.AuthResult issueToken(User user) {
        String token = generateToken();
        tokenRepository.save(new AuthToken(
                token,
                user.getId(),
                LocalDateTime.now().plusDays(tokenTtlDays)
        ));
        return new AuthDtos.AuthResult(token, AuthDtos.UserView.from(user));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
