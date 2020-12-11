package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;

public interface UserService {
    UserDTO registerStudent(UserDTO userDTO);
    UserDTO registerTeacher(UserDTO userDTO);
}
