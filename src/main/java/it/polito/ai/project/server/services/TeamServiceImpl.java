package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeamRepository;
import it.polito.ai.project.server.repositories.VirtualMachinesRepository;
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
    VirtualMachinesRepository virtualMachinesRepository;

    @Autowired
    ModelMapper modelMapper;

    StudentServiceImpl studentService;

    TeacherServiceImp teacherService;

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
            return null;
        }
        return studentOptional.get()
                    .getCourses()
                    .stream()
                    .map(x -> modelMapper.map(x, CourseDTO.class))
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
            optionalStudentList.stream().map(x -> x.get().getId())
            .anyMatch(x -> studentsInTeam.contains(x))
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
        if(teacherService.getTeamForCourse(courseId).stream()
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

        teamRepository.save(team);
        team = teamRepository.findById(team.getId()).get();
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

    //

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

        // remove team from each student
        teamOptional.get().getMembers().forEach(x -> teamOptional.get().removeMember(x));

        // remove team from the repository
        this.teamRepository.deleteById(teamId);
    }

    /**
     * Retrieve all the team virtual machine
     * @param teamId the team id
     * @return all the team virtual machine
     */
    public List<VirtualMachineDTO> getTeamVirtualMachines(Long teamId){
        Optional<Team> teamOptional = this.teamRepository.findById(teamId);

        // check if the team exists
        if (!teamOptional.isPresent()) {
            throw new TeamNotFoundException();
        }

        // check if the course is enabled
        if(!teamOptional.get().getCourse().isEnabled()){
            throw new StudentServiceException("Course not enabled");
        }

        return teamOptional.get()
                .getVirtualMachines()
                .stream()
                .map(x -> modelMapper.map(x, VirtualMachineDTO.class))
                .collect(Collectors.toList());
    }

}
