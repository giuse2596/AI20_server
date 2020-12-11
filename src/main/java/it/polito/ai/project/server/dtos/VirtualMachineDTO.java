package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class VirtualMachineDTO extends RepresentationModel<VirtualMachineDTO> {

    private Long id;

    @NotBlank
    private String name;

    private String pathImage;

    @NotNull
    private Integer cpu;

    @NotNull
    private Integer ram;

    @NotNull
    private Integer diskSpace;

    private boolean active;
}
