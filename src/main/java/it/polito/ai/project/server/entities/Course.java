package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Course {

    @Id
    @NotBlank
    private String name;

    @NotBlank
    private String acronym;

    @NotBlank
    private Integer min;

    @NotBlank
    private Integer max;

    @NotBlank
    private boolean enabled;

    @ManyToOne
    @JoinColumn(name= "teacher_id")
    Teacher teacher;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vmModel_id", referencedColumnName = "id")
    VMModel vmModel;

    @ManyToMany(mappedBy = "courses")
    List<Student> students = new ArrayList<Student>();

    @OneToMany(mappedBy = "course")
    List<Team> teams = new ArrayList<Team>();

    @OneToMany(mappedBy = "course")
    List<Assignment> assignments = new ArrayList<>();

    public void addStudent(Student student){
            students.add(student);
            student.getCourses().add(this);
    }

    public void addTeam(Team team){
        teams.add(team);
        team.setCourse(this);
    }

    public void removeTeam(Team team){
        teams.remove(team);
        team.setCourse(null);
    }

    public void addAssignment(Assignment assignment){
        this.assignments.add(assignment);
        assignment.setCourse(this);
    }

}
