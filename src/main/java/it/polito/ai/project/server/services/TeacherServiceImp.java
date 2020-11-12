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

import javax.transaction.Transactional;
import java.io.Reader;
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
    TeamRepository teamRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    VMModelRepository vmModelRepository;

    @Autowired
    HomeworkRepository homeworkRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    GeneralServiceImpl generalService;

    TeamServiceImpl teamService;

    /**
     * Add a new student
     * @param student the student object to be created
     * @return a boolean, true if created correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addStudent(StudentDTO student) {
        // check if the student with the same id is already present in the db
        if(this.generalService.getStudent(student.getId()).isPresent()){
            return false;
        }
        this.studentRepository.save(modelMapper.map(student, Student.class));
        return true;
    }

    /**
     * Add a course that is not already present in the db
     * @param course the course with all the specs
     * @return boolean, true if is created correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addCourse(CourseDTO course) {
        // check if there is already a course with the same name
        if(generalService.getCourse(course.getName()).isPresent()){
            return false;
        }

        // add the course
        this.courseRepository.save(modelMapper.map(course, Course.class));
        return true;
    }

    /**
     * remove a course
     * @param course the courseDTO object to remove
     * @return true if successful;
     * false otherwise.
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean removeCourse(CourseDTO course) {
        Optional<Course> courseOptional = this.courseRepository.findById(course.getName());
        VMModel tempvm;

        // check if the course exists
        if (!(courseOptional.isPresent())) {
            return false;
        }

        // removing the students from the course
        courseOptional.get().getStudents().forEach(x -> courseOptional.get().removeStudent(x));

        // removing the teachers from the course
        courseOptional.get().getTeachers().forEach(x -> courseOptional.get().removeTeacher(x));

        // storing the virtual machine to remove
        tempvm = this.vmModelRepository.findById(courseOptional.get().getVmModel().getId()).get();

        // removing the virtual machine model from the course
        courseOptional.get().setVMModel(null);

        // removing the VM model from the repository
        this.vmModelRepository.deleteById(tempvm.getId());

        // removing the assignments from the course
        courseOptional.get().getAssignments().stream()
                .map(x -> modelMapper.map(x, AssignmentDTO.class))
                .forEach(x -> removeAssignment(x, courseOptional.get().getName()));

        // removing the teams from the course
        courseOptional.get().getTeams().forEach(x -> this.teamService.evictTeam(x.getId()));

        this.courseRepository.deleteById(course.getName());

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

        courseOptional
                .get()
                .addStudent(studentOptional.get());
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

        courseOptional
                .get()
                .removeStudent(studentOptional.get());
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
     * Add a list of new students
     * @param students the list of new students to add
     * @return a boolean list, the value of the list is true if the
     *         respective student was added correctly, false otherwise
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<Boolean> addAll(List<StudentDTO> students) {
        return students.stream()
                .map(x -> this.addStudent(x))
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
    public List<Boolean> addAndEroll(Reader r, String courseName) {
        // create a csv reader
        CsvToBean<StudentDTO> csvToBean = new CsvToBeanBuilder(r)
                .withType(StudentDTO.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        // convert `CsvToBean` object to list of students
        List<StudentDTO> studentDTOS = csvToBean.parse();

        // add all the students to the db
        this.addAll(studentDTOS);

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
    public void createAssignment(AssignmentDTO assignment, String courseName) {
        Assignment newAssignemt = new Assignment();
        Optional<Course> courseOptional = courseRepository.findById(courseName);

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
        newAssignemt.setReleaseDate(assignment.getReleaseDate());
        newAssignemt.setExpiryDate(assignment.getExpiryDate());
        newAssignemt.setPathImage(assignment.getPathImage());
        newAssignemt.setCourse(courseOptional.get());

        // save the assignment
        this.assignmentRepository.save(newAssignemt);

    }

    /**
     * remore an assignment and all relations with other entities
     * @param assignment the assignment to remove
     * @param courseName the course to which the assignment belongs
     */
    @Override
    public void removeAssignment(AssignmentDTO assignment, String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);
        Optional<Assignment> assignmentOptional = assignmentRepository.findById(assignment.getId());
        List<Delivery> tempdeliveries;
        List<Homework> temphomeworks;

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
        temphomeworks = assignmentOptional.get().getHomeworks();

        // remove the homeworks from the student
        temphomeworks.forEach(x -> x.getStudent().removeHomework(x));

        // remove homeworks from the assignment
        temphomeworks.forEach(x -> assignmentOptional.get().removeHomework(x));

        // remove the homwork from the repository
        temphomeworks.forEach(x -> this.homeworkRepository.deleteById(x.getId()));

        // remove the course from the assignment
        assignmentOptional.get().setCourse(null);

        // remove the assignment
        assignmentRepository.deleteById(assignment.getId());

    }

}
