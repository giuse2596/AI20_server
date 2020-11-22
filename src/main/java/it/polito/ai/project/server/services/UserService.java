package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;

public interface UserService {
    void registerStudent(UserDTO userDTO);
    void registerTeacher(UserDTO userDTO);
}
