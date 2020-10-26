package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class VirtualMachineDTO extends RepresentationModel<VirtualMachineDTO> {

    private String id;

    private Integer cpu;

    private Integer ram;

    private Integer diskSpace;

    private Boolean active;
}
