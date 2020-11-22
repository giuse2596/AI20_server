package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Entity
@Data
public class RegistrationToken {

    @Id
    private String id;

    @NotBlank
    private Long userId;

}
