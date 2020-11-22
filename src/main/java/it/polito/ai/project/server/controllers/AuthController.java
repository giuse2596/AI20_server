package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.UserRepository;
import it.polito.ai.project.server.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository users;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody UserDTO userDTO) {
        Optional<User> userOptional;
        String username;
        String token;

        if(userDTO == null){
            throw new BadCredentialsException("Invalid username/password supplied");
        }

        if( (userDTO.getSerialNumber() == null) || (userDTO.getPassword() == null)){
            throw new BadCredentialsException("Invalid username/password supplied");
        }

        if (userDTO.getSerialNumber().isEmpty() || userDTO.getPassword().isEmpty()) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }

        userOptional = this.users.findAll()
                                .stream()
                                .filter(x -> x.getSerialNumber().equals(userDTO.getSerialNumber()))
                                .findFirst();

        if (!userOptional.isPresent()) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }

        if (!userOptional.get().isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "User not active");
        }

        try {
            username = userOptional.get().getSerialNumber();

            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                                                                    username,
                                                                    userOptional.get().getPassword()
                                                                        )
                                );

            token = jwtTokenProvider.createToken(username, this.users.findById(userOptional.get().getId())
                    .orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found"))
                    .getRoles());

            Map<Object, Object> model = new HashMap<>();
            model.put("username", username);
            model.put("token", token);
            return ok(model);
        }
        catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }
    }
}
