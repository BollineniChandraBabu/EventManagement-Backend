package com.familywishes.service.impl;

import com.familywishes.dto.UserDtos.*;
import com.familywishes.entity.User;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Override
    public UserResponse create(UserRequest request) {
        User user = User.builder().name(request.name()).email(request.email()).password(encoder.encode(request.password()))
                .role(request.role()).active(true).deleted(false).build();
        user = userRepository.save(user);
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }

    @Override
    public List<UserResponse> list() {
        return userRepository.findAll().stream().filter(u -> !u.isDeleted())
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive())).toList();
    }

    @Override
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("Authenticated user not found");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }

    @Override
    public void deactivate(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }
}
