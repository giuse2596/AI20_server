package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class TeacherDTO extends RepresentationModel<TeacherDTO> {

    private String id;

    private String name;

    private String firstName;

    private String email;

    private Long userId;

}
