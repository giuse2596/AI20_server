package it.polito.ai.project.server.dtos;

import lombok.Data;


@Data
public class TeamDTO {
    private Long id;

    private String name;

    private String proposer;

    private int status;

    private Integer cpuMax;

    private Integer ramMax;

    private Integer diskSpaceMax;

    private int totVM;

    private int activeVM;
}
