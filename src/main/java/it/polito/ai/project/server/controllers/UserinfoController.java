package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.UserRepository;
import it.polito.ai.project.server.services.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class UserinfoController {

    @Autowired
    UserService userService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    GeneralService generalService;

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
        UserDTO user;

        try {
            // student registration
            if (splittedEmail[1].equals("studenti.polito.it") & splittedEmail[0].startsWith("s")) {
                user = this.userService.registerStudent(userDTO);
            }
            // teacher registration
            else if (splittedEmail[1].equals("polito.it") & splittedEmail[0].startsWith("d")) {
                user = this.userService.registerTeacher(userDTO);
            } else {
                // received an invalid email
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid email");
            }
        }
        catch (UserServiceException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        this.notificationService.notifyInscription(user);
    }

    @PutMapping("/modify_user")
    public UserDTO modifyUser(@RequestBody UserDTO userDTO,
                              @AuthenticationPrincipal UserDetails userDetails){
        Optional<UserDTO> userOptional = this.userService.getUser(userDetails.getUsername());

        if(!userOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try{
            return this.userService.modifyUser(userDTO);
        }
        catch (GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    @PutMapping("/{username}/modify_user_image")
    public void modifyUserImage(@RequestBody MultipartFile multipartFile,
                                @PathVariable String username,
                                @AuthenticationPrincipal UserDetails userDetails){
        Optional<UserDTO> userOptional = this.userService.getUser(userDetails.getUsername());
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("image/png");

        // check media type of the file
        try {
            mediaType = tika.detect(multipartFile.getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        if(!userOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // check if the user is the same of {username}
        if(!userOptional.get().getUsername().equals(username)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            this.userService.modifyUserImage(username, multipartFile);
        } catch (UserServiceException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    @GetMapping(value="/{username}/user_image", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getUserImage(@PathVariable String username,
                               @AuthenticationPrincipal UserDetails userDetails){
        Optional<UserDTO> userOptional = this.userService.getUser(userDetails.getUsername());

        if(!userOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // check if the user is the same of {username}
        if(!userOptional.get().getUsername().equals(username)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return this.userService.getUserImage(username);
        }catch (UserServiceException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

}
