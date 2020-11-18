package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.NotBlank;

@Data
public class VirtualMachineDTO extends RepresentationModel<VirtualMachineDTO> {

    private Long id;

    private String name;

    private String pathImage;

    private Integer cpu;

    private Integer ram;

    private Integer diskSpace;

    private boolean active;
}
