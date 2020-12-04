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
public class VMModel {
    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private Integer cpuMax;

    @NotNull
    private Integer ramMax;

    @NotNull
    private Integer diskSpaceMax;

    @NotNull
    private Integer totalInstances;

    @NotNull
    private Integer activeInstances;

    @OneToOne( mappedBy = "vmModel")
    Course course;

    public void setCourse(Course course){
        if(course == null){
            if(this.course != null){
                this.course.vmModel = null;
            }
        }
        else{
            course.vmModel = this;
        }
        this.course = course;
    }

}
