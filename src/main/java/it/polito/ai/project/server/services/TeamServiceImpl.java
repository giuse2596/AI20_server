package it.polito.ai.project.server.services;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Team;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeamRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {
    @Autowired
    CourseRepository courseRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    ModelMapper modelMapper;

    // Add a new course
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addCourse(CourseDTO course) {
        // check if there is already a course with the same name
        if(this.getCourse(course.getName()).isPresent()){
            return false;
        }

        // add the course
        this.courseRepository.save(modelMapper.map(course, Course.class));
        return true;
    }

    // Retrieve an existing course
    @Override
    public Optional<CourseDTO> getCourse(String name) {
        return courseRepository.findById(name).map(x -> modelMapper.map(x, CourseDTO.class));
    }

    // Retrieve all the courses present in the db
    @Override
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(x -> modelMapper.map(x, CourseDTO.class))
                .collect(Collectors.toList());
    }

    // Add a new student
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public boolean addStudent(StudentDTO student) {
        // check if the student with the same id is already present in the db
        if(this.getStudent(student.getId()).isPresent()){
            return false;
        }
        this.studentRepository.save(modelMapper.map(student, Student.class));
        return true;
    }

    // Retrieve a student
    @Override
    public Optional<StudentDTO> getStudent(String studentId) {
        return studentRepository.findById(studentId)
                                .map(x -> modelMapper.map(x, StudentDTO.class));
    }

    // Retrieve all students
    @Override
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

    // Retrieve all students enrolled in a specific existing course
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

    // Add an existing student in an existing course
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

    // Enable an existing course
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

    // Disable an existing course
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

    // Add a list of non existing students
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<Boolean> addAll(List<StudentDTO> students) {
        return students.stream()
                .map(x -> this.addStudent(x))
                .collect(Collectors.toList());
    }

    // Enroll to an existing course a list of existing students
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<Boolean> enrollAll(List<String> studentIds, String courseName) {
        return studentIds.stream()
                .map(x -> this.addStudentToCourse(x,courseName))
                .collect(Collectors.toList());
    }

    // Add and enroll to an existing course a list of non existing students
    // the list is passed through a csv file
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

    // Retrieve all courses in which the student is enrolled
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<CourseDTO> getCourses(String studentId) {
        // check if the student exist
        if(!this.studentRepository.existsById(studentId)){
            return null;
        }
        return this.studentRepository
                    .findById(studentId)
                    .get()
                    .getCourses()
                    .stream()
                    .map(x -> modelMapper.map(x, CourseDTO.class))
                    .collect(Collectors.toList());
    }

    // Retrieve all the teams the student belongs to
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<TeamDTO> getTeamsForStudent(String studentId) {
        if(!studentRepository.existsById(studentId)){
            throw new StudentNotFoundExeption();
        }
        return studentRepository.findById(studentId)
                                .get()
                                .getTeams()
                                .stream()
                                .map(x -> modelMapper.map(x, TeamDTO.class))
                                .collect(Collectors.toList());
    }

    // Retrieve all the members of an existing team
    @Override
    public List<StudentDTO> getMembers(Long TeamId) {
        // check if the team exist
        if(!teamRepository.existsById(TeamId)){
            throw new TeamNotFoundException();
        }
        return teamRepository.findById(TeamId)
                            .get()
                            .getMembers()
                            .stream()
                            .map(x -> modelMapper.map(x, StudentDTO.class))
                            .collect(Collectors.toList());
    }

    // propose a team for an existing course
    @Override
    public TeamDTO proposeTeam(String courseId, String name, List<String> memberIds) {
        Set<String> setStudents = new HashSet<String>(memberIds);
        Team team = new Team();

        // check if the course is present
        if(!courseRepository.existsById(courseId)){
            throw new CourseNotFoundException();
        }

        // check if all students exist
        if( memberIds.stream()
                        .map(this::getStudent)
                        .filter(x -> !x.isPresent())
                        .count() > 0)
        {
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled
        if(!this.getCourse(courseId).get().isEnabled()){
            throw new TeamServiceException();
        }

        // check if all the students are enrolled to the course
        if(
                !courseRepository.findById(courseId)
                                    .get()
                                    .getStudents()
                                    .stream()
                                    .map(Student::getId)
                                    .collect(Collectors.toList())
                                    .containsAll(memberIds)
        ){
            throw new TeamServiceException();
        }

        // check if the students are not already part of a team of this course
        if(
                memberIds.stream()
                            .map(this::getTeamsForStudent)
                            .flatMap(x -> x.stream())
                            .map(x -> teamRepository.findById(x.getId()).get())
                            .map(x -> x.getCourse().getName())
                            .collect(Collectors.toList())
                            .contains(courseId)
        ){
            throw new TeamServiceException();
        }

        // check if the cardinality is respected
        if(
                (memberIds.size() < this.getCourse(courseId).get().getMin()) ||
                        (memberIds.size() > this.getCourse(courseId).get().getMax())
        ){
            throw new TeamServiceException();
        }

        // check for duplicates
        if(memberIds.size() != setStudents.size()){
            throw new TeamServiceException();
        }

        // create the team
        team.setName(name);
        team.setCourse(courseRepository.findById(courseId).get());
        team.setStatus(memberIds.size());
        team.setMembers(studentRepository.findAll()
                                            .stream()
                                            .filter(x -> memberIds.contains(x.getId()))
                                            .collect(Collectors.toList())
                        );

        teamRepository.save(team);
        // team = teamRepository.findById(team.getId()).get();
        return modelMapper.map(team, TeamDTO.class);
    }

    // Retrieve the existing team for an existing course
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

    // Retrieve all the students that have a team in an existing course
    @Override
    public List<StudentDTO> getStudentsInTeams(String courseName) {
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        return courseRepository.getStudentsInTeams(courseName)
                                .stream()
                                .map(x -> modelMapper.map(x, StudentDTO.class))
                                .collect(Collectors.toList());
    }

    // Retrieve existing students that are not in a team of an existing course
    @Override
    public List<StudentDTO> getAvailableStudents(String courseName) {
        // check if the course exist
        if(!courseRepository.existsById(courseName)){
            throw new CourseNotFoundException();
        }
        return courseRepository.getStudentsNotInTeams(courseName)
                                .stream()
                                .map(x -> modelMapper.map(x, StudentDTO.class))
                                .collect(Collectors.toList());
    }

    // enable an existing team
    @Override
    public void enableTeam(Long teamId) {
        // check if the team exist
        if(!this.teamRepository.existsById(teamId)){
            throw new TeamNotFoundException();
        }

        teamRepository.findById(teamId).get().setStatus(1);
    }

    // Delete an existing team
    @Override
    public void evictTeam(Long teamId) {
        Team t;
        // check if the team exist
        if(!this.teamRepository.existsById(teamId)){
            throw new TeamNotFoundException();
        }
        t = this.teamRepository.findById(teamId).get();
        // remove team from each student
        this.studentRepository.findAll().stream()
                                        .filter(x -> x.getTeams().contains(t))
                                        .forEach(x -> x.getTeams().remove(t));

        // remove team from the repository
        this.teamRepository.deleteById(teamId);
    }
}
