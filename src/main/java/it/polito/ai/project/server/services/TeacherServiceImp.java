package it.polito.ai.project.server.services;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherServiceImp implements TeacherService {
    @Autowired
    CourseRepository courseRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    VirtualMachinesRepository virtualMachinesRepository;

    @Autowired
    VMModelRepository vmModelRepository;

    @Autowired
    HomeworkRepository homeworkRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    GeneralServiceImpl generalService;

    @Autowired
    TeamServiceImpl teamService;

    /**
     * Function to see if a theacher belongs to a course
     * @param teacherId teahcer id
     * @param courseName course name
     * @return true if the course is among the teacher ones
     */
    @Override
    public boolean teacherInCourse(String teacherId, String courseName) {
        Optional<Teacher> teacherOptional = this.teacherRepository.findById(teacherId);
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);

        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        if(!teacherOptional.isPresent()){
            throw new TeacherServiceException("Teacher not found");
        }

        // check if the teacher is in the course
        if(!teacherOptional.get().getCourses()
                .stream()
                .map(x -> x.getName())
                .collect(Collectors.toList())
                .contains(courseName)){
            return false;
        }

        return true;
    }

    /**
     * Retrieve all the teachers of a course
     * @param courseName the name of the course
     * @return the list of all the teachers of a course
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<TeacherDTO> teachersNotInCourse(String courseName) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        return this.courseRepository.getTeachersNotInCourse(courseName)
                .stream()
                .map(x -> modelMapper.map(x, TeacherDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Add a course that is not already present in the db
     * @param course the course with all the specs
     * @return boolean, true if is created correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addCourse(CourseDTO course, String teacherId, VMModelDTO vmModelDTO) {
        VMModel vmModel = new VMModel();

        // check if there is already a course with the same name
        if(generalService.getCourse(course.getName()).isPresent()){
            return false;
        }

        // check if the teacher exists
        if(!teacherRepository.findById(teacherId).isPresent()){
            return false;
        }

        // set the course enabled
        course.setEnabled(true);

        vmModel.setCpuMax(vmModelDTO.getCpuMax());
        vmModel.setRamMax(vmModelDTO.getRamMax());
        vmModel.setDiskSpaceMax(vmModelDTO.getDiskSpaceMax());
        vmModel.setTotalInstances(vmModelDTO.getTotalInstances());
        vmModel.setActiveInstances(vmModelDTO.getActiveInstances());

        // add the course
        this.courseRepository.save(modelMapper.map(course, Course.class));

        vmModel.setCourse(this.courseRepository.findById(course.getName()).get());

        this.vmModelRepository.save(vmModel);

        // set the teacher to the course
        this.addTeacherToCourse(teacherId, course.getName());

        return true;
    }

    /**
     * remove a course
     * @param coursename the name of the course
     * @return true if successful;
     * false otherwise.
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean removeCourse(String coursename) {
        Optional<Course> courseOptional = this.courseRepository.findById(coursename);
        List<Student> courseStudents = new ArrayList<>();
        List<Teacher> courseTeachers = new ArrayList<>();
        List<Assignment> courseAssignments = new ArrayList<>();
        List<Team> courseTeams = new ArrayList<>();
        VMModel tempvm;

        // check if the course exists
        if (!(courseOptional.isPresent())) {
            return false;
        }

        // removing the students from the course
        courseOptional.get().getStudents().forEach(x -> courseStudents.add(x));
        courseStudents.forEach(x -> this.removeStudentToCourse(x.getId(), coursename, true));

        // removing the teachers from the course
        courseOptional.get().getTeachers().forEach(x -> courseTeachers.add(x));
        courseTeachers.forEach(x -> courseOptional.get().removeTeacher(x));

        // storing the virtual machine to remove
        tempvm = this.vmModelRepository.findById(courseOptional.get().getVmModel().getId()).get();

        // removing the virtual machine model from the course
        courseOptional.get().setVMModel(null);

        // removing the VM model from the repository
        this.vmModelRepository.deleteById(tempvm.getId());

        // removing the assignments from the course
        courseOptional.get().getAssignments().forEach(x -> courseAssignments.add(x));
        courseAssignments.forEach(x -> removeAssignment(x.getId(), coursename));

        // removing the teams from the course
        courseOptional.get().getTeams().forEach(x -> courseTeams.add(x));
        courseTeams.forEach(x -> this.teamService.evictTeam(x.getId()));

        this.courseRepository.deleteById(coursename);

        return true;
    }

    /**
     * modify the course attributes
     * @param courseDTO the course dto
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void modifyCourse(CourseDTO courseDTO) {
        Optional<Course> courseOptional = courseRepository.findById(courseDTO.getName());

        // check if the course exists
        if (!courseOptional.isPresent()) {
            throw new CourseNotFoundException();
        }

        // check if the course sent and the one in the db are both not enabled
        // otherwise it means the teacher want to enable the course
        if (!courseOptional.get().isEnabled() & !courseDTO.isEnabled()) {
            throw new TeacherServiceException("Course not enabled");
        }

        // check if there are teams in a course
        if(courseOptional.get().getTeams().size() > 0){

            // check if there are teams with a number of members
            // smaller than the min value or grater than the max
            if(
                    courseOptional.get().getTeams()
                    .stream()
                    .map(x -> x.getMembers().size())
                    .min(Integer::compare).get() < courseDTO.getMin()
                    |
                    courseOptional.get().getTeams()
                    .stream()
                    .map(x -> x.getMembers().size())
                    .max(Integer::compare).get() > courseDTO.getMax()
            ){
                throw new TeacherServiceException("Min or max values cannot be changed");
            }
        }

        // update the course fields
        courseOptional.get().setMin(courseDTO.getMin());
        courseOptional.get().setMax(courseDTO.getMax());
        courseOptional.get().setEnabled(courseDTO.isEnabled());

        this.courseRepository.save(courseOptional.get());
    }

    /**
     * Add an existing student in an existing course
     * @param studentId the student to enroll
     * @param courseName the course in which the student must be enrolled
     * @return boolean, true if enrolled correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addStudentToCourse(String studentId, String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Student> studentOptional = studentRepository.findById(studentId);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }
        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            return false;
        }

        // check if the course already contains the student
        if (
               courseOptional
                        .get()
                        .getStudents()
                        .contains(studentOptional.get() )
        )
        {
            return false;
        }

        // check if the student already has the course
        if(
                studentOptional
                        .get()
                        .getCourses()
                        .contains(courseOptional.get() )
        )
        {
            return false;
        }

        courseOptional.get().addStudent(studentOptional.get());

        return true;
    }

    /**
     * Add a teacher to a course
     * @param teacherId the teacher id
     * @param courseName the name of the course
     * @return true for success;
     *          false if fail.
     */
    @Override
    public boolean addTeacherToCourse(String teacherId, String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Teacher> teacherOptional = teacherRepository.findById(teacherId);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the teacher exist
        if(!teacherOptional.isPresent()){
            throw new TeacherServiceException();
        }

        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            return false;
        }

        // check if the course already contains the teacher
        if (
                courseOptional.get()
                        .getTeachers()
                        .contains(teacherOptional.get() )
        )
        {
            return false;
        }

        // check if the teacher already has the course
        if(
                teacherOptional.get()
                        .getCourses()
                        .contains(courseOptional.get() )
        )
        {
            return false;
        }

        courseOptional.get().addTeacher(teacherOptional.get());

        this.courseRepository.save(courseOptional.get());

        return true;
    }

    /**
     * remove a student from a course
     * @param studentId the student id
     * @param courseName the course to remove the student from
     * @return true if successful;
     *         false otherwise.
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean removeStudentToCourse(String studentId, String courseName, boolean deleteCourse) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Student> studentOptional = studentRepository.findById(studentId);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the student exist
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled and if the course is beeing deleted
        if(!courseOptional.get().isEnabled() & !deleteCourse){
            return false;
        }

        // check if the course contains the student
        if (
                !courseOptional
                        .get()
                        .getStudents()
                        .contains(studentOptional.get() )
        )
        {
            return false;
        }

        // check if the student has the course
        if(
                !studentOptional
                        .get()
                        .getCourses()
                        .contains(courseOptional.get() )
        )
        {
            return false;
        }

        // removing the student from the virtual machines
        studentOptional.get().getTeams()
                .stream()
                .filter(x -> x.getCourse().getName().equals(courseName))
                .flatMap(x -> x.getVirtualMachines().stream())
                .filter(x -> x.getOwners().contains(studentOptional.get()))
                .forEach(x -> {
                    x.removeOwner(studentOptional.get());
                    // if the size of the owners is zero then remove the virtual machine
                    // from the team and from the repository
                    if(x.getOwners().size() == 0){
                        x.setTeam(null);
                        this.vmModelRepository.deleteById(x.getId());
                    }
                });

        // removing the student from the team
        studentOptional.get().getTeams()
                .stream()
                .filter(x -> x.getCourse().getName().equals(courseName))
                .forEach(x -> {
                    x.removeMember(studentOptional.get());
                    if(x.getMembers().size() == 0){
                        teamService.evictTeam(x.getId());
                    }
                });

        // removing the student from the course
        courseOptional.get().removeStudent(studentOptional.get());

        return true;
    }

    /**
     * Enroll to an existing course a list of existing students
     * @param studentIds the list of students to add to the course
     * @param courseName the course name to add students to
     * @return a boolean list, the value of the list is true if the
     *         respective student was added correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<Boolean> enrollAll(List<String> studentIds, String courseName) {
        long studentsNotPresent;

        // check if the students are present
        studentsNotPresent = studentIds
                .stream()
                .map(x -> this.studentRepository.findById(x).isPresent())
                .filter(x -> !x)
                .count();

        if(studentsNotPresent > 0){
            throw new StudentNotFoundExeption();
        }

        return studentIds.stream()
                .map(x -> this.addStudentToCourse(x,courseName))
                .collect(Collectors.toList());
    }

    /**
     * Add and enroll to an existing course a list of non existing students
     * the list is passed through a csv file
     * @param r the file Reader
     * @param courseName the course name to add students to
     * @return a boolean list, the value of the list is true if the
     *         respective student was added correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<Boolean> enrollCSV(Reader r, String courseName) {
        // create a csv reader
        CsvToBean<StudentDTO> csvToBean = new CsvToBeanBuilder(r)
                .withType(StudentDTO.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        // convert `CsvToBean` object to list of students
        List<StudentDTO> studentDTOS = csvToBean.parse();

        // enroll all students to the course
        return enrollAll(
                studentDTOS.stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList()),
                courseName
        );
    }

    /**
     * change the limit of resources available for a team
     * @param newTeam the team object with the new parameters
     * @param courseName the name of the course
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void changeVMvalues(TeamDTO newTeam, String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Team> teamOptional = teamRepository.findById(newTeam.getId());
        Team teamToUpdate;

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            throw new TeacherServiceException("The course is not enabled");
        }

        // check if the team exists
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        // get the team to update
        teamToUpdate = teamOptional.get();

        // check if the total cpu of the instances of
        // team's virtual machines is greater than the max
        if(teamToUpdate.getVirtualMachines()
                .stream()
                .map(x -> x.getCpu())
                .mapToInt(x -> x)
                .sum() > newTeam.getCpuMax()){
            throw new TeacherServiceException("Cannot change cpu value");
        }

        // check if the total ram of the instances of
        // team's virtual machines is greater than the max
        if(teamToUpdate.getVirtualMachines()
                .stream()
                .map(x -> x.getRam())
                .mapToInt(x -> x)
                .sum() > newTeam.getCpuMax()){
            throw new TeacherServiceException("Cannot change ram value");
        }

        // check if the total disk space of the instances of
        // team's virtual machines is greater than the max
        if(teamToUpdate.getVirtualMachines()
                .stream()
                .map(x -> x.getDiskSpace())
                .mapToInt(x -> x)
                .sum() > newTeam.getCpuMax()){
            throw new TeacherServiceException("Cannot change disk space value");
        }

        // check if the total active team's virtual machines is greater than the max
        if(teamToUpdate.getVirtualMachines()
                .stream()
                .filter(x -> x.isActive())
                .count() > newTeam.getActiveVM()){
            throw new TeacherServiceException("Cannot change max active virtual machine value");
        }

        // apply the changes to the team
        teamToUpdate.setCpuMax(newTeam.getCpuMax());
        teamToUpdate.setRamMax(newTeam.getRamMax());
        teamToUpdate.setDiskSpaceMax(newTeam.getDiskSpaceMax());
        teamToUpdate.setTotVM(newTeam.getTotVM());
        teamToUpdate.setActiveVM(newTeam.getActiveVM());

        this.teamRepository.save(teamToUpdate);

    }

    /**
     * create an assignment for a course
     * @param assignment the new assignment
     * @param courseName course's name to which the assignment refers
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public AssignmentDTO createAssignment(AssignmentDTO assignment, String courseName) {
        Assignment newAssignemt = new Assignment();
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<String> extension;
        File newFile;
        InputStream inputStream;
        OutputStream outputStream;

        extension = Optional.ofNullable(assignment.getMultipartFile().getOriginalFilename())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(
                        assignment.getMultipartFile().getOriginalFilename().lastIndexOf(".") + 1
                        )
                );

        // check if the file has an extension
        if(!extension.isPresent()){
            throw new NoExtensionException();
        }

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            throw new TeacherServiceException("Course not enabled");
        }

        // check if the name of the assignment already exists in the course
        if(assignmentRepository.findAll().stream()
                .filter(x -> x.getCourse().getName().equals(courseName))
                .filter(x -> x.getName().equals(assignment.getName()))
                .count() > 0
        ){
            throw new TeacherServiceException("Assignment name already exists for this course");
        }

        // check if the expiry date is not the same of the creation day or a previous day
        if(assignment.getExpiryDate().before(new Date(new Time(System.currentTimeMillis()).getTime()))){
            throw new TeacherServiceException("The expiry date do not must be the current or previous day");
        }

        // create the assignment
        newAssignemt.setName(assignment.getName());
        newAssignemt.setReleaseDate(new Date(new Timestamp(System.currentTimeMillis()).getTime()));
        newAssignemt.setExpiryDate(assignment.getExpiryDate());
        newAssignemt.setCourse(courseOptional.get());
        newAssignemt.setPathImage("src/main/resources/images/deliveries/empty_image.png");

        // save the assignment
        this.assignmentRepository.save(newAssignemt);

        // set path of the file
        newAssignemt.setPathImage("src/main/resources/images/assignments/" +
                newAssignemt.getId().toString() +
                "." + extension.get());

        newFile = new File(newAssignemt.getPathImage());

        try {
            inputStream = assignment.getMultipartFile().getInputStream();

            if (!newFile.exists()) {
                newFile.getParentFile().mkdir();
            }
            outputStream = new FileOutputStream(newFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            throw  new TeacherServiceException("Error reading the file");
        }

        // create homework with a delivery with the NULL status for all students
        courseOptional.get().getStudents()
                .forEach(x -> {
                    Homework studentHomework = new Homework();
                    Delivery firstDelivery = new Delivery();
                    studentHomework.setEditable(true);
                    studentHomework.setStudent(x);
                    studentHomework.setAssignment(newAssignemt);
                    // adding a delivery to the homework with NULL state
                    firstDelivery.setHomework(studentHomework);
                    firstDelivery.setStatus(Delivery.Status.NULL);
                    firstDelivery.setPathImage("src/main/resources/images/deliveries/empty_image.png");
                    firstDelivery.setTimestamp(new Timestamp(System.currentTimeMillis()));
                    this.homeworkRepository.save(studentHomework);
                    this.deliveryRepository.save(firstDelivery);
                });

        return modelMapper.map(newAssignemt, AssignmentDTO.class);
    }

    /**
     * remore an assignment and all relations with other entities
     * @param assignmentId the assignment to remove
     * @param courseName the course to which the assignment belongs
     */
    @Override
    public void removeAssignment(Long assignmentId, String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Assignment> assignmentOptional = assignmentRepository.findById(assignmentId);
        List<Delivery> tempdeliveries;
        List<Homework> temphomeworks = new ArrayList<>();

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new TeacherServiceException("Assignment does not exists");
        }

        // store the deliveries to remove
        tempdeliveries = assignmentOptional.get().getHomeworks()
                            .stream()
                            .flatMap(x -> x.getDeliveries().stream())
                            .collect(Collectors.toList());

        // remove the deliveries from the homework
        tempdeliveries.forEach(x -> x.setHomework(null));

        // remove all deliveries form the repository
        tempdeliveries.forEach(x -> {

            // if the image of the delivery is not the default one it is deleted
            if(!x.getPathImage().equals("src/main/resources/images/deliveries/empty_image.png")){
                try {
                    Files.delete(Paths.get(x.getPathImage()));
                } catch (IOException e) {
                    throw new UserServiceException("Error deleting the old image");
                }
            }

            this.deliveryRepository.deleteById(x.getId());
        });

        // store the homeworks to remove
        assignmentOptional.get().getHomeworks().forEach(x -> temphomeworks.add(x));

        // remove the homeworks from the student
        temphomeworks.forEach(x -> x.getStudent().removeHomework(x));

        // remove homeworks from the assignment
        temphomeworks.forEach(x -> assignmentOptional.get().removeHomework(x));

        // remove the homwork from the repository
        temphomeworks.forEach(x -> this.homeworkRepository.deleteById(x.getId()));

        // remove the course from the assignment
        assignmentOptional.get().setCourse(null);

        try {
            Files.delete(Paths.get(assignmentOptional.get().getPathImage()));
        } catch (IOException e) {
            throw new TeacherServiceException("Error deleting the old image");
        }

        // remove the assignment
        assignmentRepository.deleteById(assignmentId);

    }

    /**
     * Assign a mark to an homework
     * @param homeworkDTO the homework with the mark to set
     */
    @Override
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public void assignMarkToHomework(HomeworkDTO homeworkDTO) {
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkDTO.getId());

        // check if the homework exists
        if(!homeworkOptional.isPresent()){
            throw new TeacherServiceException("Homework does not exists");
        }

        // assign a mark to the homework
        homeworkOptional.get().setMark(homeworkDTO.getMark());

        // set the flag to false
        homeworkOptional.get().setEditable(homeworkDTO.isEditable());

        this.homeworkRepository.save(homeworkOptional.get());

    }

    /**
     * Upload the revision of the teacher
     * @param homeworkId the homework id
     * @param multipartFile the review of the teacher
     */
    @Override
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public DeliveryDTO revisionDelivery(Long homeworkId, MultipartFile multipartFile) {
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkId);
        Optional<String> extension;
        Delivery delivery = new Delivery();
        File newFile;
        InputStream inputStream;
        OutputStream outputStream;

        extension = Optional.ofNullable(multipartFile.getOriginalFilename())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(multipartFile.getOriginalFilename().lastIndexOf(".") + 1));

        // check if file has an extension
        if(!extension.isPresent()){
            throw new NoExtensionException();
        }

        // check if the homework exists
        if (!homeworkOptional.isPresent()) {
            throw new TeacherServiceException("The homework doesn't exist");
        }

        // check if is possible to review the delivery
        if(!homeworkOptional.get().getDeliveries()
                .get(homeworkOptional.get().getDeliveries().size()-1)
                .getStatus().equals(Delivery.Status.DELIVERED)){
            throw new TeacherServiceException("Cannot review the delivery");
        }

        delivery.setStatus(Delivery.Status.REVIEWED);
        delivery.setTimestamp(new Timestamp(System.currentTimeMillis()));
        delivery.setHomework(homeworkOptional.get());

        this.deliveryRepository.save(delivery);

        delivery.setPathImage("src/main/resources/images/deliveries/" +
                delivery.getId().toString() + "." + extension.get());

        // save the file in the application
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
            throw new TeacherServiceException("Error saving the file");
        }

        return modelMapper.map(delivery, DeliveryDTO.class);
    }


    /**
     * Retrieve the image of the assignment
     * @param assignmentId the id of the assignment
     * @return the image of the assignment
     */
    @Override
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public byte[] getAssignmentImage(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<String> extension;
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the assignment exists
        if (!assignmentOptional.isPresent()) {
            throw new TeacherServiceException("Assignment does not exist");
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
            throw new TeacherServiceException("Error reading the file");
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Function to retrieve vm model dto
     * @param courseName course name
     * @return vm model dto
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public VMModelDTO getVMModel(String courseName) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        Optional<VMModel> vmModel;

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        vmModel = this.vmModelRepository.findAll()
                .stream()
                .filter(x -> x.getCourse().getName().equals(courseName))
                .findFirst();

        // check if the vm model is present
        if(!vmModel.isPresent()){
            throw new TeacherServiceException("Virtual machine model does not exists");
        }

        return modelMapper.map(vmModel.get(), VMModelDTO.class);
    }

    /**
     * Set an homework to editable/not editable
     * @param homeworkDTO homework dto with the editable field setted
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void setEditableHomework(HomeworkDTO homeworkDTO) {
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkDTO.getId());

        // check if the homework exists
        if(!homeworkOptional.isPresent()){
            throw new TeacherServiceException("Homework does not exists");
        }

        // set the flag to false
        homeworkOptional.get().setEditable(homeworkDTO.isEditable());

        this.homeworkRepository.save(homeworkOptional.get());
    }

    /**
     * Function to retrieve teacher courses
     * @param teacherId teacher id
     * @return list of teacher courses dto
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<CourseDTO> getTeacherCourses(String teacherId) {
        Optional<Teacher> teacherOptional = this.teacherRepository.findById(teacherId);

        if(!teacherOptional.isPresent()){
            throw new TeacherServiceException("Teacher does not exists");
        }

        return teacherOptional.get().getCourses()
                .stream()
                .map(x -> modelMapper.map(x, CourseDTO.class))
                .collect(Collectors.toList());

    }

    /**
     * Function to retrieve assignment homework
     * @param assignmentId assignment id
     * @return list of assignment homework
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<HomeworkDTO> getHomework(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);

        // check if assignment exists
        if (!assignmentOptional.isPresent()) {
            throw new TeacherServiceException();
        }

        return assignmentOptional.get()
                .getHomeworks()
                .stream()
                .map(x -> this.modelMapper.map(x, HomeworkDTO.class))
                .collect(Collectors.toList());
    }

}
