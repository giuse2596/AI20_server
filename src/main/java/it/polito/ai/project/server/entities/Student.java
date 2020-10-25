package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Student {

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

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name="student_course", joinColumns = @JoinColumn(name="student_id"),
                inverseJoinColumns = @JoinColumn(name="course_name") )
    List<Course> courses = new ArrayList<Course>();

    @ManyToMany(mappedBy = "members")
    List<Team> teams = new ArrayList<Team>();

    @ManyToMany(mappedBy = "student")
    List<VirtualMachine> virtualMachines = new ArrayList<>();

    @OneToMany( mappedBy = "student")
    List<Homework> homeworks = new ArrayList<>();

    public void addCourse(Course course){
            courses.add(course);
            course.getStudents().add(this);
    }

    public void removeCourse(Course course){
        courses.remove(course);
        course.getStudents().remove(this);
    }

    public void addTeam(Team team){
        this.teams.add(team);
        team.getMembers().add(this);
    }

    public void removeTeam(Team team){
        this.teams.remove(team);
        team.getMembers().remove(this);
    }

    public void addVM(VirtualMachine virtualMachine){
        this.virtualMachines.add(virtualMachine);
        virtualMachine.getOwners().add(this);
    }

    public void removeVM(VirtualMachine virtualMachine){
        this.virtualMachines.remove(virtualMachine);
        virtualMachine.getOwners().remove(this);
    }

    public void addHomework(Homework homework){
        this.homeworks.add(homework);
        homework.student = this;
    }

}
