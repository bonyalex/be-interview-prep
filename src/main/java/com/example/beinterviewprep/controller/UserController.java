package com.example.beinterviewprep.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.beinterviewprep.dto.PageResponse;
import com.example.beinterviewprep.dto.UserResponse;
import com.example.beinterviewprep.service.UserService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the logged-in user's profile")
    public UserResponse me(Principal principal) {
        return userService.profile(principal.getName());
    }

    @GetMapping
    @Operation(summary = "List users (ADMIN only)")
    public PageResponse<UserResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.list(pageable);
    }
}
