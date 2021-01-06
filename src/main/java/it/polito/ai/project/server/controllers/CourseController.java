package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.Assignment;
import it.polito.ai.project.server.entities.Teacher;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.AssignmentRepository;
import it.polito.ai.project.server.repositories.TeacherRepository;
import it.polito.ai.project.server.repositories.UserRepository;
import it.polito.ai.project.server.services.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/API/courses")
public class CourseController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private GeneralService generalService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private UserService userService;

    private ModelHelper modelHelper;

    /**
     * URL to retrieve all the courses
     * @return all the courses
     */
    @GetMapping({"", "/"})
    public List<CourseDTO> all(){
        return generalService.getAllCourses()
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all teacher courses
     * @param userDetails the user who make the request
     * @return the list of courses of a teacher
     */
    @GetMapping("/teacher_courses")
    public List<CourseDTO> getTeacherCourses(@AuthenticationPrincipal UserDetails userDetails){
        Optional<UserDTO> teacherOptional = this.userService.getUser(userDetails.getUsername());

        // check if the teacher exists
        if(!teacherOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        try{
            return this.teacherService.getTeacherCourses(userDetails.getUsername());
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * URL to retrieve a single course
     * @param name the name of the course
     * @return the specified course
     */
    @GetMapping("/{name}")
    public CourseDTO getOne(@PathVariable String name){
        Optional<CourseDTO> course = this.generalService.getCourse(name);

        if(!course.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }
        return modelHelper.enrich(course.get());
    }

    /**
     * URL to retrieve all the students enrolled in a course
     * @param name the name of the course
     * @return all the students enrolled in the specified course
     */
    @GetMapping("/{name}/enrolled")
    public List<StudentDTO> enrolledStudents(@PathVariable String name,
                                             @AuthenticationPrincipal UserDetails userDetails){
        List<StudentDTO> students;
        List<String> roles = userDetails
                                .getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {

            // if is a student check that is enrolled
            if (roles.contains("STUDENT")) {
                if (
                        !this.studentService.getCourses(userDetails.getUsername())
                                .stream()
                                .map(CourseDTO::getName)
                                .collect(Collectors.toList())
                                .contains(name)
                ){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve all the students enrolled in a course
            students = this.generalService.getEnrolledStudents(name)
                    .stream()
                    .map(x -> modelHelper.enrich(x))
                    .collect(Collectors.toList());
        }
        catch (StudentNotFoundExeption | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return students;
    }

    /**
     * URL to create a new course
     * @param courseModelDTO wrapper of courseDTO and vmModelDTO
     * @return the course enriched with the URLs to the course services
     */
    @PostMapping({"","/"})
    public CourseDTO addCourse(@Valid @RequestBody CourseModelDTO courseModelDTO,
                               @AuthenticationPrincipal UserDetails userDetails){

        // add the course
        if (
                !teacherService.addCourse(
                        courseModelDTO.getCourseDTO(),
                        userDetails.getUsername(),
                        courseModelDTO.getVmModelDTO()
                )
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, courseModelDTO.getCourseDTO().getName());
        }

        return modelHelper.enrich(this.generalService.getCourse(courseModelDTO.getCourseDTO().getName()).get());
    }

    /**
     * URL to enroll a student to a course
     * @param studentDTO the student object to enroll
     * @param name the name of the course
     */
    @PostMapping("/{name}/enrollOne")
    @ResponseStatus(HttpStatus.CREATED)
    public void enrollStudent(@Valid @RequestBody StudentDTO studentDTO,
                              @PathVariable String name,
                              @AuthenticationPrincipal UserDetails userDetails){

        try{

            // check if the user is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

             if(!teacherService.addStudentToCourse(studentDTO.getId(), name)){
                 throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, studentDTO.getId()+" "+name);
             }
        }
        catch (TeacherServiceException | CourseNotFoundException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * URL to enroll multiple students to a course
     * @param file the file where there are the students
     * @param name the name of the course
     * @return list of boolean values corresponding to every student
     */
    @PostMapping("/{name}/enrollMany")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Boolean> enrollStudents(@RequestParam("file") MultipartFile file,
                                        @PathVariable String name,
                                        @AuthenticationPrincipal UserDetails userDetails){
        Reader reader;
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("text/csv");
        supportedMediaTypes.add("text/plain");

        try{

            // check if the user is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            mediaType = tika.detect(file.getInputStream());

            // check if the type is supported
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            return teacherService.enrollCSV(reader, name);
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        catch (TeacherServiceException | StudentNotFoundExeption | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * URL to retrieve all the active teams for a given course
     * @param name the name of the course
     * @return the list of the teams of the course
     */
    @GetMapping("/{name}/enabled_teams")
    public List<TeamDTO> getCourseTeams(@PathVariable String name,
                                        @AuthenticationPrincipal UserDetails userDetails){

        try{

            // check if the user is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.teamService.getEnabledTeamsForCourse(name);
        }
        catch (CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    /**
     * Retrieve all student not enabled teams
     * @param name the name of the course
     * @return the list of teams not enabled for a course
     */
    @GetMapping("/{name}/student_not_enabled_teams")
    public List<TeamDTO> getStudentNotEnabledCourseTeams(@PathVariable String name,
                                                  @AuthenticationPrincipal UserDetails userDetails){
        try{
            return teamService.getStudentTeamsNotEnabled(name, userDetails.getUsername());
        }
        catch (CourseNotFoundException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve all student enabled teams
     * @param name the name of the course
     * @return the list of teams not enabled for a course
     */
    @GetMapping("/{name}/student_enabled_teams")
    public List<TeamDTO> getStudentEnabledCourseTeams(@PathVariable String name,
                                                  @AuthenticationPrincipal UserDetails userDetails){
        try{
            return teamService.getStudentTeamsEnabled(name, userDetails.getUsername());
        }
        catch (CourseNotFoundException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the team's students
     * @param name course name
     * @param teamid the team id
     * @return the list of students that are in the team
     */
    @GetMapping("/{name}/teams/{teamid}")
    public List<StudentDTO> getTeamMembers(@PathVariable String name,
                                           @PathVariable Long teamid,
                                           @AuthenticationPrincipal UserDetails userDetails){
        List<StudentDTO> members;
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {
            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve team members
            members = this.teamService.getMembers(teamid);

            // check if the student belongs to it
            if (roles.contains("STUDENT")) {
                if(!members
                        .stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList())
                        .contains(userDetails.getUsername())
                    )
                {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the team belongs to the course
            if (!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(teamid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return members;
        }
        catch (CourseNotFoundException | TeamNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the team's virtual machines
     * @param teamid the team id
     * @param userDetails the user who make the request
     * @return the list of team's virtual machines
     */
    @GetMapping("/{name}/teams/{teamid}/virtual_machines")
    public List<VirtualMachineDTO> getTeamVirtualMachines(@PathVariable String name,
                                                          @PathVariable Long teamid,
                                                          @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {
            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve team members and check if the student belongs to it
            if (roles.contains("STUDENT")) {
                if(!this.teamService.getMembers(teamid)
                        .stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList())
                        .contains(userDetails.getUsername())
                )
                {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the team belongs to the course
            if (!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(teamid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.generalService.getTeamVirtualMachines(teamid);
        }
        catch (TeamNotFoundException | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Retrieve the owners of a team's virtual machine
     * @param teamid the team id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     * @return the owners of a virtual machine
     */
    @GetMapping("/{name}/teams/{teamid}/virtual_machines/{vmid}/owners")
    public List<StudentDTO> getVirtualMachineOwners(@PathVariable String name,
                                                    @PathVariable Long teamid,
                                                    @PathVariable Long vmid,
                                                    @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {
            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve team members and check if the student belongs to it
            if (roles.contains("STUDENT")) {
                if(!this.teamService.getMembers(teamid)
                        .stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList())
                        .contains(userDetails.getUsername())
                )
                {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the team belongs to the course
            if (!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(teamid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the virtual machine belongs to the team
            if(
                    !this.generalService.getTeamVirtualMachines(teamid)
                            .stream()
                            .map(x -> x.getId())
                            .collect(Collectors.toList())
                            .contains(vmid)
            ){
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            return this.generalService.getVirtualMachineOwners(vmid);

        }
        catch (GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (TeamNotFoundException | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the image of a team's virtual machine
     * @param teamid the team id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     * @return the imagine of a virtual machine
     */
    @GetMapping(value="/{name}/teams/{teamid}/virtual_machines/{vmid}/image",
            produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getVirtualMachineImage(@PathVariable String name,
                                         @PathVariable Long teamid,
                                         @PathVariable Long vmid,
                                         @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {
            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve team members and check if the student belongs to it
            if (roles.contains("STUDENT")) {
                if(!this.teamService.getMembers(teamid)
                        .stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList())
                        .contains(userDetails.getUsername())
                )
                {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the team belongs to the course
            if (!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(teamid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the virtual machine belongs to the team
            if(
                    !this.generalService.getTeamVirtualMachines(teamid)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(vmid)
            ){
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            return this.generalService.getVirtualMachineImage(vmid);

        }
        catch (GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (TeamNotFoundException | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{name}/teams/{teamid}/available_resources")
    public HashMap<String, Integer> getTeamAvailableResources(@PathVariable String name,
                                                              @PathVariable Long teamid,
                                                              @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try {
            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // retrieve team members and check if the student belongs to it
            if (roles.contains("STUDENT")) {
                if(!this.teamService.getMembers(teamid)
                        .stream()
                        .map(x -> x.getId())
                        .collect(Collectors.toList())
                        .contains(userDetails.getUsername())
                )
                {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the team belongs to the course
            if (!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(teamid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.generalService.getVMAvailableResources(teamid);

        }
        catch (GeneralServiceException | StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (TeamNotFoundException | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * URL to retrieve all students in a team
     * @param name the name of the team
     * @return the list of the students in the team
     */
//    @GetMapping("/{name}/students_in_team")
//    public List<StudentDTO> getStudentsInTeams(@PathVariable String name,
//                                               @AuthenticationPrincipal UserDetails userDetails){
//        List<String> courses;
//
//        // retrieve STUDENT COURSES TO VERIFY IF IS ENROLLED TO THIS COUSE
//        try{
//            members = this.teamService.getMembers(teamid)
//                    .stream()
//                    .map(x -> x.getId())
//                    .collect(Collectors.toList());
//        }
//        catch (TeamNotFoundException e){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//
//        // check if the user is a teacher of the course or a team member
//        try{
//            if(!teacherService.teacherInCourse(userDetails.getUsername(), name) &
//                    !members.contains(userDetails.getUsername())){
//                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//            }
//        }
//        catch (TeacherServiceException e){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
//        }
//
//        try {
//            return teamService.getStudentsInTeams(name).stream()
//                    .map(x -> modelHelper.enrich(x)).collect(Collectors.toList());
//        }
//        catch (CourseNotFoundException e){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
//        }
//    }

    /**
     * URL to retrieve all the students of a given course not yet in a team
     * @param name the name of the course
     * @return the list of the students of a given course not yet in a team
     */
    @GetMapping("/{name}/available_students")
    public List<StudentDTO> getAvailableStudents(@PathVariable String name){
        try {
            return teamService.getAvailableStudents(name).stream()
                    .map(x -> modelHelper.enrich(x)).collect(Collectors.toList());
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }
    }

    /**
     * Retrieve the assignment of a course
     * @param assignmentid the assignment id
     * @return the DTO of an assignment
     */
//    @GetMapping("/{name}/assignments/{assignmentid}")
//    public AssignmentDTO getAssignment(@PathVariable String name,
//                                       @PathVariable Long assignmentid,
//                                       @AuthenticationPrincipal UserDetails userDetails){
//        List<String> roles = userDetails
//                .getAuthorities()
//                .stream()
//                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
//
//        try{
//
//            // if is a teacher check that is his course
//            if (roles.contains("TEACHER")) {
//                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
//                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//                }
//            }
//
//            // if is a student check that is enrolled
//            if (roles.contains("STUDENT")) {
//                if (
//                        !this.studentService.getCourses(userDetails.getUsername())
//                                .stream()
//                                .map(CourseDTO::getName)
//                                .collect(Collectors.toList())
//                                .contains(name)
//                ){
//                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
//                }
//            }
//
//            return generalService.getAssignment(assignmentid);
//        }
//        catch (GeneralServiceException | CourseNotFoundException |
//                TeacherServiceException | StudentNotFoundExeption e){
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
//        }
//    }

    /**
     * Retrieve all the course's assignments
     * @param name the name of the course
     * @return the list of all the assignments
     */
    @GetMapping("/{name}/assignments")
    public List<AssignmentDTO> getCourseAssignments(@PathVariable String name,
                                                    @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try{

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a student check that is enrolled
            if (roles.contains("STUDENT")) {
                if (
                        !this.studentService.getCourses(userDetails.getUsername())
                                .stream()
                                .map(CourseDTO::getName)
                                .collect(Collectors.toList())
                                .contains(name)
                ){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            return generalService.getCourseAssignments(name);
        }
        catch (CourseNotFoundException | TeacherServiceException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the deliveries of a student
     * @param assignmentid the assignment id
     * @param studentid the student id
     * @param userDetails the user who make the request
     * @return the list of all the deliveries for an assignment
     */
    @GetMapping("/{name}/assignments/{assignmentid}/{studentid}")
    public List<DeliveryDTO> getAssignmentDeliveries(@PathVariable String name,
                                                     @PathVariable Long assignmentid,
                                                     @PathVariable String studentid,
                                                     @AuthenticationPrincipal UserDetails userDetails
    ){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try{

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a student check that is the one specified in the url
            if (roles.contains("STUDENT")) {
                if (userDetails.getUsername().equals(studentid)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.generalService.getAssignmentStudentDeliveries(assignmentid, studentid);
        }
        catch (CourseNotFoundException | TeacherServiceException |
                StudentNotFoundExeption | GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the last delivery of an assignment
     * @param assignmentid the assignment id
     * @param studentid the student id
     * @param userDetails the user who make the request
     * @return the last delivery of the student for an assignment
     */
    @GetMapping("/{name}/assignments/{assignmentid}/{studentid}/last")
    public DeliveryDTO getAssignmentLastDelivery(@PathVariable String name,
                                             @PathVariable Long assignmentid,
                                             @PathVariable String studentid,
                                             @AuthenticationPrincipal UserDetails userDetails
    ){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try{

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a student check that is the one specified in the url
            if (roles.contains("STUDENT")) {
                if (userDetails.getUsername().equals(studentid)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.generalService.getAssignmentLastDelivery(assignmentid, studentid);
        }
        catch (CourseNotFoundException | TeacherServiceException |
                StudentNotFoundExeption | GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Delete a course
     * @param name the name of the course
     * @param userDetails the user who make the request
     */
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCourse(@PathVariable String name,
                             @AuthenticationPrincipal UserDetails userDetails){

        try{
            // check if is the teacher course
            if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if(!this.teacherService.removeCourse(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    /**
     * Modify the course
     * @param name the name of the course
     * @param courseDTO the course dto
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/modify")
    public void modifyCourse(@PathVariable String name,
                             @RequestParam @Valid CourseDTO courseDTO,
                             @AuthenticationPrincipal UserDetails userDetails){

        // check if is a teacher of the course
        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.modifyCourse(courseDTO);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Add a teacher to a course
     * @param name the name of the course
     */
    @PostMapping("/{name}/add_teacher")
    @ResponseStatus(HttpStatus.OK)
    public void addTeacherToCourse(@PathVariable String name,
                                   @AuthenticationPrincipal UserDetails userDetails){
        try {
            if (!this.teacherService.addTeacherToCourse(userDetails.getUsername(), name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }
        catch (CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Remove a student from a course
     * @param name the name of the course
     * @param studentid the student id
     * @param userDetails the user who make the request
     */
    @DeleteMapping("/{name}/remove/{studentid}")
    public void removeStudentFromCourse(@PathVariable String name,
                                        @PathVariable String studentid,
                                        @AuthenticationPrincipal UserDetails userDetails){

        try{
            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the student is enrolled to this course
            if (!this.teacherService.removeStudentToCourse(studentid, name, false)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (StudentNotFoundExeption | CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Retrieve the homework of a student
     * @param assignmentid the assignment id
     * @param studentid the student id
     * @param userDetails the user who make the request
     * @return the homework DTO of the student
     */
    @GetMapping("/{name}/assignments/{assignmentid}/homeworks/{studentid}")
    public HomeworkDTO getStudentHomework(@PathVariable String name,
                                          @PathVariable Long assignmentid,
                                          @PathVariable String studentid,
                                          @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try{

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a student check that is the one specified in the url
            if (roles.contains("STUDENT")) {
                if (userDetails.getUsername().equals(studentid)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.generalService.getStudentHomework(assignmentid, studentid);
        }
        catch (StudentNotFoundExeption | StudentServiceException |
                TeacherServiceException | GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Change values of a team
     * @param name the name of the course
     * @param teamid the team id
     * @param team the team dto
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/teams/{teamid}")
    public void changeVMValues(@PathVariable String name,
                               @PathVariable Long teamid,
                               @RequestBody TeamDTO team,
                               @AuthenticationPrincipal UserDetails userDetails){
        try{
            // check if is a teacher of the course
            if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }


            // check if the team belongs to the course
            if(!this.teamService.getTeamsForCourse(name)
                    .stream()
                    .map(TeamDTO::getId)
                    .collect(Collectors.toList())
                    .contains(teamid)
            ){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

        }
        catch (TeacherServiceException | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.changeVMvalues(team, name);
        }
        catch (CourseNotFoundException | TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Create an assignment
     * @param name the name of the course
     * @param assignmentDTO the assignment to create
     * @param userDetails the user who make the request
     * @return the created assignment DTO
     */
    @PostMapping("/{name}/assignments")
    public AssignmentDTO createAssignment(@PathVariable String name,
                                          @Valid @ModelAttribute AssignmentDTO assignmentDTO,
                                          @AuthenticationPrincipal UserDetails userDetails){
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("image/png");

        // check media type of the file
        try {
            mediaType = tika.detect(assignmentDTO.getMultipartFile().getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        catch (TeacherServiceException | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            return this.teacherService.createAssignment(assignmentDTO, name);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Modify an homework, set the mark or editable flag
     * @param name the name of the course
     * @param homeworkid the homework id
     * @param homeworkDTO the homework modified
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    public void modifyHomework(@PathVariable String name,
                               @PathVariable Long assignmentid,
                               @PathVariable Long homeworkid,
                               @RequestBody @Valid HomeworkDTO homeworkDTO,
                               @AuthenticationPrincipal UserDetails userDetails){

        // check if the homeworkid is the same of the homeworkDTO
        if(!homeworkid.equals(homeworkDTO.getId())){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        try{
            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the homework belongs to the assignment
            if (!this.teacherService.getHomework(assignmentid)
                    .stream()
                    .map(HomeworkDTO::getId)
                    .collect(Collectors.toList())
                    .contains(homeworkid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            if(homeworkDTO.getMark() == null){
                this.teacherService.setEditableHomework(homeworkDTO);
            }else{
                this.teacherService.assignMarkToHomework(homeworkDTO);
            }
        }
        catch (TeacherServiceException | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Upload the teacher's revision for an homework
     * @param name the name of the course
     * @param assignmentid assignment id
     * @param homeworkid the homework id
     * @param multipartFile the image of the revision
     * @param userDetails the user who make the request
     */
    @PostMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    @ResponseStatus(HttpStatus.OK)
    public void revisionDelivery(@PathVariable String name,
                                 @PathVariable Long assignmentid,
                                 @PathVariable Long homeworkid,
                                 @RequestBody MultipartFile multipartFile,
                                 @AuthenticationPrincipal UserDetails userDetails){
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("image/png");

        try {
            // check media type of the file
            mediaType = tika.detect(multipartFile.getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the homework belongs to the assignment
            if (!this.teacherService.getHomework(assignmentid)
                    .stream()
                    .map(HomeworkDTO::getId)
                    .collect(Collectors.toList())
                    .contains(homeworkid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            this.teacherService.revisionDelivery(homeworkid, multipartFile);
        }
        catch (StudentServiceException | TeacherServiceException | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * Retrieve the assignment image
     * @param name the name of the course
     * @param assignmentid the assignment id
     * @param userDetails the user who make the request
     * @return the image of the assignment
     */
    @GetMapping(value = "/{name}/assignments/{assignmentid}/image",
            produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getAssignmentImage(@PathVariable String name,
                                     @PathVariable Long assignmentid,
                                     @AuthenticationPrincipal UserDetails userDetails){
        try{
            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // check if the assignment belongs to the course
            if (!this.generalService
                    .getCourseAssignments(name)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(assignmentid)
            ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.teacherService.getAssignmentImage(assignmentid);
        }
        catch (TeacherServiceException | CourseNotFoundException  e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{name}/virtual_machine_model")
    public VMModelDTO getVMModel(@PathVariable String name,
                                 @AuthenticationPrincipal UserDetails userDetails){
        try{
            // check if is a teacher of the course
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            return this.teacherService.getVMModel(name);
        }
        catch (CourseNotFoundException | TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Retrieve the image of a delivery
     * @param studentid the student id
     * @param deliveryid the delivery id
     * @param userDetails the user who make the request
     * @return the image of the delivery
     */
    @GetMapping(value = "/{name}/{studentid}/deliveries/{deliveryid}",
            produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public byte[] getDeliveryImage(@PathVariable String name,
                                   @PathVariable String studentid,
                                   @PathVariable Long deliveryid,
                                   @AuthenticationPrincipal UserDetails userDetails){
        List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        try{

            // if is a teacher check that is his course
            if (roles.contains("TEACHER")) {
                if(!this.teacherService.teacherInCourse(userDetails.getUsername(), name)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            // if is a student
            if (roles.contains("STUDENT")) {
                // check if is the one specified in the url
                if (userDetails.getUsername().equals(studentid)){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }

                // check if is enrolled to the course
                if (
                    !this.studentService.getCourses(userDetails.getUsername())
                            .stream()
                            .map(CourseDTO::getName)
                            .collect(Collectors.toList())
                            .contains(name)
                ){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }

            return generalService.getDeliveryImage(deliveryid);
        }
        catch (TeacherServiceException | CourseNotFoundException |
                StudentNotFoundExeption | GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

}
