package com.yasser.footballersmarket.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yasser.footballersmarket.transaction.TransactionController;
import com.yasser.footballersmarket.user.dto.AuthRequest;
import com.yasser.footballersmarket.user.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    Logger logger = LoggerFactory.getLogger(TransactionController.class);

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest registerRequest) {
        logger.info("user attempting register username {}", registerRequest.getUsername());
        try {
            return ResponseEntity.ok(authService.registerUser(registerRequest));
        }catch (Exception e){
            logger.error("user register error username {} error {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthResponse(-1, e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest authRequest) {
        logger.info("user attempting login username {}", authRequest.getUsername());
        try {
            return ResponseEntity.ok(authService.authenticateUser(authRequest));
        }catch (Exception e){
            logger.error("user login failed username {} error {}", authRequest.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/auth/user-by-token")
    public ResponseEntity<UserResponse> getUserByToken(){

        try {
            UserResponse userByToken = userService.getUserByToken();
            if(userByToken == null)
                return ResponseEntity.badRequest().build();
            return ResponseEntity.ok(userByToken);
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users/top-users")
    public List<UserResponse> getTopRankedUsers(){
        return userService.getTopRankedUsers();
    }

    @GetMapping("/users/rankings")
    public List<UserResponse> getAllUsersScores() throws JsonProcessingException {
        return userService.getAllUsersScores();
    }

    @GetMapping
    public String hello(){
        return "allowed";
    }
}
