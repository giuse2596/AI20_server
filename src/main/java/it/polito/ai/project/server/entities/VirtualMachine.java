package it.polito.ai.project.server.entities;

import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
public class VirtualMachine {
    @Id
    @NotBlank
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private Integer cpu;

    @NotBlank
    private Integer ram;

    @NotBlank
    private Integer diskSpace;

    // status virtual machine
    @NotBlank
    private Boolean active = false;

    @ManyToOne
    @JoinColumn(name= "team_id")
    Team team;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name="vm_owners", joinColumns = @JoinColumn(name="student_id"),
            inverseJoinColumns = @JoinColumn(name="vm_id") )
    List<Student> owners = new ArrayList<>();

    public void setTeam(Team team){
        if(team == null){
            if(this.team != null){
                this.team.getVirtualMachines().remove(this);
            }
        }
        else{
            team.virtualMachines.add(this);
        }
        this.team = team;
    }

    public void addOwner(Student student){
        this.owners.add(student);
        student.getVirtualMachines().add(this);
    }

    public void removeOwner(Student student){
        this.owners.remove(student);
        student.getVirtualMachines().remove(this);
    }

}
