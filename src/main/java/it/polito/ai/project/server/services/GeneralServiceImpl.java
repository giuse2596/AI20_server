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
import java.util.HashMap;
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
    TeamRepository teamRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    ModelMapper modelMapper;

    /**
     * Retrieve all the courses present in the db
     * @return a list of all the courses present in the db
     */
    @Override
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(x -> modelMapper.map(x, CourseDTO.class))
                .collect(Collectors.toList());
    }

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
     * Retrieve all students enrolled in a specific existing course
     * @param courseName the course string to retrieve all the students enrolled in it
     * @return a list of students
     */
    @Override
    public List<StudentDTO> getEnrolledStudents(String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        return courseOptional
                .get()
                .getStudents()
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
            throw new CourseNotFoundException();
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
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<Homework> homeworkOptional;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new GeneralServiceException();
        }

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        homeworkOptional = assignmentOptional.get().getHomeworks()
                    .stream()
                    .filter(x -> x.getStudent().getId().equals(studentId))
                    .findFirst();

        // check if there is exists, assignment and student not correlated
        if (!homeworkOptional.isPresent()) {
            throw new GeneralServiceException();
        }

        return modelMapper.map(
                homeworkOptional.get()
                .getDeliveries()
                .get(homeworkOptional.get().getDeliveries().size()-1),
                DeliveryDTO.class);
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
        List<Homework> homeworkList;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new GeneralServiceException();
        }

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        homeworkList =  assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                .collect(Collectors.toList());

        // check if there is at least one homework in case
        // assignment and student are not correlated
        if (homeworkList.size() == 0) {
            throw new GeneralServiceException();
        }

        // there is always only one homework
        return homeworkList.get(0).getDeliveries()
                .stream()
                .map(x -> modelMapper.map(x, DeliveryDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve student homework given an assignment
     * @param assignmentId assignment id
     * @param studentId student id
     * @return homework dto
     */
    @Override
    public HomeworkDTO getStudentHomework(Long assignmentId, String studentId) {
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<Homework> homeworkOptional;

        // check if student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new GeneralServiceException();
        }

        homeworkOptional = assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                .findFirst();

        // check if the homework exists
        if(!homeworkOptional.isPresent()){
            throw new GeneralServiceException();
        }

        return modelMapper.map(homeworkOptional.get(), HomeworkDTO.class);
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
            throw new GeneralServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new GeneralServiceException("Course not enabled");
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new GeneralServiceException("Team not active");
        }

        return this.modelMapper.map(virtualMachineOptional.get(),
                VirtualMachineDTO.class);
    }

    /**
     * Retrieve all the team virtual machine
     * @param teamId the team id
     * @return all the team virtual machine
     */
    @Override
    public List<VirtualMachineDTO> getTeamVirtualMachines(Long teamId){
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);

        // check if the team exists
        if (!teamOptional.isPresent()) {
            throw new TeamNotFoundException();
        }

        // check if the course is enabled
        if(!teamOptional.get().getCourse().isEnabled()){
            throw new GeneralServiceException("Course not enabled");
        }

        // check if the team is active
        if(!teamOptional.get().isActive()){
            throw new GeneralServiceException("Team not active");
        }

        return teamOptional.get()
                .getVirtualMachines()
                .stream()
                .map(x -> modelMapper.map(x, VirtualMachineDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentDTO> getVirtualMachineOwners(Long vmId) {
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);

        // check if the vm exists
        if(!virtualMachineOptional.isPresent()){
            throw new GeneralServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new GeneralServiceException("Course not enabled");
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new GeneralServiceException("Team not active");
        }

        return virtualMachineOptional.get().getOwners()
                .stream()
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Check if the virtual machine's image can be retrieved
     * @param vmId the virtual machine id
     * @return true if the check is passed,
     *          false if the virtual machine is not active;
     */
    @Override
    public byte[] getVirtualMachineImage(Long vmId) {
        Optional<VirtualMachine> virtualMachineOptional = this.virtualMachinesRepository.findById(vmId);
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the vm exists
        if(!virtualMachineOptional.isPresent()){
            throw new GeneralServiceException("Virtual machine doesn't exist");
        }

        // check if the course is enabled
        if(!virtualMachineOptional.get().getTeam().getCourse().isEnabled()){
            throw new GeneralServiceException("Course not enabled");
        }

        // check if the team is active
        if(!virtualMachineOptional.get().getTeam().isActive()){
            throw new GeneralServiceException("Team not active");
        }

        // check if virtual machine is active
        if(!virtualMachineOptional.get().isActive()){
            throw new GeneralServiceException("Virtual machine not active");
        }

        try {
            bufferedImage = ImageIO.read(new File(virtualMachineOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new GeneralServiceException("Error while reading the file");
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
            throw new GeneralServiceException();
        }

        try {
            bufferedImage = ImageIO.read(new File(deliveryOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new GeneralServiceException();
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Retrieve the available resources of a team
     * @param teamId the team id
     * @return a map with all the values of the available resources
     */
    @Override
    public HashMap<String, Integer> getVMAvailableResources(Long teamId) {
        HashMap<String, Integer> resources = new HashMap<>();
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);
        int totCpu;
        int totRam;
        int totDiskSpace;
        int totVM;
        int activeVM;

        // check if the team exists
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        // sum the total cpu of all virtual machines
        totCpu = teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getCpu)
                .reduce(0, Integer::sum);

        resources.put("cpu",teamOptional.get().getCpuMax() - totCpu);

        // sum the total ram of all virtual machines
        totRam = teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getRam)
                .reduce(0, Integer::sum);

        resources.put("ram",teamOptional.get().getRamMax() - totRam);

        // sum the total disk space of all virtual machines
        totDiskSpace = teamOptional.get().getVirtualMachines()
                .stream()
                .map(VirtualMachine::getDiskSpace)
                .reduce(0, Integer::sum);

        resources.put("diskSpace",teamOptional.get().getDiskSpaceMax() - totDiskSpace);

        // sum the total virtual machines created in the team
        totVM = teamOptional.get().getVirtualMachines().size();

        resources.put("totVM", totVM);

        // sum all the active virtual machines created in the team
        activeVM = (int) teamOptional.get().getVirtualMachines()
                .stream()
                .filter(x -> x.isActive())
                .count();

        resources.put("activeVM", activeVM);

        return resources;
    }

}
