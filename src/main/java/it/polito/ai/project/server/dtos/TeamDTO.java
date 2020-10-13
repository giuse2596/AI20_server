package it.polito.ai.project.server.dtos;

import lombok.Data;

@Data
public class TeamDTO {
    Long id;

    String name;

    int status;
}
