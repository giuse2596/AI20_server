package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class CourseDTO extends RepresentationModel<CourseDTO> {

    private String name;

    private String acronym;

    private Integer min;

    private Integer max;

    private boolean enabled;

}
