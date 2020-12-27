package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;

import java.util.List;
import java.util.Optional;

public interface GeneralService {
    List<CourseDTO> getAllCourses();
    Optional<CourseDTO> getCourse(String name);
    Optional<StudentDTO> getStudent(String studentId);
    List<StudentDTO> getAllStudents();
    List<StudentDTO> getEnrolledStudents(String courseName);
    AssignmentDTO getAssignment(Long assignmentId);
    List<AssignmentDTO> getCourseAssignments(String courseName);
    DeliveryDTO getAssignmentLastDelivery(Long assignmentId, String studentId);
    List<DeliveryDTO> getAssignmentStudentDeliveries(Long assignmentId, String studentId);
    HomeworkDTO getStudentHomework(Long assignmentId, String studentId);
    VirtualMachineDTO getVirtualMachine(Long vmId);
    List<VirtualMachineDTO> getTeamVirtualMachines(Long teamId);
    boolean getVirtualMachineImage(Long vmId);
    byte[] getDeliveryImage(Long deliveryId);
}
