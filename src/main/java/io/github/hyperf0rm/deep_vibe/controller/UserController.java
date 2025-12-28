package io.github.hyperf0rm.deep_vibe.controller;

import io.github.hyperf0rm.deep_vibe.dto.LastFmUser;
import io.github.hyperf0rm.deep_vibe.dto.UserCreateRequest;
import io.github.hyperf0rm.deep_vibe.dto.UserResponse;
import io.github.hyperf0rm.deep_vibe.entity.User;
import io.github.hyperf0rm.deep_vibe.repository.UserRepository;
import io.github.hyperf0rm.deep_vibe.service.LastFmService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final UserRepository userRepository;
    private final LastFmService lastFmService;

    public UserController(UserRepository userRepository, LastFmService service) {
        this.userRepository = userRepository;
        this.lastFmService = service;
    }

    @PostMapping(path = "/add")
    public ResponseEntity<UserResponse> addNewUser(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setLastfmUsername(request.lastfmUsername());
        User savedUser = userRepository.save(user);
        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getLastfmUsername(),
                savedUser.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(path = "/all")
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping(path = "/sync/{username}")
    public LastFmUser doSync(@PathVariable String username) {
        return lastFmService.getUserInfo(username);
    }
}
