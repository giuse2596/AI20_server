package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class HomeworkDTO extends RepresentationModel<HomeworkDTO> {

    private Long id;

    private Integer mark;

    private boolean editable;
}
