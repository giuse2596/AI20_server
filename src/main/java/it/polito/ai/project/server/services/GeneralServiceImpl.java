package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class GeneralServiceImpl implements GeneralService{

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    VirtualMachinesRepository virtualMachinesRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    ModelMapper modelMapper;

    /**
     * Retrieve an existing course
     * @param name the name of the existing course
     * @return an Optional<CourseDTO>
     */
    @Override
    public Optional<CourseDTO> getCourse(String name) {
        return courseRepository.findById(name).map(x -> modelMapper.map(x, CourseDTO.class));
    }

    /**
     * Retrieve a student
     * @param studentId the student id string of the student to retrieve
     * @return an Optional<StudentDTO>
     */
    @Override
    public Optional<StudentDTO> getStudent(String studentId) {
        return studentRepository.findById(studentId)
                .map(x -> modelMapper.map(x, StudentDTO.class));
    }

    /**
     * Retrieve all students present in the db
     * @return a list with all the students present in the db
     */
    @Override
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the assignment object
     * @param assignmentId the id of the assignment
     * @return the assignment DTO
     */
    @Override
    public AssignmentDTO getAssignment(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new GeneralServiceException("The assignment does not exists");
        }

        return modelMapper.map(assignmentOptional.get(), AssignmentDTO.class);
    }

    /**
     * Retrieve all the assignments of the course
     * @param courseName the name of the course
     * @return all the assignments of the course
     */
    @Override
    public List<AssignmentDTO> getCourseAssignments(String courseName) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        List<Assignment> assignments;

        //check if the course exists
        if(!courseOptional.isPresent()){
            throw new TeacherServiceException("Course not found");
        }

        assignments = courseOptional.get().getAssignments();

        return assignments
                .stream()
                .map(x -> modelMapper.map(x, AssignmentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the student's last delivery for an assignment
     * @param assignmentId the id of the assignment
     * @return all the students' last deliveries for an assignment
     */
    @Override
    public DeliveryDTO getAssignmentLastDelivery(Long assignmentId, String studentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<DeliveryDTO> deliveryDTOOptional;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new TeacherServiceException("Assignment not found");
        }

        deliveryDTOOptional = assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                // there will be always one homework for the given couple assignment-student
                .findFirst()
                .map(x -> x.getDeliveries().get(x.getDeliveries().size()-1))
                .map(x -> modelMapper.map(x, DeliveryDTO.class));

        return deliveryDTOOptional.get();
    }

    /**
     * Retrieve the list of the deliveries of one student
     * @param assignmentId the assignment id
     * @param studentId the student id
     * @return the list of the assignment of the student
     */
    @Override
    public List<DeliveryDTO> getAssignmentStudentDeliveries(Long assignmentId, String studentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        List<DeliveryDTO> deliveries;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new TeacherServiceException("Assignment not found");
        }

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new TeacherServiceException("Student not found");
        }

        deliveries =  assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                .flatMap(x -> x.getDeliveries().stream())
                .map(x -> modelMapper.map(x, DeliveryDTO.class))
                .collect(Collectors.toList());

        return deliveries;
    }

    @Override
    public UserDTO modifyUser(UserDTO userDTO) {
        Optional<User> userOptional = this.userRepository.findByUsername(userDTO.getUsername());
        Optional<Student> studentOptional;
        Optional<Teacher> teacherOptional;
        String[] splittedEmail = userDTO.getEmail().trim().split("@");

        // check if user exists
        if(!userOptional.isPresent()){
            throw new GeneralServiceException("User not found");
        }

        userOptional.get().setName(userDTO.getName());
        userOptional.get().setFirstName(userDTO.getFirstName());
        userOptional.get().setPassword(userDTO.getPassword());

        if (splittedEmail[1].equals("studenti.polito.it")) {
            studentOptional = this.studentRepository.findById(userDTO.getUsername());
            studentOptional.get().setName(userDTO.getName());
            studentOptional.get().setFirstName(userDTO.getFirstName());
        }
        else {
            teacherOptional = this.teacherRepository.findById(userDTO.getUsername());
            teacherOptional.get().setName(userDTO.getName());
            teacherOptional.get().setFirstName(userDTO.getFirstName());
        }

        this.userRepository.save(userOptional.get());

        return modelMapper.map(userOptional.get(), UserDTO.class);
    }

    @Override
    public HomeworkDTO getStudentHomework(Long assignmentId, String studentId) {
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Homework homework;

        // check if student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new StudentServiceException("The assignment does not exists");
        }

        homework = assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                .findFirst().get();

        return modelMapper.map(homework, HomeworkDTO.class);
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
     * Function to get the image of the specified delivery
     * @param deliveryId the id of the delivery
     * @return a byte array of the image associated to the delivery
     */
    @Override
    public byte[] getDeliveryImage(Long deliveryId){
        Optional<Delivery> deliveryOptional = this.deliveryRepository.findById(deliveryId);
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the delivery exists
        if (!deliveryOptional.isPresent()) {
            throw new StudentServiceException();
        }

        try {
            bufferedImage = ImageIO.read(new File(deliveryOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }
        return byteArrayOutputStream.toByteArray();
    }

}
