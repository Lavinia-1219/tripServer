package com.tripcompanion.auth;

import com.tripcompanion.poi.PoiScorer;
import com.tripcompanion.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "不能为空")
            @Size(min = 3, max = 40, message = "长度需要在 3~40 之间")
            String username,

            @NotBlank(message = "不能为空")
            @Size(min = 6, max = 64, message = "长度需要在 6~64 之间")
            String password,

            @Size(max = 40, message = "最多 40 个字符")
            String nickname
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "不能为空") String username,
            @NotBlank(message = "不能为空") String password
    ) {
    }

    public record UserView(Long id, String username, String nickname, List<String> preferences) {
        public static UserView from(User user) {
            return new UserView(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    PoiScorer.parsePreferences(user.getPreferences()));
        }
    }

    public record PreferenceRequest(
            String preferences
    ) {
    }

    public record AuthResult(String token, UserView user) {
    }
}
