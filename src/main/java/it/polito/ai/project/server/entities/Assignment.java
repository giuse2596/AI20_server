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
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

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
        homework.assignment = this;
    }

    public void setCourse(Course course){
        if(course == null){
            if(this.course != null) {
                this.course.getAssignments().remove(this);
            }
        }
        else{
            course.getAssignments().add(this);
        }
        this.course = course;
    }
}
