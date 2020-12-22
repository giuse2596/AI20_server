package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Date;

@Data
public class AssignmentDTO extends RepresentationModel<AssignmentDTO> {

    private Long id;

    @NotBlank
    private String name;

    private Date releaseDate;

    @NotNull
    private Date expiryDate;

    private String pathImage;

    @NotNull
    private MultipartFile multipartFile;

}
