package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import java.sql.Date;

@Data
public class AssignmentDTO extends RepresentationModel<AssignmentDTO> {

    private Long id;

    private String name;

    private Date releaseDate;

    private Date expiryDate;

    private String pathImage;

}
