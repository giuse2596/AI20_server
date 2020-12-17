package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class UserDTO extends RepresentationModel<UserDTO> {

    private Long id;

    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String username;

    @NotBlank
    private String name;

    @NotBlank
    private String firstName;

    private String pathImage;

    private boolean active;

}
