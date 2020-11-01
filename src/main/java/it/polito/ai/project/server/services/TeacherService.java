package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.VirtualMachine;

import java.io.Reader;
import java.util.Date;
import java.util.List;

public interface TeacherService {
    boolean addStudent(StudentDTO student);
    boolean addCourse(CourseDTO course);
    boolean removeCourse(CourseDTO course);
    void modifyCourse(String courseId, CourseDTO course);
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
    void changeVMvalues(TeamDTO newTeam, String courseName);
    void createAssignment(AssignmentDTO assignment, String courseName);
}
