package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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
    TokenRepository tokenRepository;

    @Autowired
    VirtualMachinesRepository virtualMachinesRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    TeacherServiceImp teacherService;

    /**
     * Retrive active team given a course
     * @param courseName course name
     * @return list of active course team
     */
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Override
    public List<TeamDTO> getEnabledTeamsForCourse(String courseName) {
        Optional<Course> courseOptional = courseRepository.findById(courseName);

        // check if the course exist
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }
        return courseOptional
                .get()
                .getTeams()
                .stream()
                .filter(Team::isActive)
                .map(x -> modelMapper.map(x, TeamDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the existing team for an existing course
     * @param courseName the course name
     * @return list of all the teams in the course
     */
    @Override
    public List<TeamDTO> getTeamsForCourse(String courseName) {
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
     * Retrieve all the teams the student belongs to
     * @param studentId the student id
     * @return list of the teams the student belongs to
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<TeamDTO> getTeamsForStudent(String studentId) {
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }
        return studentOptional.get()
                            .getTeams()
                            .stream()
                            .map(x -> modelMapper.map(x, TeamDTO.class))
                            .collect(Collectors.toList());
    }

    /**
     * Retrieve the team of a student given the course name
     * @param studentId the student id
     * @param courseName the course id
     * @return the team of the student
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public TeamDTO getTeamForStudent(String studentId, String courseName) {
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        Optional<Team> team;

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new StudentNotFoundExeption();
        }

        // check if the course exists
        if (!courseOptional.isPresent()) {
            throw new CourseNotFoundException();
        }

        // check if the student is enrolled to the course
        if (!courseOptional.get()
                .getStudents()
                .stream()
                .map(x->x.getId())
                .collect(Collectors.toList())
                .contains(studentId)
        ) {
            throw new TeamServiceException();
        }

        // take the active team of the specified course
        team = studentOptional.get()
                .getTeams()
                .stream()
                .filter(x ->
                        x.isActive() &
                        x.getCourse().getName().equals(courseName))
                .findFirst();

        if (!team.isPresent()) {
            throw new TeamNotFoundException();
        }

        return modelMapper.map(team, TeamDTO.class);

    }

    /**
     * Retrieve all student teams not active for a course
     * @param courseName the name of the course
     * @param studentId the student id
     * @return the teams not active for a course
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<TeamDTO> getStudentTeamsNotEnabled(String courseName, String studentId) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        return studentOptional.get()
                .getTeams()
                .stream()
                // select team not enabled belonging to the given course
                .filter(x ->
                    x.getCourse().getName().equals(courseName) &
                    !x.isActive()
                )
                .map(x -> this.modelMapper.map(x, TeamDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all student teams active for a course
     * @param courseName the name of the course
     * @param studentId the student id
     * @return the teams not active for a course
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<TeamDTO> getStudentTeamsEnabled(String courseName, String studentId) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);

        // check if the course exists
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        // check if the student exists
        if (!studentOptional.isPresent()) {
            throw new StudentNotFoundExeption();
        }

        return studentOptional.get()
                .getTeams()
                .stream()
                // select team enabled belonging to the given course
                .filter(x ->
                        x.getCourse().getName().equals(courseName) & x.isActive()
                )
                .map(x -> this.modelMapper.map(x, TeamDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all the members of an existing team
     * @param teamId the team id
     * @return the list of students that belong to the specified team
     */
    @Override
    public List<StudentDTO> getMembers(Long teamId) {
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);

        // check if the team exist
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }
        return teamOptional.get()
                        .getMembers()
                        .stream()
                        .map(x -> modelMapper.map(x, StudentDTO.class))
                        .collect(Collectors.toList());
    }

    /**
     * Propose a team for an existing course
     * @param courseId course id to which the proposed team belongs
     * @param name team name
     * @param proposer id of the student who proposed the team, is
     *                 included in the memberIds list
     * @param memberIds list of students proposed for the team,
     *                  include the proposer
     * @return the TeamDTO object
     */
    @Override
    public TeamDTO proposeTeam(String courseId, String name, String proposer, List<String> memberIds) {
        Set<String> setStudents = new HashSet<>(memberIds);
        Team team = new Team();
        VMModel vmModel;
        Optional<Course> courseOptional = this.courseRepository.findById(courseId);
        List<Optional<Student>> optionalStudentList = new ArrayList<>();
        List<Student> studentsInTeam;

        // check if the course is present
        if(!courseOptional.isPresent()){
            throw new CourseNotFoundException();
        }

        vmModel = courseOptional.get().getVmModel();
        studentsInTeam = this.courseRepository.getStudentsInTeams(courseId);

        // check if all students exist
        memberIds.forEach(x -> optionalStudentList.add(this.studentRepository.findById(x)));
        if (optionalStudentList.stream().anyMatch(x -> !x.isPresent()) ) {
            throw new StudentNotFoundExeption();
        }

        // check if the course is enabled
        if(!courseOptional.get().isEnabled()){
            throw new TeamServiceException();
        }

        // check if all the students are enrolled to the course
        if (
            optionalStudentList.stream()
                    .anyMatch(x -> !x.get().getCourses().contains(courseOptional.get()))
        ) {
            throw new TeamServiceException();
        }

        // check if the students are not already part of a team of this course
        if(
            optionalStudentList
                    .stream()
                    .map(x -> x.get().getId())
                    .anyMatch(
                            x -> studentsInTeam
                                    .stream()
                                    .map(y -> modelMapper.map(y, StudentDTO.class))
                                    .map(StudentDTO::getId)
                                    .collect(Collectors.toList())
                                    .contains(x)
                    )
        ){
            throw new TeamServiceException();
        }

        // check if the cardinality is respected
        if(
            (memberIds.size() < courseOptional.get().getMin()) ||
            (memberIds.size() > courseOptional.get().getMax())
        ){
            throw new TeamServiceException();
        }

        // check for duplicates
        if(memberIds.size() != setStudents.size()){
            throw new TeamServiceException();
        }

        // check if exist other teams with the same name in the course
        if(this.getTeamsForCourse(courseId).stream()
                            .anyMatch(x -> x.getName().equals(name))
        ){
            throw new TeamServiceException();
        }

        // create the team
        team.setName(name);
        team.setProposer(proposer);
        team.setActive(false);
        team.setCourse(courseOptional.get());
        team.setMembers(optionalStudentList.stream().map(x -> x.get()).collect(Collectors.toList()));

        // set resources limit for the team
        team.setCpuMax(vmModel.getCpuMax());
        team.setRamMax(vmModel.getRamMax());
        team.setDiskSpaceMax(vmModel.getDiskSpaceMax());
        team.setTotVM(vmModel.getTotalInstances());

        // setted as the max of total active virtual machines specified in the model
        team.setActiveVM(vmModel.getActiveInstances());

        team = teamRepository.save(team);
        return modelMapper.map(team, TeamDTO.class);
    }

    /**
     * Retrieve all the students who have a team in an existing course
     * @param courseName the course name
     * @return list of all the students who have a team in the specified course
     */
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

    /**
     * Retrieve existing students who are not part of a course team
     * @param courseName coruse name
     * @return list of students who are not part of a course team
     */
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

    /**
     * Enable an existing team
     * @param teamId team id to enable
     */
    @Override
    public void enableTeam(Long teamId) {
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);

        // check if the team exist
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        teamOptional.get().setActive(true);
    }

    /**
     * Delete an existing team
     * @param teamId team id to delete
     */
    @Override
    public void evictTeam(Long teamId) {
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);
        List<VirtualMachine> tempvirtualmachines;
        List<Student> studentsInTeam = new ArrayList<>();

        // check if the team exist
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        // store all the virtual machines to remove
        tempvirtualmachines = this.virtualMachinesRepository.findAll()
                                .stream()
                                .filter(x -> x.getTeam().getId().equals(teamId))
                                .collect(Collectors.toList());

        // delete the relation between the virtual machine of the team and the students
        tempvirtualmachines.forEach(x -> x.getOwners().forEach(x::removeOwner));

        // delete the team from the virtual machine
        tempvirtualmachines.forEach(x -> x.setTeam(null));

        // remove the virtual machine from the repository
        tempvirtualmachines.forEach(x -> this.virtualMachinesRepository.deleteById(x.getId()));

        // remove team from each student, copying the members in a temporary variable
        // because the original list is modified
        teamOptional.get().getMembers().forEach(x -> studentsInTeam.add(x));
        studentsInTeam.forEach(x -> teamOptional.get().removeMember(x));

        // remove relation between team and course
        teamOptional.get().setCourse(null);

        // remove team from the repository
        this.teamRepository.deleteById(teamId);
    }

    /**
     * Retrieve the list of the students that confirmed to join in one team
     * @param teamId the team id
     * @return the list of the students that confirmed to join in one team
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<StudentDTO> getConfirmedMembersTeam(Long teamId) {
        Optional<Team> teamOptional = teamRepository.findById(teamId);
        List<String> membersIds;

        // check if the team exists
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        membersIds = this.tokenRepository.findAllByTeamId(teamId)
                    .stream()
                    .map(Token::getStudentId)
                    .collect(Collectors.toList());

        return teamOptional.get().getMembers()
                .stream()
                .filter(x -> !membersIds
                        .contains(x.getId())
                )
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all the pendant students' requests to join a team
     * @param teamId the team id
     * @return the list of pendant members
     */
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Override
    public List<StudentDTO> getPendentMembersTeam(Long teamId) {
        Optional<Team> teamOptional = teamRepository.findById(teamId);
        List<String> membersIds;

        // check if the team exists
        if(!teamOptional.isPresent()){
            throw new TeamNotFoundException();
        }

        membersIds = this.tokenRepository.findAllByTeamId(teamId)
                .stream()
                .map(Token::getStudentId)
                .collect(Collectors.toList());

        return teamOptional.get().getMembers()
                .stream()
                .filter(x -> membersIds
                        .contains(x.getId())
                )
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

}
