package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class HomeworkDTO extends RepresentationModel<HomeworkDTO> {

    @NotNull
    private Long id;

    @Min(0)
    private Integer mark;

    @NotNull
    private boolean editable;
}
