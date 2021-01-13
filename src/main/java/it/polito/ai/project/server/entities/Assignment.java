package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

@Entity
@Data
public class Assignment {
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Date releaseDate;

    @NotNull
    private Date expiryDate;

    @NotBlank
    private String pathImage;

    @ManyToOne
    @JoinColumn(name= "course_id")
    Course course;

    @OneToMany(mappedBy = "assignment", fetch = FetchType.EAGER)
    List<Homework> homeworks = new ArrayList<Homework>();

    public void addHomework(Homework homework){
        this.homeworks.add(homework);
        homework.assignment = this;
    }

    public void removeHomework(Homework homework){
        this.homeworks.remove(homework);
        homework.assignment = null;
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
