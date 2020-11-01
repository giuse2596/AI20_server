package it.polito.ai.project.server.services;


import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;

import java.util.List;

public interface StudentService {
    void createVirtualMachine(Integer cpu, Integer ram, Integer diskspace,
                              String vmName, Long teamId, String owner);
    void startVirtualMachine(Long vmId, String studentId);
    void stopVirtualMachine(Long vmId, String studentId);
    void deleteVirtualMachine(Long vmId, String studentId);
    void addVirtualMachineOwners(String owner, List<String> students, Long vmId);
    VirtualMachineDTO getVirtualMachine(String studentId, Long vmId);
}
