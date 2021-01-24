package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Reader;
import java.util.List;

public interface TeacherService {
    boolean teacherInCourse(String teacherId, String couseName);
    List<TeacherDTO> teachersNotInCourse(String courseName);
    boolean addCourse(CourseDTO course, String teacherId, VMModelDTO vmModelDTO);
    boolean removeCourse(String coursename);
    void modifyCourse(CourseDTO courseDTO);
    boolean addStudentToCourse(String studentId, String courseName);
    boolean addTeacherToCourse(String teacherId, String courseName);
    boolean removeStudentToCourse(String studentId, String courseName, boolean deleteCourse);
    List<Boolean> enrollAll(List<String> studentIds, String courseName);
    List<Boolean> enrollCSV(Reader r, String courseName);
    void changeVMvalues(TeamDTO newTeam, String courseName);
    AssignmentDTO createAssignment(AssignmentDTO assignment, String courseName);
    void removeAssignment(Long assignmentId, String courseName);
    void assignMarkToHomework(HomeworkDTO homeworkDTO);
    DeliveryDTO revisionDelivery(Long homeworkId, MultipartFile multipartFile);
    byte[] getAssignmentImage(Long assignmentId);
    VMModelDTO getVMModel(String courseName);
    void setEditableHomework(HomeworkDTO homeworkDTO);
    List<CourseDTO> getTeacherCourses(String teacherId);
    List<HomeworkDTO> getHomework(Long assignmentId);
}
