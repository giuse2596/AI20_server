package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Min;

@Data
public class VMModelDTO extends RepresentationModel<VMModelDTO> {

    private Long id;

    @Min(1)
    private Integer cpuMax;

    @Min(1)
    private Integer ramMax;

    @Min(1)
    private Integer diskSpaceMax;

    @Min(1)
    private Integer totalInstances;

    @Min(1)
    private Integer activeInstances;

}
