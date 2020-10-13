package it.polito.ai.project.server.services;

public interface UserService {
    void registerStudent(String username, String password);
    void registerTeacher(String username, String password);
}
