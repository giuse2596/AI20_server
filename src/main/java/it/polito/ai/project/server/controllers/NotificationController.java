package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.TeamDTO;
// import it.polito.ai.es2.esercitazione2.services.*;
import it.polito.ai.project.server.services.*;
import it.polito.ai.project.server.services.NotificationServiceImpl;
import it.polito.ai.project.server.services.TeamService;
import it.polito.ai.project.server.services.TeamServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    NotificationServiceImpl notificationService;

    @Autowired
    private TeamService teamService;

    @GetMapping("/confirm/{token}")
    public String confirmToken(@PathVariable String token){
        if (this.notificationService.confirm(token)) {
            return "confirm";
        }
        return "error";
    }

    @GetMapping("/reject/{token}")
    public String rejectToken(@PathVariable String token){
        if (this.notificationService.reject(token)) {
            return "reject";
        }
        return "error";
    }

    @GetMapping("/confirmRegistration/{token}")
    public String confirmRegistration(@PathVariable String token){
        if (this.notificationService.confirmRegistration(token)) {
            return "confirm_registration";
        }
        return "error";
    }

    @PostMapping("/propose/{courseName}/{team}/{proposerid}")
    public void notify(
                        @PathVariable String courseName,
                        @PathVariable String team,
                        @PathVariable String proposerid,
                        @RequestBody List<String> students,
                        @RequestParam @NotNull Long expiryOffset
                    )
    {
        TeamDTO teamDTO;

        try {
            teamDTO = teamService.proposeTeam(courseName, team, proposerid, students);
        }
        catch (StudentNotFoundExeption | CourseNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        catch (TeamServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

       notificationService.notifyTeam(teamDTO, students, expiryOffset);

    }

}
