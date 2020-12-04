package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Entity
@Data
public class Token {
    @Id
    private String id;

    @NotNull
    private Long teamId;

    @NotBlank
    private String studentId;

    @NotNull
    private Timestamp expiryDate;
}
