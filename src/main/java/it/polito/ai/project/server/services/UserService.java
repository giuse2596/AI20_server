package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserDTO registerStudent(UserDTO userDTO);
    UserDTO registerTeacher(UserDTO userDTO);
    User getActiveUser(String username);
    void modifyUserImage(String username, MultipartFile multipartFile);
    byte[] getUserImage(String username);
}
