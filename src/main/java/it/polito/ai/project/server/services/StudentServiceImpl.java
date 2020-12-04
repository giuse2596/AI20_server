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
        virtualMachine.setPathImage("da/cambiare");
        virtualMachine.setActive(false);
        virtualMachine.setTeam(teamOptional.get());
        virtualMachine.addOwner(studentOptional.get());

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

        // check if the vm is active
        if (!virtualMachineOptional.get().isActive()){
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
    @Override
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

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
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
        virtualMachineOptional.get().getOwners()
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
    @Override
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
     * Get an existing virtual machine
     * @param vmId the virtual machine id
     * @return the virtual machine DTO
     */
    @Override
    public VirtualMachineDTO getVirtualMachine(Long vmId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);

        // check if the vm exists
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
        }

        return this.modelMapper.map(virtualMachineOptional.get(),
                                    VirtualMachineDTO.class);
    }


    /**
     * Get an existing virtual machine
     * @param vmId the virtual machine id
     * @return the virtual machine image
     */
    @Override
    public byte[] getVirtualMachineImage(Long vmId){
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the vm exists
        if(!virtualMachineOptional.isPresent()){
            throw new StudentServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new StudentServiceException("Team not active");
        }

        try {
            bufferedImage = ImageIO.read(new File(virtualMachineOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }

        return byteArrayOutputStream.toByteArray();
    }


    /**
     * Function to upload a delivery for a student homework
     * @param homeworkId homework id
     * @param path path where to save the multipartFile
     * @param multipartFile file to store
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public void uploadDelivery(Long homeworkId, String path, MultipartFile multipartFile){
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkId);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Date deliveryDate = Date.valueOf(timestamp.toLocalDateTime().toLocalDate());
        Delivery delivery = new Delivery();

        // check if the homework exists
        if (!homeworkOptional.isPresent()) {
            throw new StudentServiceException("The homework doesn't exist");
        }

        // check if the homework is editable
        if (!homeworkOptional.get().isEditable()) {
            throw new StudentServiceException("Homework not editable");
        }

        // check if the assignment is not expired
        if (deliveryDate.after(homeworkOptional.get().getAssignment().getExpiryDate())) {
            throw new StudentServiceException("Delivery date expired");
        }

        delivery.setPathImage(path);
        delivery.setStatus(Delivery.Status.DELIVERED);
        delivery.setTimestamp(timestamp);
        delivery.setHomework(homeworkOptional.get());

        this.deliveryRepository.save(delivery);

        // path passed as:
        // String path = request.getSession().getServletContext().getRealPath("/tmp/...");
        File dirPath = new File(path + delivery.getId().toString());

        //check destination exists, if not create it
//        if(!dirPath.exists())
//        {
//            dirPath.mkdir();
//        }

        try {
            multipartFile.transferTo(dirPath);
        }
        catch (IllegalStateException | IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Function to retrieve all the deleveries given a student homework
     * @param homeworkId the student homework id
     * @return list of all the deliveries for the specified homework
     */
    @Override
    public List<DeliveryDTO> getStudentDeliveries(Long homeworkId){
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkId);

        // check if the homework exists
        if (!homeworkOptional.isPresent()) {
            throw new StudentServiceException();
        }

        return homeworkOptional.get()
                                .getDeliveries()
                                .stream()
                                .map(x -> modelMapper.map(x, DeliveryDTO.class))
                                .collect(Collectors.toList());
    }

    /**
     * Function to get the image of the specified delivery
     * @param deliveryDTO the dto to send containing delivery info
     * @return a byte array of the image associated to the delivery
     */
    @Override
    public byte[] getDeliveryImage(DeliveryDTO deliveryDTO){
        Optional<Delivery> deliveryOptional = this.deliveryRepository.findById(deliveryDTO.getId());
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the delivery exists
        if (!deliveryOptional.isPresent()) {
            throw new StudentServiceException();
        }

        try {
            bufferedImage = ImageIO.read(new File(deliveryDTO.getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }
        return byteArrayOutputStream.toByteArray();
    }


    /**
     * Function to get the image of a specified assignment and
     * mark as READ the student homework
     * @param assignmentDTO the dto to send containing assignment info
     * @param studentDTO the student dto
     * @return a byte array of the image associated to the assignment
     */
    @Override
    public byte[] getAssignmentImage(AssignmentDTO assignmentDTO, StudentDTO studentDTO){
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentDTO.getId());
        Optional<Student> studentOptional = this.studentRepository.findById(studentDTO.getId());
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Delivery delivery = new Delivery();
        Homework homework;

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        // check if the assignment exists
        if (!assignmentOptional.isPresent()) {
            throw new StudentServiceException();
        }

        // get the homework for the given assignment
        homework = studentOptional.get()
                        .getHomeworks()
                        .stream()
                        .filter(x -> x.getAssignment().getId().equals(assignmentDTO.getId()))
                        .collect(Collectors.toList()).get(0);

        if (
            homework.getDeliveries()
                    .get(homework.getDeliveries().size()-1)
                    .getStatus().equals(Delivery.Status.NULL)
        ) {
            delivery.setHomework(homework);
            delivery.setTimestamp(new Timestamp(System.currentTimeMillis()));
            delivery.setStatus(Delivery.Status.READ);
            this.deliveryRepository.save(delivery);
        }

        try {
            bufferedImage = ImageIO.read(new File(assignmentDTO.getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }
        return byteArrayOutputStream.toByteArray();
    }

}
