package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;
import it.polito.ai.project.server.entities.VirtualMachine;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

public interface TeacherService {
    boolean addStudent(StudentDTO student);
    boolean addCourse(CourseDTO course);
    boolean removeCourse(CourseDTO course);
    boolean modifyCourse(String courseId, CourseDTO course);
    List<StudentDTO> getEnrolledStudents(String courseName);
    boolean addStudentToCourse(String studentId, String courseName);
    boolean removeStudentToCourse(String studentId, String courseName);
    void enableCourse(String courseName);
    void disableCourse(String courseName);
    List<TeamDTO> getTeamForCourse(String courseName);
    List<VirtualMachineDTO> getVMForTeam(Long teamId); // use the getTeamForCourse function
    List<Boolean> addAll(List<StudentDTO> students);
    List<Boolean> enrollAll(List<String> studentIds, String courseName);
    List<Boolean> addAndEroll(Reader r, String courseName);

}
