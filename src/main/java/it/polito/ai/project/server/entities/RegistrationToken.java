package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Data
public class RegistrationToken {

    @Id
    private String id;

    @NotNull
    private Long userId;

}
