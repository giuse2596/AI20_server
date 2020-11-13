package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

    @NotEmpty
    private Integer min;

    @NotEmpty
    private Integer max;

    @NotEmpty
    private boolean enabled;

    @ManyToMany(mappedBy = "courses")
    List<Teacher> teachers = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vmModel_id", referencedColumnName = "id")
    VMModel vmModel;

    @ManyToMany(mappedBy = "courses")
    List<Student> students = new ArrayList<Student>();

    @OneToMany(mappedBy = "course")
    List<Team> teams = new ArrayList<Team>();

    @OneToMany(mappedBy = "course")
    List<Assignment> assignments = new ArrayList<>();

    public void addTeacher(Teacher teacher){
        this.teachers.add(teacher);
        teacher.getCourses().add(this);
    }

    public void removeTeacher(Teacher teacher){
        this.teachers.remove(teacher);
        teacher.getCourses().remove(this);
    }

    public void setVMModel(VMModel vmModel){
        if(vmModel == null){
            if(this.vmModel != null){
                this.vmModel.course = null;
            }
        }
        else{
            vmModel.course = this;
        }
        this.vmModel = vmModel;
    }

    public void addStudent(Student student){
        this.students.add(student);
        student.getCourses().add(this);
    }

    public void removeStudent(Student student){
        students.remove(student);
        student.getCourses().remove(this);
    }

    public void addTeam(Team team){
        this.teams.add(team);
        team.course = this;
    }

    public void removeTeam(Team team){
        this.teams.remove(team);
        team.course = null;
    }

    public void addAssignment(Assignment assignment){
        this.assignments.add(assignment);
        assignment.course = this;
    }

    public void removeAssignment(Assignment assignment){
        this.assignments.remove(assignment);
        assignment.course = null;
    }

}
