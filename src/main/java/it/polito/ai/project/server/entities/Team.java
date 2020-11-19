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
public class Team {

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String proposer;

    @NotEmpty
    private boolean active;

    @NotEmpty
    private Integer cpuMax;

    @NotEmpty
    private Integer ramMax;

    @NotEmpty
    private Integer diskSpaceMax;

    @NotEmpty
    private int totVM;

    @NotEmpty
    private int activeVM;

    // the size of this list must be < than totVM
    // and the active virtual machines of the list must be < than activeVM
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
