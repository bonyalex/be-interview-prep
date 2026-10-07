package com.example.beinterviewprep.service;

import com.example.beinterviewprep.dto.PageResponse;
import com.example.beinterviewprep.dto.UserResponse;
import com.example.beinterviewprep.exception.ResourceNotFoundException;
import com.example.beinterviewprep.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse profile(String email) {
        return userRepository
                .findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
    }
}
