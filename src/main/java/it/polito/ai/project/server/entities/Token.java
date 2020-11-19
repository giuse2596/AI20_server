package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.sql.Timestamp;

@Entity
@Data
public class Token {
    @Id
    private String id;

    @NotBlank
    private Long teamId;

    @NotBlank
    private String studentId;

    @NotEmpty
    private Timestamp expiryDate;
}
