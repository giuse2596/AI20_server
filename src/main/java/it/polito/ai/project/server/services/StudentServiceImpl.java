package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
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
    HomeworkRepository homeworkRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    ModelMapper modelMapper;

    /**
     * Retrieve all courses the student is enrolled in
     * @param studentId the student id
     * @return list of all the courses the student is enrolled in
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<CourseDTO> getCourses(String studentId) {
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }
        return studentOptional.get()
                .getCourses()
                .stream()
                .map(x -> modelMapper.map(x, CourseDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Function to create a virtual machine for a given team
     * @param virtualMachineDTO virtual machine DTO
     * @param teamId team to which the virtual machine belongs
     * @param owner list of the virtual machine owers
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void createVirtualMachine(VirtualMachineDTO virtualMachineDTO, Long teamId, String owner) {

        Optional<Team> teamOptional = this.teamRepository.findById(teamId);
        Optional<Student> studentOptional = this.studentRepository.findById(owner);
        VirtualMachine virtualMachine = new VirtualMachine();

        // check if the team exist
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the team is active
        if(!teamOptional.get().isActive()){
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
                                    .contains(virtualMachineDTO.getName())
        ){
            throw new StudentServiceException("The vm name must be unique");

        }

        // check if the resources given don't exceed
        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getCpu)
                .reduce(0, (a, b) -> a+b) + virtualMachineDTO.getCpu()) > teamOptional.get().getCpuMax()){
            throw new StudentServiceException("Cpu limit exceeded");
        }

        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getRam)
                .reduce(0, (a, b) -> a+b) + virtualMachineDTO.getRam()) > teamOptional.get().getRamMax()){
            throw new StudentServiceException("RAM limit exceeded");
        }

        if((teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getDiskSpace)
                .reduce(0, (a, b) -> a+b) + virtualMachineDTO.getDiskSpace())
                                            > teamOptional.get().getDiskSpaceMax()
        )
        {
            throw new StudentServiceException("Disk space limit exceeded");
        }

        // create the virtual machine
        virtualMachine.setName(virtualMachineDTO.getName());
        virtualMachine.setCpu(virtualMachineDTO.getCpu());
        virtualMachine.setDiskSpace(virtualMachineDTO.getDiskSpace());
        virtualMachine.setRam(virtualMachineDTO.getRam());
        virtualMachine.setPathImage("src/main/resources/images/virtual_machines/virtual_machine.png");
        virtualMachine.setActive(false);
        virtualMachine.setTeam(teamOptional.get());
        virtualMachine.addOwner(studentOptional.get());
        virtualMachine.setCreator(studentOptional.get().getId());

        this.virtualMachinesRepository.save(virtualMachine);
    }


    /**
     * Change virtual machine used resources
     * @param virtualMachineDTO the DTO with the new resources
     * @param owner one of the owners of the virtual machine
     */
    @Override
    public void changeVirtualMachineParameters(VirtualMachineDTO virtualMachineDTO, String owner){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository
                                                                .findById(virtualMachineDTO.getId());
        Optional<Student> studentOptional = this.studentRepository.findById(owner);
        Team team;

        // check if the vm exists
        if (!virtualMachineOptional.isPresent()) {
            throw new StudentServiceException("Virtual machine doesn't exists");
        }

        // check if the vm is active, it must be stopped to modify it
        if (virtualMachineOptional.get().isActive()){
            throw new StudentServiceException("Virtual machine is not active");
        }

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        // check if the student is the owner
        if (!virtualMachineOptional.get().getOwners().contains(studentOptional.get())) {
            throw new StudentServiceException("The student is not one of the owner of the virtual machine");
        }

        team = virtualMachineOptional.get().getTeam();

        // check if the team is active
        if(!team.isActive()){
            throw new StudentServiceException("Team not active");
        }

        // check if the course is enabled
        if(!team.getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the DTO has a different name from the one in the repo
        // if yes change it
        if(!virtualMachineOptional.get().getName().equals(virtualMachineDTO.getName())){

            // check if the name is unique for all the team virtual machines
            if(team.getVirtualMachines().stream()
                    .map(VirtualMachine::getName)
                    .collect(Collectors.toList())
                    .contains(virtualMachineDTO.getName())
            ){
                throw new StudentServiceException("The vm name must be unique");

            }

            // change virtual machine name
            virtualMachineOptional.get().setName(virtualMachineDTO.getName());
        }


        // check the cpu, ram and diskspace constraints
        if (
            (team.getVirtualMachines().stream().map(VirtualMachine::getCpu).reduce(0, Integer::sum)
                + virtualMachineDTO.getCpu() - virtualMachineOptional.get().getCpu())
            > team.getCpuMax()
        ) {
            throw new StudentServiceException("Cpu limit exceeded");
        }

        if (
            (team.getVirtualMachines().stream().map(VirtualMachine::getRam).reduce(0, Integer::sum)
                    + virtualMachineDTO.getRam() - virtualMachineOptional.get().getRam())
            > team.getRamMax()
        ) {
            throw new StudentServiceException("RAM limit exceeded");
        }

        if (
            (team.getVirtualMachines().stream().map(VirtualMachine::getDiskSpace).reduce(0, Integer::sum)
                + virtualMachineDTO.getDiskSpace() - virtualMachineOptional.get().getDiskSpace())
             > team.getDiskSpaceMax()
        ) {
            throw new StudentServiceException("Disk space limit exceeded");
        }

        virtualMachineOptional.get().setCpu(virtualMachineDTO.getCpu());
        virtualMachineOptional.get().setRam(virtualMachineDTO.getRam());
        virtualMachineOptional.get().setDiskSpace(virtualMachineDTO.getDiskSpace());

        this.virtualMachinesRepository.save(virtualMachineOptional.get());
    }


    /**
     * Funcition to start an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void startVirtualMachine(Long vmId, String studentId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        long activeVMs;

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

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
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

        // take all team active vm
        activeVMs = virtualMachineOptional.get()
                .getTeam()
                .getVirtualMachines()
                .stream()
                .filter(VirtualMachine::isActive)
                .count();

        // check if can be activated another virtual machine
        if ((activeVMs + 1) > virtualMachineOptional.get().getTeam().getActiveVM()) {
            throw new StudentServiceException("Active VM limit reached");
        }

        virtualMachineOptional.get().setActive(true);
    }

    /**
     * Funcition to stop an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
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

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
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

        virtualMachineOptional.get().setActive(false);
    }

    /**
     * Funcition to delete an existing Virtual Machine
     * @param vmId virtual machine id
     * @param studentId student id
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void deleteVirtualMachine(Long vmId, String studentId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        List<Student> owners = new ArrayList<>();

        // check if the vm exist
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
        }

        // check if the student is one of the owner of the virtual machine
        if (!virtualMachineOptional.get().getOwners()
                .stream()
                .map(Student::getId)
                .collect(Collectors.toList())
                .contains(studentId)
        ) {
            throw new StudentServiceException();
        }

        // delete the virtual machine from all the students and the team
        virtualMachineOptional.get()
                .getOwners()
                .forEach(x -> owners.add(x));

        owners.forEach(x -> virtualMachineOptional.get().removeOwner(x));

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
    @Override
    public void addVirtualMachineOwners(String owner, List<String> students, Long vmId){
        Optional<Student> studentOptional = this.studentRepository.findById(owner);
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);

        // check if the owner exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();

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

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
        }

        // add all students as owners of the virtual machine
        students.stream()
                .map(x -> this.studentRepository.findById(x).get())
                .forEach(
                        x -> virtualMachineOptional.get().addOwner(x)
                );
    }

    /**
     * Function to upload a delivery for a student homework
     * @param studentId student id
     * @param homeworkId homework id
     * @param multipartFile file to store
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void uploadDelivery(String studentId, Long homeworkId, MultipartFile multipartFile){
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkId);
        Optional<String> extension;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Date deliveryDate = Date.valueOf(timestamp.toLocalDateTime().toLocalDate());
        Delivery delivery = new Delivery();
        File newFile;
        InputStream inputStream;
        OutputStream outputStream;
        Delivery.Status status;

        extension = Optional.ofNullable(multipartFile.getOriginalFilename())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(multipartFile.getOriginalFilename().lastIndexOf(".") + 1));

        // check if the file has an extension
        if(!extension.isPresent()){
            throw new NoExtensionException();
        }

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        // check if the homework exists
        if (!homeworkOptional.isPresent()) {
            throw new StudentServiceException("The homework doesn't exist");
        }

        // check if the homework belongs to the student
        if (!homeworkOptional.get().getStudent().getId().equals(studentId)) {
            throw new StudentServiceException("The homework doesn't belogns to the student");
        }

        // check if the homework is editable
        if (!homeworkOptional.get().isEditable()) {
            throw new StudentServiceException("Homework not editable");
        }

        // check if the course is enabled
        if (!homeworkOptional.get().getAssignment().getCourse().isEnabled()) {
            throw new StudentServiceException("Course not enabled");
        }

        // check if the assignment is not expired
        if (deliveryDate.after(homeworkOptional.get().getAssignment().getExpiryDate())) {
            throw new StudentServiceException("Delivery date expired");
        }

        status = homeworkOptional.get().getDeliveries()
                .get(homeworkOptional.get().getDeliveries().size()-1)
                .getStatus();

        // check to avoid multiple uploads
        if(!status.equals(Delivery.Status.READ) & !status.equals(Delivery.Status.REVIEWED)){
            throw new StudentServiceException("Delivery not possible");
        }

        delivery.setStatus(Delivery.Status.DELIVERED);
        delivery.setTimestamp(timestamp);
        delivery.setHomework(homeworkOptional.get());

        this.deliveryRepository.save(delivery);

        delivery.setPathImage("src/main/resources/images/deliveries/" +
                delivery.getId().toString() + "." + extension.get());

        newFile = new File(delivery.getPathImage());

        try {
            inputStream = multipartFile.getInputStream();

            if (!newFile.exists()) {
                newFile.getParentFile().mkdir();
            }
            outputStream = new FileOutputStream(newFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e)
        {
            throw new StudentServiceException("Error saving the file");
        }

    }

    /**
     * Function to get the image of a specified assignment and
     * mark as READ the student homework
     * @param assignmentId the assignment id
     * @param studentId the student id
     * @return a byte array of the image associated to the assignment
     */
    @Override
    public byte[] getAssignmentImage(Long assignmentId, String studentId){
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<String> extension;
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Delivery delivery = new Delivery();
        Optional<Homework> homeworkOptional;

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        // check if the assignment exists
        if (!assignmentOptional.isPresent()) {
            throw new StudentServiceException();
        }

        // get the homework for the given assignment
        homeworkOptional = studentOptional.get()
                        .getHomeworks()
                        .stream()
                        .filter(x -> x.getAssignment().getId().equals(assignmentId))
                        .findFirst();

        // check if exists an homework for the given assignment
        if (!homeworkOptional.isPresent()) {
            throw new StudentServiceException();
        }

        // if the last delivery has status NULL add a delivery with READ status
        if (
            homeworkOptional.get()
                    .getDeliveries()
                    .get(homeworkOptional.get().getDeliveries().size()-1)
                    .getStatus().equals(Delivery.Status.NULL)
        ) {
            delivery.setHomework(homeworkOptional.get());
            delivery.setTimestamp(new Timestamp(System.currentTimeMillis()));
            delivery.setPathImage("src/main/resources/images/deliveries/empty_image.png");
            delivery.setStatus(Delivery.Status.READ);
            this.deliveryRepository.save(delivery);
        }

        // get the extension of the file
        extension = Optional.ofNullable(assignmentOptional.get().getPathImage())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(assignmentOptional.get().getPathImage().lastIndexOf(".") + 1));

        try {
            bufferedImage = ImageIO.read(new File(assignmentOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, extension.get(), byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }
        return byteArrayOutputStream.toByteArray();
    }

}
