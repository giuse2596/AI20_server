package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/API/students")
public class StudentController {

    @Autowired
    TeamService teamService;

    ModelHelper modelHelper;

    @GetMapping({"", "/"})
    public List<StudentDTO> all(){
        return teamService.getAllStudents()
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public StudentDTO getOne(@PathVariable String id){
        Optional<StudentDTO> student = teamService.getStudent(id);

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, id);
        }
        return modelHelper.enrich(student.get());
    }

    @PostMapping({"","/"})
    public StudentDTO addStudent(@Valid @RequestBody StudentDTO studentDTO){

        try {

            if (!teamService.addStudent(studentDTO)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, studentDTO.getId());
            }
        }
        catch (TransactionSystemException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return modelHelper.enrich(teamService.getStudent(studentDTO.getId()).get());
    }

    @GetMapping("/{id}/courses")
    public List<CourseDTO> getCourses(@PathVariable String id){
        Optional<StudentDTO> student = teamService.getStudent(id);

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        return teamService
                .getCourses(id)
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/teams")
    public List<TeamDTO> getTeams(@PathVariable String id){
        Optional<StudentDTO> student = teamService.getStudent(id);

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        return teamService
                .getTeamsForStudent(id);
    }

}
