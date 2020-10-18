package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import java.util.Date;

@Data
public class AssignmentDTO extends RepresentationModel<AssignmentDTO> {

    private String id;

    private Date releaseDate;

    private Date expiryDate;

    private String pathImage;

}
