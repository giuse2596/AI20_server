package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class VMModel {
    @Id
    @NotBlank
    private String id;

    @NotBlank
    private Integer cpuMax;

    @NotBlank
    private Integer ramMax;

    @NotBlank
    private Integer diskSpaceMax;

    @NotBlank
    private Integer totalInstances;

    @NotBlank
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
