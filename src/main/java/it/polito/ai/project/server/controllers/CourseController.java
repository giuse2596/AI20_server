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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Date;
import java.sql.Timestamp;
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
//        CourseDTO courseDTO = new CourseDTO();
//        VMModelDTO vmModelDTO = new VMModelDTO();
//
//        courseDTO.setName(courseModelDTO.getName());
//        courseDTO.setAcronym(courseModelDTO.getAcronym());
//        courseDTO.setMin(courseModelDTO.getMin());
//        courseDTO.setMax(courseModelDTO.getMax());
//
//        vmModelDTO.setCpuMax(courseModelDTO.getCpuMax());
//        vmModelDTO.setRamMax(courseModelDTO.getRamMax());
//        vmModelDTO.setDiskSpaceMax(courseModelDTO.getDiskSpaceMax());
//        vmModelDTO.setActiveInstances(courseModelDTO.getActiveInstances());
//        vmModelDTO.setTotalInstances(courseModelDTO.getTotalInstances());

        try {
            if (!teacherService.addCourse(courseModelDTO.getCourseDTO(), userDetails.getUsername(), courseModelDTO.getVmModelDTO())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, courseModelDTO.getCourseDTO().getName());
            }
        }
        catch (TransactionSystemException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
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
        catch (CourseNotFoundException | StudentNotFoundExeption | TransactionSystemException e){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
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
    public List<Boolean> enrollStudents(@RequestParam("file") MultipartFile file, @PathVariable String name){
        Reader reader;

        if(!file.getContentType().equals("text/csv")){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try {
             reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
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

    @GetMapping("/{name}/teams/{teamid}")
    public List<StudentDTO> getTeamMembers(@PathVariable Long teamid){
        try{
            return this.teamService.getMembers(teamid);
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

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

        if(!userDetails.getAuthorities().contains("ROLE_TEACHER") &
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

    @GetMapping("/{name}/teams/{teamid}/virtual_machines/{vmid}/image")
    public byte[] getVirtualMachineImage(@PathVariable Long teamid,
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

        if(!userDetails.getAuthorities().contains("ROLE_TEACHER") &
                !members.contains(userDetails.getUsername())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return this.generalService.getVirtualMachineImage(vmid);
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

    @GetMapping("/{name}/assignments/{assignmentid}/{studentid}")
    public List<DeliveryDTO> getAssignmentDeliveries(@PathVariable Long assignmentid,
                                                     @PathVariable String studentid,
                                                     @AuthenticationPrincipal UserDetails userDetails
    ){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!userDetails.getAuthorities().contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        try {
            return this.generalService.getAssignmentStudentDeliveries(assignmentid, studentid);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{name}/assignments/{assignmentid}/{studentid}/last")
    public DeliveryDTO getAssignmentDelivery(@PathVariable Long assignmentid,
                                                     @PathVariable String studentid,
                                                     @AuthenticationPrincipal UserDetails userDetails
    ){
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        if(!userDetails.getAuthorities().contains("ROLE_TEACHER") &
                !userDetails.getUsername().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if(!user.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        try {
            return this.generalService.getAssignmentLastDelivery(assignmentid, studentid);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

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

    @PutMapping("/{name}/modify")
    public void modifyCourse(@PathVariable String name, @RequestBody CourseDTO courseDTO,
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
            this.teacherService.modifyCourse(name, courseDTO);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{name}/add_teacher/{teacherid}")
    @ResponseStatus(HttpStatus.OK)
    public void addTeacherToCourse(@PathVariable String name, @PathVariable String teacherid){
        if(!this.teacherService.addTeacherToCourse(teacherid, name)){
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{name}/remove/{studentid}")
    public void removeStudentFromCourse(@PathVariable String name, @PathVariable String studentid,
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
            this.teacherService.removeStudentToCourse(studentid, name);
        }
        catch (StudentNotFoundExeption | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    @GetMapping("/{name}/assignments/{assignmentsid}/homeworks/{studentid}")
    public HomeworkDTO getStudentHomework(@PathVariable Long assignmentsid,
                                          @PathVariable String studentid,
                                          @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, studentid);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(studentid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return this.generalService.getStudentHomework(assignmentsid, studentid);
        }
        catch (StudentNotFoundExeption | StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/{name}/teams/{teamid}")
    public void changeVMValues(@PathVariable String name, @RequestBody TeamDTO team,
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

    @PostMapping("/{name}/assignments")
    public AssignmentDTO createAssignment(@PathVariable String name,
                                          //@RequestBody @Valid AssignmentFileDTO assignmentFileDTO,
                                          @RequestBody AssignmentDTO assignmentDTO,
                                          @RequestParam("file") MultipartFile multipartFile,
                                          @AuthenticationPrincipal UserDetails userDetails){
        //AssignmentDTO assignmentDTO = new AssignmentDTO();

        //assignmentDTO.setName("Prova1");
        //assignmentDTO.setExpiryDate(new Date(new Timestamp(System.currentTimeMillis()).getTime()));

        try{
            if(!teacherService.teacherInCourse(userDetails.getUsername(), name)){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        try{
            return this.teacherService.createAssignment(assignmentDTO, name, multipartFile);
        }
        catch (CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (TeacherServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    public void assignMarkToHomework(@PathVariable String name,
                                     @RequestBody HomeworkDTO homeworkDTO,
                                     @AuthenticationPrincipal UserDetails userDetails){

        if(homeworkDTO.getMark() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
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

    @PostMapping("/{name}/assignments/{assignmentid}/homeworks/{homeworkid}")
    @ResponseStatus(HttpStatus.OK)
    public void revisionDelivery(@PathVariable String name,
                                 @PathVariable Long homeworkid,
                                 @RequestBody MultipartFile multipartFile,
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
            this.teacherService.revisionDelivery(homeworkid, multipartFile);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{name}/assignments/{assignmentid}/image")
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

}
