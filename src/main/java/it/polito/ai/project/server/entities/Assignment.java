package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class Assignment {
    @Id
    @NotBlank
    private String id;

    @NotBlank
    private Date releaseDate;

    @NotBlank
    private Date expiryDate;

    @NotBlank
    private String pathImage;

    @ManyToOne
    @JoinColumn(name= "course_id")
    Course course;

    @OneToMany(mappedBy = "assignment")
    List<Homework> homeworks = new ArrayList<Homework>();

    public void addHomework(Homework homework){
        this.homeworks.add(homework);
        homework.setAssignment(this);
    }

}
