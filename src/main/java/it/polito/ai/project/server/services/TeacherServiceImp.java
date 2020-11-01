package it.polito.ai.project.server.services;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.AssignmentRepository;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeamRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.Reader;
import java.util.Date;
import java.util.List;
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

    GeneralServiceImpl generalService;

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
        // check if the course exists
        if (!(this.courseRepository.findById(course.getName()).isPresent())) {
            return false;
        }
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

        // check if the course exists
        if (!courseRepository.findById(course.getName()).isPresent()) {
            throw new CourseNotFoundException();
        }

        // get the course
        courseToUpdate = courseRepository.findById(courseId).get();

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
        // check if the course exists
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        return courseRepository.findById(courseName)
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
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        // check if the student exist
        if(!studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }
        // check if the course is enabled
        if(!courseRepository.findById(courseName).get().isEnabled()){
            return false;
        }

        // check if the course already contains the student
        if (
                courseRepository.findById(courseName)
                        .get()
                        .getStudents()
                        .contains(studentRepository.findById(studentId).get() )
        )
        {
            return false;
        }

        // check if the student already has the course
        if(
                studentRepository.findById(studentId)
                        .get()
                        .getCourses()
                        .contains(courseRepository.findById(courseName).get() )
        )
        {
            return false;
        }

        courseRepository.findById(courseName)
                .get()
                .addStudent(studentRepository.findById(studentId).get());
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
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        // check if the student exist
        if(!studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }
        // check if the course is enabled
        if(!courseRepository.findById(courseName).get().isEnabled()){
            return false;
        }

        // check if the course contains the student
        if (
                !courseRepository.findById(courseName)
                        .get()
                        .getStudents()
                        .contains(studentRepository.findById(studentId).get() )
        )
        {
            return false;
        }

        // check if the student has the course
        if(
                !studentRepository.findById(studentId)
                        .get()
                        .getCourses()
                        .contains(courseRepository.findById(courseName).get() )
        )
        {
            return false;
        }

        courseRepository.findById(courseName)
                .get()
                .removeStudent(studentRepository.findById(studentId).get());
        return true;
    }

    /**
     * Enable an existing course
     * @param courseName the name of the course to enable
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public void enableCourse(String courseName) {
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        courseRepository.findById(courseName)
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
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        courseRepository.findById(courseName)
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
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        return courseRepository.findById(courseName)
                .get()
                .getTeams()
                .stream()
                .map(x -> modelMapper.map(x, TeamDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * get the VM of a team
     * @param teamId the team id
     * @return the virtual machine of the team
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<VirtualMachineDTO> getVMForTeam(Long teamId) {
        // check if the team exists
        if(!teamRepository.findById(teamId).isPresent()){
            throw new TeamNotFoundException();
        }
        return teamRepository.findById(teamId).get().getVirtualMachines().stream()
                .map(x -> modelMapper.map(x, VirtualMachineDTO.class))
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
        Team teamToUpdate;

        // check if the course exists
        if(!this.courseRepository.findById(courseName).isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the course is enabled
        if(!this.courseRepository.findById(courseName).get().isEnabled()){
            throw new TeacherServiceException("The course is not enabled");
        }

        // check if the team exists
        if(!this.getTeamForCourse(courseName).contains(this.teamRepository.findById(newTeam.getId()))){
            throw new TeamNotFoundException();
        }

        // get the team to update
        teamToUpdate = this.teamRepository.findById(newTeam.getId()).get();

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

        // check if the course exists
        if(!this.courseRepository.findById(courseName).isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the course is enabled
        if(!this.courseRepository.findById(courseName).get().isEnabled()){
            throw new TeacherServiceException("Course not enabled");
        }

        // check if the name of the assignment already exists
        if(this.assignmentRepository.findById(assignment.getId()).isPresent()){
            throw new TeacherServiceException("Assignment already exists");
        }

        // create the assignment
        newAssignemt.setId(assignment.getId());
        newAssignemt.setReleaseDate(assignment.getReleaseDate());
        newAssignemt.setExpiryDate(assignment.getExpiryDate());
        newAssignemt.setPathImage(assignment.getPathImage());
        newAssignemt.setCourse(this.courseRepository.findById(courseName).get());

        // save the assignment
        this.assignmentRepository.save(newAssignemt);

    }

}
