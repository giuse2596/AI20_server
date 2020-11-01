package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class VMModelDTO extends RepresentationModel<VMModelDTO> {

    private Long id;

    private Integer cpuMax;

    private Integer ramMax;

    private Integer diskSpaceMax;

    private Integer totalInstances;

    private Integer activeInstances;

}
