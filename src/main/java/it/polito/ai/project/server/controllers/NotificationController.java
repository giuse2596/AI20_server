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
        this.notificationService.confirm(token);
        return "confirm";
    }

    @GetMapping("/reject/{token}")
    public String rejectToken(@PathVariable String token){
        this.notificationService.reject(token);
        return "reject";
    }

    @PostMapping("/propose/{courseName}/{team}")
    public void notify(
                        @PathVariable String courseName,
                        @PathVariable String team,
                        @RequestBody List<String> students
                    )
    {
        TeamDTO teamDTO;

        try {
            teamDTO = teamService.proposeTeam(courseName, team, students);
        }
        catch (TeamServiceException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

       notificationService.notifyTeam(teamDTO, students);

    }

}
