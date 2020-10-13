package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Teacher {

    @Id
    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String firstname;

    @NotBlank
    private String pathImage;

    @NotBlank
    private String email;

    @NotBlank
    private Long userId;

    @OneToMany(mappedBy = "teacher")
    List<Course> courses = new ArrayList<Course>();

    public void addCourse(Course course){
        this.courses.add(course);
        course.setTeacher(this);
    }

}
