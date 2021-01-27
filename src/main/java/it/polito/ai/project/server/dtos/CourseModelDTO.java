package it.polito.ai.project.server.dtos;

import lombok.Data;

import javax.validation.Valid;

@Data
public class CourseModelDTO {

    @Valid
    private CourseDTO courseDTO;

    @Valid
    private VMModelDTO vmModelDTO;

}
