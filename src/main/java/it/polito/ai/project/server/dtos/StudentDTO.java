package it.polito.ai.project.server.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

@Data
public class StudentDTO extends RepresentationModel<StudentDTO> {
    @CsvBindByName
    private String id;

    @CsvBindByName
    private String name;

    @CsvBindByName
    private String firstName;

    @CsvBindByName
    private String email;

    @CsvBindByName
    private Long userId;


}
