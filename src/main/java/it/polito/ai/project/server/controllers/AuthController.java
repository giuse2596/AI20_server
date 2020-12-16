package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.security.jwt.JwtTokenProvider;
import it.polito.ai.project.server.services.UserNotActiveException;
import it.polito.ai.project.server.services.UserService;
import it.polito.ai.project.server.services.UserServiceException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserService userService;

    @Autowired
    ModelMapper modelMapper;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody UserDTO userDTO) {
        User user;
        String token;

        try{
            user = userService.getActiveUser(userDTO.getUsername());
        }
        catch (UserServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (UserNotActiveException e){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {

            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                                    userDTO.getUsername(),
                                    userDTO.getPassword()
                                                                        )
                                );

            token = jwtTokenProvider.createToken(userDTO.getUsername(), user.getRoles());

            Map<Object, Object> model = new HashMap<>();
            model.put("user", modelMapper.map(user, UserDTO.class));
            model.put("token", token);
            return ok(model);
        }
        catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }
    }
}
