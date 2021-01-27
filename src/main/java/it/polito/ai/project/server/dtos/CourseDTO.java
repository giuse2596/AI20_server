package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CourseDTO extends RepresentationModel<CourseDTO> {

    @NotBlank
    private String name;

    @NotBlank
    private String acronym;

    @NotNull
    @Min(2)
    private Integer min;

    @NotNull
    private Integer max;

    @NotNull
    private boolean enabled;

}
