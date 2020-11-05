package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;

import java.util.List;
import java.util.Optional;

public interface GeneralService {
    Optional<CourseDTO> getCourse(String name);
    Optional<StudentDTO> getStudent(String studentId);
    List<StudentDTO> getAllStudents();

}