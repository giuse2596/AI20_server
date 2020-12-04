package it.polito.ai.project.server.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class VirtualMachine {
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String pathImage;

    @NotNull
    private Integer cpu;

    @NotNull
    private Integer ram;

    @NotNull
    private Integer diskSpace;

    // status virtual machine
    @NotNull
    private boolean active;

    @ManyToOne
    @JoinColumn(name= "team_id")
    Team team;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name="vm_owners", joinColumns = @JoinColumn(name="vm_id"),
            inverseJoinColumns = @JoinColumn(name="student_id") )
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
