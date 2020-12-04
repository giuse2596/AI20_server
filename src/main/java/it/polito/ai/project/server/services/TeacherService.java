package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Reader;
import java.util.List;

public interface TeacherService {
    boolean addCourse(CourseDTO course, String teacherId, VMModelDTO vmModelDTO);
    boolean removeCourse(String coursename);
    void modifyCourse(String courseId, CourseDTO course);
    List<StudentDTO> getEnrolledStudents(String courseName);
    boolean addStudentToCourse(String studentId, String courseName);
    boolean addTeacherToCourse(String teacherId, String courseName);
    boolean removeStudentToCourse(String studentId, String courseName);
    void enableCourse(String courseName);
    void disableCourse(String courseName);
    List<TeamDTO> getTeamForCourse(String courseName);
    List<Boolean> enrollAll(List<String> studentIds, String courseName);
    List<Boolean> enrollCSV(Reader r, String courseName);
    void changeVMvalues(TeamDTO newTeam, String courseName);
    VirtualMachineDTO getTeamVirtualMachine(Long vmId);
    AssignmentDTO createAssignment(AssignmentDTO assignment, String courseName);
    void removeAssignment(Long assignmentId, String courseName);
    void assignMarkToHomework(HomeworkDTO homework);
    void revisionDelivery(HomeworkDTO homeworkDTO, MultipartFile multipartFile);
    byte[] getAssignmentImage(Long assignmentId);
}
