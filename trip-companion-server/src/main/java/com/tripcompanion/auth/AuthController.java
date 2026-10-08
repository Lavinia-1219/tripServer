package com.tripcompanion.auth;

import com.tripcompanion.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResult> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ApiResponse.ok(authService.register(req.username(), req.password(), req.nickname()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthResult> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResponse.ok(authService.login(req.username(), req.password()));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me(@CurrentUser Long userId) {
        return ApiResponse.ok(AuthDtos.UserView.from(authService.requireUser(userId)));
    }

    @PatchMapping("/me/preferences")
    public ApiResponse<AuthDtos.UserView> updatePreferences(
            @CurrentUser Long userId,
            @RequestBody AuthDtos.PreferenceRequest req) {
        return ApiResponse.ok(authService.updatePreferences(userId, req.preferences()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestAttribute(AuthInterceptor.ATTR_TOKEN) String token) {
        authService.logout(token);
        return ApiResponse.ok();
    }
}
