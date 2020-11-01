package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, name);
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

        return teacherService.getEnrolledStudents(name)
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    /**
     * URL to create a new course
     * @param dto the course object to create
     * @return the course enriched with the URLs to the course services
     */
    @PostMapping({"","/"})
    public CourseDTO addCourse(@Valid @RequestBody CourseDTO dto){

        try {
            if (!teacherService.addCourse(dto)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, dto.getName());
            }
        }
        catch (TransactionSystemException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return modelHelper.enrich(generalService.getCourse(dto.getName()).get());
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
     * @return list of boolean values correspondig to every student
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
        return teacherService.addAndEroll(reader, name);
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
        return teacherService.getTeamForCourse(name);
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

}
