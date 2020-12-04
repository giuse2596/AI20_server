package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Homework {

    @Id
    @GeneratedValue
    private Long id;

    private Integer mark;

    @NotNull
    private boolean editable;

    @ManyToOne
    @JoinColumn(name= "student_id")
    Student student;

    @ManyToOne
    @JoinColumn(name= "assignment_id")
    Assignment assignment;

    @OneToMany(mappedBy = "homework")
    List<Delivery> deliveries = new ArrayList<>();

    public void setStudent(Student student){
        if(student == null){
            if(this.student != null) {
                this.student.getHomeworks().remove(this);
            }
        }
        else{
            student.getHomeworks().add(this);
        }
        this.student = student;
    }

    public void setAssignment(Assignment assignment){
        if(assignment == null){
            if(this.assignment != null){
                this.assignment.getHomeworks().remove(this);
            }
        }
        else{
            assignment.getHomeworks().add(this);
        }
        this.assignment = assignment;
    }

    public void addDelivery(Delivery delivery){
        this.deliveries.add(delivery);
        delivery.homework = this;
    }

    public void removeDelivery(Delivery delivery){
        this.deliveries.remove(delivery);
        delivery.homework = null;
    }

}
