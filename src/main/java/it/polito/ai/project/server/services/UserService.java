package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface UserService {
    Optional<UserDTO> getUser(String username);
    UserDTO registerStudent(UserDTO userDTO);
    UserDTO registerTeacher(UserDTO userDTO);
    User getActiveUser(String username);
    UserDTO modifyUser(UserDTO userDTO);
    void modifyUserImage(String username, MultipartFile multipartFile);
    byte[] getUserImage(String username);
}
