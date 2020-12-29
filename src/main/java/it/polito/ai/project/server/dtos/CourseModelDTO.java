package it.polito.ai.project.server.dtos;

import lombok.Data;

@Data
public class CourseModelDTO {

    private CourseDTO courseDTO;
    private VMModelDTO vmModelDTO;

}
