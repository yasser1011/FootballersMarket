package com.yasser.footballersmarket.user;

import com.yasser.footballersmarket.security.JwtService;
import com.yasser.footballersmarket.transaction.TransactionController;
import com.yasser.footballersmarket.user.dto.AuthRequest;
import com.yasser.footballersmarket.user.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

// put it here instead of User Service
// to avoid circular dependency in User service and Security Config
@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService, PasswordEncoder passwordEncoder,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public AuthResponse authenticateUser(AuthRequest authRequest){
        logger.info("user attempting login username {}", authRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );
        // exception thrown if not authenticated
        User user = (User)authentication.getPrincipal();
        //User user = repo.findbyusername;

        String generatedToken = jwtService.generateToken(user.getUsername());
        logger.info("user login success username {}", authRequest.getUsername());

        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getPoints());
        return new AuthResponse(0, "", generatedToken, userResponse);
    }

        public AuthResponse registerUser(AuthRequest registerRequest){
            String usernameInput = registerRequest.getUsername();
            if(usernameInput.length() > 10){
                logger.info("username not less than 11 characters");
                throw new IllegalStateException("username must be less than 11 characters");
            }
            User user = userRepository.findUserByUsername(usernameInput);
            if(user != null){
                logger.info("username already used");
                throw new IllegalStateException("username already found");
            }

            User user1 = new User(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword())
            );

        User savedUser = userRepository.save(user1);
        UserResponse userResponse = new UserResponse(savedUser.getId()
                , savedUser.getUsername(), savedUser.getPoints());
        String generatedToken = jwtService.generateToken(userResponse.getUsername());

        logger.info("user register success username {}", registerRequest.getUsername());
        return new AuthResponse(0, "", generatedToken, userResponse);
    }

}
