package it.polito.ai.project.server.services;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
     * @param teahcerId teahcer id
     * @param courseName course name
     * @return true if the course is among the teacher ones
     */
    @Override
    public boolean teacherInCourse(String teahcerId, String courseName) {
        Optional<Teacher> teacherOptional = this.teacherRepository.findById(teahcerId);

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
        courseStudents.forEach(x -> this.removeStudentToCourse(x.getId(), coursename));

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
     * @param course   the courseDTO object to modify
     * @param courseId the courseId
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void modifyCourse(String courseId, CourseDTO course) {
        Course courseToUpdate;
        Optional<Course> courseOptional = courseRepository.findById(courseId);

        // check if the course exists
        if (!courseOptional.isPresent()) {
            throw new CourseNotFoundException();
        }

        // get the course
        courseToUpdate = courseOptional.get();

        // update the course fields
        courseToUpdate.setName(course.getName());
        courseToUpdate.setAcronym(course.getAcronym());
        courseToUpdate.setMin(course.getMin());
        courseToUpdate.setMax(course.getMax());
        courseToUpdate.setEnabled(course.isEnabled());

        this.courseRepository.save(courseToUpdate);
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
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            return false;
        }

        // check if the course already contains the teacher
        if (
                courseOptional
                        .get()
                        .getTeachers()
                        .contains(teacherOptional.get() )
        )
        {
            return false;
        }

        // check if the teacher already has the course
        if(
                teacherOptional
                        .get()
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
    public boolean removeStudentToCourse(String studentId, String courseName) {
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
     * Enable an existing course
     * @param courseName the name of the course to enable
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void enableCourse(String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        courseOptional
                .get()
                .setEnabled(true);
    }

    /**
     * Disable an existing course
     * @param courseName the name of the course to disable
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void disableCourse(String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        courseOptional
                .get()
                .setEnabled(false);
    }

    /**
     * Retrieve the existing team for an existing course
     * @param courseName the course name
     * @return list of all the teams in the course
     */
    @Override
    public List<TeamDTO> getTeamForCourse(String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        return courseOptional
                .get()
                .getTeams()
                .stream()
                .map(x -> modelMapper.map(x, TeamDTO.class))
                .collect(Collectors.toList());
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
    public AssignmentDTO createAssignment(AssignmentDTO assignment,
                                          String courseName, MultipartFile multipartFile) {
        Assignment newAssignemt = new Assignment();
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        File newFile;
        InputStream inputStream = null;
        OutputStream outputStream = null;

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

        // create the assignment
        newAssignemt.setName(assignment.getName());
        newAssignemt.setReleaseDate(new Date(new Timestamp(System.currentTimeMillis()).getTime()));
        newAssignemt.setExpiryDate(assignment.getExpiryDate());
        newAssignemt.setCourse(courseOptional.get());

        // save the assignment
        this.assignmentRepository.save(newAssignemt);

        // set path of the file
        newAssignemt.setPathImage("src/main/resources/images/assignments/" +
                newAssignemt.getId().toString() +
                ".png");

        newFile = new File(newAssignemt.getPathImage());

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
        } catch (IOException e) {
            throw  new TeacherServiceException();
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
        tempdeliveries.forEach(x -> this.deliveryRepository.deleteById(x.getId()));

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

        // remove the assignment
        assignmentRepository.deleteById(assignmentId);

    }

    /**
     * Assign a mark to an homework
     * @param homework the homework to evaluate
     */
    @Override
    public void assignMarkToHomework(HomeworkDTO homework) {
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homework.getId());

        // check if the homework exists
        if(!homeworkOptional.isPresent()){
            throw new TeacherServiceException("Homework does not exists");
        }

        // assign a mark to the homework
        homeworkOptional.get().setMark(homework.getMark());

        // set the flag to false
        homeworkOptional.get().setEditable(false);

        this.homeworkRepository.save(homeworkOptional.get());

    }

    /**
     * Upload the revision of the teacher
     * @param homeworkId the homework id
     * @param multipartFile the review of the teacher
     */
    @Override
    public void revisionDelivery(Long homeworkId, MultipartFile multipartFile) {
        Optional<Homework> homeworkOptional = this.homeworkRepository.findById(homeworkId);
        Delivery delivery = new Delivery();

        // check if the homework exists
        if (!homeworkOptional.isPresent()) {
            throw new StudentServiceException("The homework doesn't exist");
        }

        // set the appropriate path to pathimage
        delivery.setPathImage("path of the file");
        delivery.setStatus(Delivery.Status.REVIEWED);
        delivery.setTimestamp(new Timestamp(System.currentTimeMillis()));
        delivery.setHomework(homeworkOptional.get());

        this.deliveryRepository.save(delivery);

        // save the file in the application

    }


    /**
     * Retrieve the image of the assignment
     * @param assignmentId the id of the assignment
     * @return the image of the assignment
     */
    @Override
    public byte[] getAssignmentImage(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // check if the assignment exists
        if (!assignmentOptional.isPresent()) {
            throw new TeacherServiceException("Assignment does not exist");
        }

        try {
            bufferedImage = ImageIO.read(new File(assignmentOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new StudentServiceException();
        }

        return byteArrayOutputStream.toByteArray();
    }

}
