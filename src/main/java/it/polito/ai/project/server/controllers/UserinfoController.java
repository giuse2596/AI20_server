package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register/student")
    public void registerStudent(@RequestBody AuthenticationRequest data){
        userService.registerStudent(data.getUsername(), data.getPassword());
    }

    @PostMapping("/register/teacher")
    public void registerTeacher(@RequestBody AuthenticationRequest data){
        userService.registerTeacher(data.getUsername(), data.getPassword());
    }
}
