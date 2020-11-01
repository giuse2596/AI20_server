package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;
import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Team;
import it.polito.ai.project.server.entities.VirtualMachine;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeamRepository;
import it.polito.ai.project.server.repositories.VirtualMachinesRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentServiceImpl implements StudentService{

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    VirtualMachinesRepository virtualMachinesRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    ModelMapper modelMapper;

    /**
     * Function to create a virtual machine for a given team
     * @param cpu virtual machine cpu
     * @param ram virtual machine ram
     * @param diskspace virtual machine disk space
     * @param vmName virtual machine name given by the creator
     * @param teamId team to which the virtual machine belongs
     * @param owner list of the virtual machine owers
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void createVirtualMachine(Integer cpu, Integer ram, Integer diskspace,
                                                   String vmName, Long teamId, String owner) {
        Team team;
        VirtualMachine virtualMachine;

        // check if the team exist
        if(!this.teamRepository.existsById(teamId)){
            throw new TeamNotFoundException();
        }

        // check if the student exist
        if(!this.studentRepository.existsById(owner)){
            throw new StudentNotFoundExeption();
        }

        team = this.teamRepository.findById(teamId).get();

        // check if the team is active
        if(team.getStatus() != 1){
            throw new StudentServiceException();
        }

        // check if the student belongs to the team
        if(team.getMembers()
                .stream()
                .filter( x -> x.getId().equals(owner))
                .count() < 1
        ){
            throw new StudentNotFoundExeption();
        }

        // check if the student is enrolled to the course
        if(!team.getCourse().getStudents().contains(this.studentRepository.findById(owner).get())){
            throw new StudentServiceException();
        }

        // check if the course is enabled
        if(!team.getCourse().isEnabled()){
            throw new StudentServiceException();
        }

        // check if can be created another virtual machine
        if((team.getVirtualMachines().size() + 1) > team.getTotVM()){
            throw new StudentServiceException("Max number virtual machines reached!");
        }

        // check if the name is unique for all the team virtual machines
        if(team.getVirtualMachines().stream()
                                    .map(x -> x.getName())
                                    .collect(Collectors.toList())
                                    .contains(vmName)
        ){
            throw new StudentServiceException("The vm name must be unique");

        }

        // check if the resources given don't exceed
        if((team.getVirtualMachines()
                .stream()
                .map(x -> x.getCpu())
                .reduce(0, (a, b) -> a+b) + cpu) > team.getCpuMax()){
            throw new StudentServiceException("Cpu limit exceeded");
        }

        if((team.getVirtualMachines()
                .stream()
                .map(x -> x.getRam())
                .reduce(0, (a, b) -> a+b) + ram) > team.getRamMax()){
            throw new StudentServiceException("RAM limit exceeded");
        }

        if((team.getVirtualMachines()
                .stream()
                .map(x -> x.getDiskSpace())
                .reduce(0, (a, b) -> a+b) + diskspace)> team.getDiskSpaceMax()){
            throw new StudentServiceException("Disk space limit exceeded");
        }

        // create the virtual machine
        virtualMachine = VirtualMachine.builder()
                                        .name(vmName)
                                        .cpu(cpu)
                                        .diskSpace(diskspace)
                                        .ram(ram)
                                        .active(false)
                                        .build();

        virtualMachine.setTeam(team);
        virtualMachine.addOwner(studentRepository.findById(owner).get());

        this.virtualMachinesRepository.save(virtualMachine);
    }

    /**
     * Funcition to start an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void startVirtualMachine(Long vmId, String studentId){
        VirtualMachine virtualMachine;

        // check if the vm exist
        if(!this.virtualMachinesRepository.existsById(vmId)){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!this.studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }

        virtualMachine = this.virtualMachinesRepository.findById(vmId).get();

        // check if the course is enabled
        if(!virtualMachine.getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the student is one of the owner of the virtual machine
        if (virtualMachine.getOwners()
                            .stream()
                            .map(x -> x.getId())
                            .filter( x -> x.equals(studentId))
                            .count() < 1
        ) {
            throw new StudentServiceException();
        }

        virtualMachine.setActive(true);
    }

    /**
     * Funcition to stop an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void stopVirtualMachine(Long vmId, String studentId){
        VirtualMachine virtualMachine;

        // check if the vm exist
        if(!this.virtualMachinesRepository.existsById(vmId)){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!this.studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }

        virtualMachine = this.virtualMachinesRepository.findById(vmId).get();

        // check if the course is enabled
        if(!virtualMachine.getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the student is one of the owner of the virtual machine
        if (virtualMachine.getOwners()
                .stream()
                .map(x -> x.getId())
                .filter( x -> x.equals(studentId))
                .count() < 1
        ) {
            throw new StudentServiceException();
        }

        virtualMachine.setActive(false);
    }

    /**
     * Funcition to delete an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void deleteVirtualMachine(Long vmId, String studentId){
        VirtualMachine virtualMachine;

        // check if the vm exist
        if(!this.virtualMachinesRepository.existsById(vmId)){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!this.studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }

        virtualMachine = this.virtualMachinesRepository.findById(vmId).get();

        // check if the student is one of the owner of the virtual machine
        if (!virtualMachine.getOwners()
                .stream()
                .map(x -> x.getId())
                .collect(Collectors.toList())
                .contains(studentId)
        ) {
            throw new StudentServiceException();
        }

        // delete the virtual machine from all the students and the team
        virtualMachine.getOwners().stream().forEach(x -> virtualMachine.removeOwner(x));
        virtualMachine.setTeam(null);
        this.virtualMachinesRepository.deleteById(vmId);
    }

    /**
     * Function to add owners to an existing virtual machine
     * @param owner one of the owners of the virtual machine
     * @param students the list of students id to add as owners
     * @param vmId the virtual machine id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void addVirtualMachineOwners(String owner, List<String> students, Long vmId){

        // check if the owner exist
        if(!this.studentRepository.existsById(owner)){
            throw new StudentServiceException("Owner doesn't exist");

        }

        // check if all the students exist
        if(students.stream()
                    .filter(x -> this.studentRepository.existsById(x))
                    .count() != students.size()
        ){
            throw new StudentServiceException("At least one student doesn't exist");
        }

        // check if the vm exist
        if(!this.virtualMachinesRepository.existsById(vmId)){
            throw new StudentServiceException("The virtual machine doesn't exist");
        }

        // check if the owner is present in the owner list
        if(!this.virtualMachinesRepository.findById(vmId).get()
                .getOwners()
                .contains(this.studentRepository.findById(owner).get())
        ){
            throw new StudentServiceException("The student is not the owner of the virtual machine");
        }

        // check if the course is enabled
        if(!this.virtualMachinesRepository.findById(vmId).get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // add all students as owners of the virtual machine
        students.stream()
                .map(x -> this.studentRepository.findById(x).get())
                .forEach(
                        x -> this.virtualMachinesRepository.findById(vmId).get().addOwner(x)
                );
    }

    /**
     * Get an existing virtual machine
     * @param studentId the student id
     * @param vmId the virtual machine id
     * @return the virtual machine DTO
     */
    public VirtualMachineDTO getVirtualMachine(String studentId, Long vmId){

        // check if the student exists
        if(!this.studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }

        // check if the vm exists
        if(!this.virtualMachinesRepository.existsById(vmId)){
            throw new StudentServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!this.virtualMachinesRepository.findById(vmId).get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        return this.modelMapper.map(this.virtualMachinesRepository.findById(vmId).get(),
                                    VirtualMachineDTO.class);
    }

}
