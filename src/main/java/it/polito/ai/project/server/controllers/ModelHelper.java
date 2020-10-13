package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

public class ModelHelper {

    public static CourseDTO enrich(CourseDTO courseDTO){

        courseDTO.add(WebMvcLinkBuilder.linkTo(CourseController.class)
                .slash(courseDTO.getName())
                .withSelfRel());

        courseDTO.add(WebMvcLinkBuilder.linkTo(CourseController.class)
                .slash(courseDTO.getName())
                .slash("enrolled")
                .withRel("enrolled"));

        return courseDTO;
    }

    public static StudentDTO enrich(StudentDTO studentDTO){

        studentDTO.add(WebMvcLinkBuilder.linkTo(StudentController.class)
                .slash(studentDTO.getId())
                .withSelfRel());

        return studentDTO;
    }

}
