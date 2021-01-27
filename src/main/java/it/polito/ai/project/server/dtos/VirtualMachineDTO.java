package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class VirtualMachineDTO extends RepresentationModel<VirtualMachineDTO> {

    private Long id;

    @NotBlank
    private String name;

    private String pathImage;

    private String creator;

    @Min(1)
    private Integer cpu;

    @Min(1)
    private Integer ram;

    @Min(1)
    private Integer diskSpace;

    private boolean active;
}
