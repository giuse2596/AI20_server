package it.polito.ai.project.server.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Date;

@Data
public class AssignmentFileDTO {

//    @NotBlank
//    private String name;
//
//    @NotNull
//    private Date expiryDate;

    @NotNull
    private MultipartFile multipartFile;

}
