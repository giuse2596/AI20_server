package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Team {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private int status;

    @NotNull
    private int totVM;

    @NotNull
    private int activeVM;

    @OneToMany( mappedBy = "team")
    List<VirtualMachine> virtualMachines = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name= "course_id")
    Course course;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name="team_student", joinColumns = @JoinColumn(name="team_id"),
                inverseJoinColumns = @JoinColumn(name="student_id"))
    List<Student> members = new ArrayList<Student>();

    public void setCourse(Course course){
        if(course == null){
            if(this.course != null) {
                this.course.getTeams().remove(this);
            }
        }
        else {
            course.getTeams().add(this);
        }
        this.course = course;
    }

    public void addMember(Student student){
        this.members.add(student);
        student.getTeams().add(this);
    }

    public void removeMember(Student student){
        this.members.remove(student);
        student.getTeams().remove(this);
    }

    public void addVM(VirtualMachine virtualMachine){
        this.virtualMachines.add(virtualMachine);
        virtualMachine.team = this;
    }

    public void removeVM(VirtualMachine virtualMachine){
        this.virtualMachines.remove(virtualMachine);
        virtualMachine.team = null;
    }
}
