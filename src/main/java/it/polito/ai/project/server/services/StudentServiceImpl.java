package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.VirtualMachineDTO;
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
import java.util.List;
import java.util.Optional;
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

        Optional<Team> teamOptional = this.teamRepository.findById(teamId);
        Optional<Student> studentOptional = this.studentRepository.findById(owner);
        VirtualMachine virtualMachine;

        // check if the team exist
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the team is active
        if(teamOptional.get().getStatus() != 1){
            throw new StudentServiceException("Team not active");
        }

        // check if the student belongs to the team
        if(teamOptional.get().getMembers()
                .stream()
                .filter( x -> x.getId().equals(owner))
                .count() < 1
        ){
            throw new StudentNotFoundExeption();
        }

        // check if the student is enrolled to the course
        if(!teamOptional.get().getCourse().getStudents().contains(studentOptional.get())){
            throw new StudentServiceException("Student not enrolled");
        }

        // check if the course is enabled
        if(!teamOptional.get().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if can be created another virtual machine
        if((teamOptional.get().getVirtualMachines().size() + 1) > teamOptional.get().getTotVM()){
            throw new StudentServiceException("Max number virtual machines reached!");
        }

        // check if the name is unique for all the team virtual machines
        if(teamOptional.get().getVirtualMachines().stream()
                                    .map(VirtualMachine::getName)
                                    .collect(Collectors.toList())
                                    .contains(vmName)
        ){
            throw new StudentServiceException("The vm name must be unique");

        }

        // check if the resources given don't exceed
        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getCpu)
                .reduce(0, (a, b) -> a+b) + cpu) > teamOptional.get().getCpuMax()){
            throw new StudentServiceException("Cpu limit exceeded");
        }

        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getRam)
                .reduce(0, (a, b) -> a+b) + ram) > teamOptional.get().getRamMax()){
            throw new StudentServiceException("RAM limit exceeded");
        }

        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getDiskSpace)
                .reduce(0, (a, b) -> a+b) + diskspace)> teamOptional.get().getDiskSpaceMax()){
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

        virtualMachine.setTeam(teamOptional.get());
        virtualMachine.addOwner(studentOptional.get());

        this.virtualMachinesRepository.save(virtualMachine);
    }

    /**
     * Funcition to start an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void startVirtualMachine(Long vmId, String studentId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the vm exist
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()) {
            throw new StudentServiceException("Course not enabled");
        }

        // check if the student is one of the owner of the virtual machine
        if (virtualMachineOptional.get().getOwners()
                            .stream()
                            .map(Student::getId)
                            .filter( x -> x.equals(studentId))
                            .count() < 1
        ) {
            throw new StudentServiceException("The student is not one of the vm owners");
        }

        virtualMachineOptional.get().setActive(true);
    }

    /**
     * Funcition to stop an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void stopVirtualMachine(Long vmId, String studentId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the vm exist
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the student is one of the owner of the virtual machine
        if (virtualMachineOptional.get().getOwners()
                .stream()
                .map(x -> x.getId())
                .filter( x -> x.equals(studentId))
                .count() < 1
        ) {
            throw new StudentServiceException("The student is not one of the vm owners");
        }

        virtualMachineOptional.get().setActive(false);
    }

    /**
     * Funcition to delete an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public void deleteVirtualMachine(Long vmId, String studentId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the vm exist
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the student is one of the owner of the virtual machine
        if (!virtualMachineOptional.get().getOwners()
                .stream()
                .map(x -> x.getId())
                .collect(Collectors.toList())
                .contains(studentId)
        ) {
            throw new StudentServiceException();
        }

        // delete the virtual machine from all the students and the team
        virtualMachineOptional.get()
                            .getOwners()
                            .forEach(x -> virtualMachineOptional.get().removeOwner(x));
        virtualMachineOptional.get().setTeam(null);
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
        Optional<Student> studentOptional = this.studentRepository.findById(owner);
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);

        // check if the owner exist
        if(!studentOptional.isPresent()){
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
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException("The virtual machine doesn't exist");
        }

        // check if the owner is present in the owner list
        if(!virtualMachineOptional.get()
                .getOwners()
                .contains(studentOptional.get())
        ){
            throw new StudentServiceException("The student is not the owner of the virtual machine");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // add all students as owners of the virtual machine
        students.stream()
                .map(x -> this.studentRepository.findById(x).get())
                .forEach(
                        x -> virtualMachineOptional.get().addOwner(x)
                );
    }

    /**
     * Get an existing virtual machine
     * @param studentId the student id
     * @param vmId the virtual machine id
     * @return the virtual machine DTO
     */
    public VirtualMachineDTO getVirtualMachine(String studentId, Long vmId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the vm exists
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        return this.modelMapper.map(virtualMachineOptional.get(),
                                    VirtualMachineDTO.class);
    }

}
