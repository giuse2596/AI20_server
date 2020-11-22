package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.services.UserService;
import it.polito.ai.project.server.services.UserServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class UserinfoController {

    @Autowired
    UserService userService;

    @GetMapping("/me")
    public ResponseEntity currentUser(@AuthenticationPrincipal UserDetails userDetails){
        Map<Object, Object> model = new HashMap<>();
        model.put("username", userDetails.getUsername());
        model.put("roles", userDetails.getAuthorities()
                            .stream()
                            .map(a -> ((GrantedAuthority) a).getAuthority())
                            .collect(toList())
                    );
        return ok(model);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody UserDTO userDTO){
        String[] splittedEmail = userDTO.getEmail().trim().split("@");

        try {
            // student registration
            if (splittedEmail[1].equals("studenti.polito.it")) {
                this.userService.registerStudent(userDTO);
            }
            // teacher registration
            else if (splittedEmail[1].equals("polito.it")) {
                this.userService.registerTeacher(userDTO);
            } else {
                // received an invalid email
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid email");
            }
        }
        catch (UserServiceException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

    }
}
