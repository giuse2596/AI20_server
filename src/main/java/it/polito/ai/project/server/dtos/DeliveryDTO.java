package it.polito.ai.project.server.dtos;

import it.polito.ai.project.server.entities.Delivery;
import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.sql.Timestamp;

@Data
public class DeliveryDTO {

    public enum Status{
        NULL,
        READ,
        DELIVERED,
        REVIEWED
    }

    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private Delivery.Status status;

    private String pathImage;

    private Timestamp timestamp;

}
