package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.entities.Assignment;
import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.Teacher;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.AssignmentRepository;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.TeacherRepository;
import it.polito.ai.project.server.repositories.UserRepository;
import it.polito.ai.project.server.services.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private TeacherService teacherService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private ModelHelper modelHelper;

    /**
     * URL to retrieve all the courses
     * @return all the courses
     */
    @GetMapping({"", "/"})
    public List<CourseDTO> all(){
        return teamService.getAllCourses()
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    @GetMapping("/teacher_courses")
    public List<CourseDTO> getTeacherCourses(@AuthenticationPrincipal UserDetails userDetails){
        Optional<Teacher> teacherOptional = teacherRepository.findById(userDetails.getUsername());

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
        Optional<CourseDTO> course = generalService.getCourse(name);

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
    public List<StudentDTO> enrolledStudents(@PathVariable String name){
        List<StudentDTO> students;

        try{
            students = teacherService.getEnrolledStudents(name)
                    .stream()
                    .map(x -> modelHelper.enrich(x))
                    .collect(Collectors.toList());
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
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

        if (
                !teacherService.addCourse(
                        courseModelDTO.getCourseDTO(),
                        userDetails.getUsername(),
                        courseModelDTO.getVmModelDTO()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    courseModelDTO.getCourseDTO().getName()
            );
        }

        return modelHelper.enrich(generalService.getCourse(courseModelDTO.getCourseDTO().getName()).get());
    }

    /**
     * URL to enroll a student to a course
     * @param studentDTO the student object to enroll
     * @param name the name of the course
     */
    @PostMapping("/{name}/enrollOne")
    @ResponseStatus(HttpStatus.CREATED)
    public void enrollStudent(@Valid @RequestBody StudentDTO studentDTO, @PathVariable String name){
        try{
             if(!teacherService.addStudentToCourse(studentDTO.getId(), name)){
                 throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, studentDTO.getId()+""+name);
             }
        }
        catch (CourseNotFoundException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
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
                                        @PathVariable String name){
        Reader reader;
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("text/csv");
        supportedMediaTypes.add("text/plain");

        try {
            reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            mediaType = tika.detect(file.getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return teacherService.enrollCSV(reader, name);
    }

    /**
     * URL to enable a course
     * @param name the name of the course
     * @return the course enriched with the  URLs to the course services
     */
    @PostMapping({"/{name}/enable"})
    public CourseDTO enableCourse(@PathVariable String name){

        try {
            teacherService.enableCourse(name);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }

        return modelHelper.enrich(generalService.getCourse(name).get());
    }

    /**
     * URL to disable a course
     * @param name the name of the course
     * @return the course enriched with the  URLs to the course services
     */
    @PostMapping({"/{name}/disable"})
    public CourseDTO disableCourse(@PathVariable String name){

        try {
            teacherService.disableCourse(name);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }

        return modelHelper.enrich(generalService.getCourse(name).get());
    }

    /**
     * URL to retrieve all the teams for a given course
     * @param name the name of the course
     * @return the list of the teams of the course
     */
    @GetMapping("/{name}/teams")
    public List<TeamDTO> getCourseTeams(@PathVariable String name){
        Optional<CourseDTO> course = generalService.getCourse(name);

        if(!course.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }

        try{
            return teacherService.getTeamForCourse(name);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    /**
     * Retrieve the team's students
     * @param teamid the team id
     * @return the list of students that are in the team
     */
    @GetMapping("/{name}/teams/{teamid}")
    public List<StudentDTO> getTeamMembers(@PathVariable Long teamid){
        try{
            return this.teamService.getMembers(teamid);
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retrieve the team's virtual machines
     * @param teamid the team id
     * @param userDetails the user who make the request
     * @return the list of team's virtual machines
     */
    @GetMapping("/{name}/teams/{teamid}/virtula_machines")
    public List<VirtualMachineDTO> getTeamVritualMachines(@PathVariable Long teamid,
                                                          @AuthenticationPrincipal UserDetails userDetails){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
        List<String> members;

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        try{
            members = this.teamService.getMembers(teamid)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList());
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !members.contains(userDetails.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return this.teamService.getTeamVirtualMachines(teamid);
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
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
    public String getVirtualMachineImage(@PathVariable Long teamid,
                                         @PathVariable Long vmid,
                                         @AuthenticationPrincipal UserDetails userDetails){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
        List<String> members;

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        try{
            members = this.teamService.getMembers(teamid)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList());
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !members.contains(userDetails.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return "virtual_machine.html";
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * URL to retrieve all students in a team
     * @param name the name of the team
     * @return the list of the students in the team
     */
    @GetMapping("/{name}/students_in_team")
    public List<StudentDTO> getStudentsInTeams(@PathVariable String name){
        try {
            return teamService.getStudentsInTeams(name).stream()
                    .map(x -> modelHelper.enrich(x)).collect(Collectors.toList());
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, name);
        }
    }

    /**
     * URL to retrieve all the students of a given course not yet in a team
     * @param name the name of the course
     * @return the list of the students of a given course not yet in a team
     */
    @GetMapping("/{name}/avaiable_students")
    public List<StudentDTO> getAvaiableStudents(@PathVariable String name){
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
    @GetMapping("/{name}/assignments/{assignmentid}")
    public AssignmentDTO getAssignment(@PathVariable Long assignmentid){
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentid);

        if(!assignmentOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try{
            return generalService.getAssignment(assignmentid);
        }
        catch (GeneralServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Retrieve all the course's assignments
     * @param name the name of the course
     * @return the list of all the assignments
     */
    @GetMapping("/{name}/assignments")
    public List<AssignmentDTO> getCourseAssignments(@PathVariable String name){
        Optional<Course> courseOptional = this.courseRepository.findById(name);

        if(!courseOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try{
            return generalService.getCourseAssignments(name);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
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
    public List<DeliveryDTO> getAssignmentDeliveries(@PathVariable Long assignmentid,
                                                     @PathVariable String studentid,
                                                     @AuthenticationPrincipal UserDetails userDetails
    ){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            return this.generalService.getAssignmentStudentDeliveries(assignmentid, studentid);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
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
    public DeliveryDTO getAssignmentDelivery(@PathVariable Long assignmentid,
                                             @PathVariable String studentid,
                                             @AuthenticationPrincipal UserDetails userDetails
    ){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            return this.generalService.getAssignmentLastDelivery(assignmentid, studentid);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Delete a course
     * @param name the name of the course
     * @param userDetails the user who make the request
     */
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCourse(@PathVariable String name,@AuthenticationPrincipal UserDetails userDetails){
        Optional<Teacher> teacherOptional = this.teacherRepository.findById(userDetails.getUsername());

        if(!teacherOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        // check if the teacher is in the course
        if(!teacherOptional.get().getCourses()
                .stream()
                .map(x -> x.getName())
                .collect(Collectors.toList())
                .contains(name)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if(!this.teacherService.removeCourse(name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    /**
     * Modify the course
     * @param name the name of the course
     * @param min the min of the team member
     * @param max the max of the team member
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/modify")
    public void modifyCourse(@PathVariable String name,
                             @RequestParam int min,
                             @RequestParam int max,
                             @AuthenticationPrincipal UserDetails userDetails){

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.modifyCourse(name, min, max);
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
     * @param teacherid the teacher id
     */
    @PostMapping("/{name}/add_teacher/{teacherid}")
    @ResponseStatus(HttpStatus.OK)
    public void addTeacherToCourse(@PathVariable String name, @PathVariable String teacherid){
        if(!this.teacherService.addTeacherToCourse(teacherid, name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
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
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.removeStudentToCourse(studentid, name, false);
        }
        catch (StudentNotFoundExeption | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Retrieve the homework of a student
     * @param assignmentsid the assignment id
     * @param studentid the student id
     * @param userDetails the user who make the request
     * @return the homework DTO of the student
     */
    @GetMapping("/{name}/assignments/{assignmentsid}/homeworks/{studentid}")
    public HomeworkDTO getStudentHomework(@PathVariable Long assignmentsid,
                                          @PathVariable String studentid,
                                          @AuthenticationPrincipal UserDetails userDetails){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return this.generalService.getStudentHomework(assignmentsid, studentid);
        }
        catch (StudentNotFoundExeption | StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Change values of a team
     * @param name the name of the course
     * @param team the team id
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/teams/{teamid}")
    public void changeVMValues(@PathVariable String name,
                               @RequestBody TeamDTO team,
                               @AuthenticationPrincipal UserDetails userDetails){
        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
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
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
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
     * Set the mark of an homework
     * @param name the name of the course
     * @param homeworkid the homework id
     * @param homeworkDTO the homework with the mark set
     * @param userDetails the user who make the request
     */
    @PutMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    public void assignMarkToHomework(@PathVariable String name,
                                     @PathVariable Long homeworkid,
                                     @RequestBody HomeworkDTO homeworkDTO,
                                     @AuthenticationPrincipal UserDetails userDetails){

        if(homeworkDTO.getId() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // check if the homeworkid is the same of the homeworkDTO
        if(homeworkid != homeworkDTO.getId()){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.assignMarkToHomework(homeworkDTO);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    @PutMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}/editable")
    public void setEditableHomework(@PathVariable String name,
                                     @PathVariable Long homeworkid,
                                     @RequestBody HomeworkDTO homeworkDTO,
                                     @AuthenticationPrincipal UserDetails userDetails){

        if(homeworkDTO.getId() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // check if the homeworkid is the same of the homeworkDTO
        if(homeworkid != homeworkDTO.getId()){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.setEditableHomework(homeworkDTO);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    /**
     * Upload the teacher's revision for an homework
     * @param name the name of the course
     * @param homeworkid the homework id
     * @param multipartFile the image of the revision
     * @param userDetails the user who make the request
     */
    @PostMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    @ResponseStatus(HttpStatus.OK)
    public void revisionDelivery(@PathVariable String name,
                                 @PathVariable Long homeworkid,
                                 @RequestBody MultipartFile multipartFile,
                                 @AuthenticationPrincipal UserDetails userDetails){
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("image/png");

        // check media type of the file
        try {
            mediaType = tika.detect(multipartFile.getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            this.teacherService.revisionDelivery(homeworkid, multipartFile);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
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
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            return this.teacherService.getAssignmentImage(assignmentid);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/{name}/virtual_machine_model")
    public VMModelDTO getVMModel(@PathVariable String name,
                                 @AuthenticationPrincipal UserDetails userDetails){
        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
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
    public byte[] getDeliveryImage(@PathVariable String studentid,
                                   @PathVariable Long deliveryid,
                                   @AuthenticationPrincipal UserDetails userDetails){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        if(!userDetails.getAuthorities().stream().map(x -> x.getAuthority())
                .collect(Collectors.toList())
                .contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return generalService.getDeliveryImage(deliveryid);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

}
