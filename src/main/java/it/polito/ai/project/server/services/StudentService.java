package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.DeliveryDTO;
import it.polito.ai.project.server.dtos.HomeworkDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {
    void createVirtualMachine(VirtualMachineDTO virtualMachineDTO, Long teamId, String owner);
    void changeVirtualMachineParameters(VirtualMachineDTO virtualMachineDTO, String owner);
    void startVirtualMachine(Long vmId, String studentId);
    void stopVirtualMachine(Long vmId, String studentId);
    void deleteVirtualMachine(Long vmId, String studentId);
    void addVirtualMachineOwners(String owner, List<String> students, Long vmId);
    VirtualMachineDTO getVirtualMachine(Long vmId);
    void uploadDelivery(HomeworkDTO homeworkDTO, String path, MultipartFile multipartFile);
    List<DeliveryDTO> getStudentDeliveries(Long homeworkId);
}
