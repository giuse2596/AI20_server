package it.polito.ai.project.server.dtos;

import lombok.Data;

@Data
public class TeamDTO {
    private Long id;

    private String name;

    private int status;

    private int totVM;

    private int activeVM;
}
