package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;

import java.util.List;
import java.util.Optional;

public interface GeneralService {
    Optional<CourseDTO> getCourse(String name);
    Optional<StudentDTO> getStudent(String studentId);
    List<StudentDTO> getAllStudents();
    AssignmentDTO getAssignment(Long assignmentId);
    List<AssignmentDTO> getCourseAssignments(String courseName);
    DeliveryDTO getAssignmentLastDelivery(Long assignmentId, String studentId);
    List<DeliveryDTO> getAssignmentStudentDeliveries(Long assignmentId, String studentId);
    UserDTO modifyUser(UserDTO userDTO);
    HomeworkDTO getStudentHomework(Long assignmentId, String studentId);
    VirtualMachineDTO getVirtualMachine(Long vmId);
    byte[] getVirtualMachineImage(Long vmId);
}
