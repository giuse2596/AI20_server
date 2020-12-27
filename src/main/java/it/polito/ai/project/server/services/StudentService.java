package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {
    List<CourseDTO> getCourses(String studentId);
    void createVirtualMachine(VirtualMachineDTO virtualMachineDTO, Long teamId, String owner);
    void changeVirtualMachineParameters(VirtualMachineDTO virtualMachineDTO, String owner);
    void startVirtualMachine(Long vmId, String studentId);
    void stopVirtualMachine(Long vmId, String studentId);
    void deleteVirtualMachine(Long vmId, String studentId);
    void addVirtualMachineOwners(String owner, List<String> students, Long vmId);
    void uploadDelivery(String studentId, Long homeworkId, MultipartFile multipartFile);
    byte[] getAssignmentImage(Long assignmentId, String studentId);
}
