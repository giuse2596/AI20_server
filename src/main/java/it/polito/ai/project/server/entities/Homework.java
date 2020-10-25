package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Data
@Entity
public class Homework {

    @Id
    @NotBlank
    private String id;

    @NotBlank
    private String pathImage;

    // all'inizio e' null?
    private String status;

    @ManyToOne
    @JoinColumn(name= "student_id")
    Student student;

    @ManyToOne
    @JoinColumn(name= "assignment_id")
    Assignment assignment;

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
}
