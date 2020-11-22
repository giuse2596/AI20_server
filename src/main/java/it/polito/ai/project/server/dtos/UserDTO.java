package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
public class UserDTO extends RepresentationModel<TeacherDTO> {

    private Long id;

    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String serialNumber;

    @NotBlank
    private String name;

    @NotBlank
    private String firstname;

    @NotEmpty
    private boolean active;

}