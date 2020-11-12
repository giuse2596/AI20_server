package it.polito.ai.project.server.entities;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.sql.Timestamp;

@Entity
@Data
public class Delivery {

    public enum Status{
        NULL,
        READ,
        DELIVERED,
        REVIEWED
    }

    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Enumerated(EnumType.ORDINAL)
    private Status status;

    // when status is NULL set the path to a default image
    @NotBlank
    private String pathImage;

    @NotBlank
    private Timestamp timestamp;

    @ManyToOne
    @JoinColumn(name = "homework_id")
    Homework homework;

    public void setHomework(Homework homework){
        if(homework == null){
            if(this.homework != null){
                this.homework.getDeliveries().remove(this);
            }
        }
        else{
            homework.getDeliveries().add(this);
        }
        this.homework = homework;
    }

}
