package it.polito.ai.project.server.services;

import it.polito.ai.project.server.controllers.NotificationController;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.*;
import it.polito.ai.project.server.repositories.RegistrationTokenRepository;
import it.polito.ai.project.server.repositories.TeamRepository;
import it.polito.ai.project.server.repositories.TokenRepository;
import it.polito.ai.project.server.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
public class NotificationServiceImpl implements NotificationService{

    @Autowired
    public JavaMailSender mailSender;

    @Autowired
    public TokenRepository tokenRepository;

    @Autowired
    public RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    public TeamRepository teamRepository;

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public TeamServiceImpl teamService;

    @Autowired
    public TeacherServiceImp teacherService;

    @Async
    @Override
    public void sendMessage(String address, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(address);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public boolean confirm(String token) {
        Optional<Token> t = this.tokenRepository.findById(token);
        Optional<Team> teamOptional;
        List<Long> teamsId = new ArrayList<>();
        List<Token> tokensId = new ArrayList<>();

        // verify if exist
        if(!t.isPresent()){
            return false;
        }

        teamOptional = teamRepository.findById(t.get().getTeamId());

        // check if the team exists
        if(!teamOptional.isPresent()){
            return false;
        }

        // verify if is expired
        if(t.get().getExpiryDate().before(new Timestamp(System.currentTimeMillis()))){
            this.reject(token);
            return false;
        }

        // check if is the last student to confirm and if so enable the team and remove
        // all pending requests from all other teams of this course that have this
        // team of students as proposed members
        if(this.tokenRepository.findAllByTeamId(t.get().getTeamId()).stream().count() == 1){
            this.teamService.enableTeam(t.get().getTeamId());

            teamOptional.get()
                    .getMembers()
                    .stream()
                    .map(Student::getTeams)
                    .flatMap(Collection::stream)
                    .filter(x ->
                            (!x.getId().equals(teamOptional.get().getId())) &&
                            (x.getCourse().getName().equals(teamOptional.get().getCourse().getName()))
                            )
                    .forEach(x -> {

                        // check to avoid duplicates
                        if(!teamsId.contains(x.getId())){
                            teamsId.add(x.getId());
                        }
                    });

            // remove the pending teams
            teamsId.forEach(x -> this.teamService.evictTeam(x));

            // store the remaining tokens of the pending teams
            teamsId.forEach(x -> tokensId.addAll(this.tokenRepository.findAllByTeamId(x)));

            // delete the remaining tokens of the pending teams
            tokensId.forEach(x -> this.tokenRepository.delete(x));

        }

        this.tokenRepository.deleteById(token);

        return true;
    }

    @Override
    public boolean reject(String token) {
        Optional<Token> t = this.tokenRepository.findById(token);

        // verify if exist
        if(!t.isPresent()){
            return false;
        }

        // delete all the remaining token
        this.tokenRepository.findAllByTeamId(t.get().getTeamId())
                .forEach(x -> tokenRepository.deleteById(x.getId()));

        this.teamService.evictTeam(t.get().getTeamId());
        return true;
    }

    @Override
    public void notifyTeam(TeamDTO dto, List<String> memberIds, Long expiryOffset) {
        String s1, s2, email;
        Timestamp ts = new Timestamp(System.currentTimeMillis() + expiryOffset);

        for(String student : memberIds){

            // the the token is not sent to the proposer, doing so
            // is like he confirmed the link sent by email,
            // the function getConfirmedMembersTeam in TeamServiceImpl
            // does not return him
            if(!dto.getProposer().equals(student)){

                Token t = new Token();
                t.setExpiryDate(ts);
                t.setTeamId(dto.getId());
                t.setStudentId(student);
                t.setId(UUID.randomUUID().toString());
                this.tokenRepository.save(t);

                s1 = WebMvcLinkBuilder.linkTo(NotificationController.class)
                        .slash("confirm")
                        .slash(t.getId()).toString();

                s2 = WebMvcLinkBuilder.linkTo(NotificationController.class)
                        .slash("reject")
                        .slash(t.getId()).toString();

                email = student + "@studenti.polito.it";

                sendMessage(email,
                        "Invitation to group " + dto.getName(),
                        "confirm at: " + s1 + " or reject at: " + s2);

            }
        }

    }

    @Override
    public void notifyInscription(UserDTO userDTO){
        RegistrationToken registrationToken = new RegistrationToken();
        String link;

        registrationToken.setId(UUID.randomUUID().toString());
        registrationToken.setUserId(userDTO.getId());
        this.registrationTokenRepository.save(registrationToken);

        link = WebMvcLinkBuilder.linkTo(NotificationController.class)
                .slash("confirmRegistration")
                .slash(registrationToken.getId()).toString();

        sendMessage(userDTO.getEmail(),
                "Confirm registration ",
                "Welcome! Confirm your registration at: " + link);
    }

    @Override
    public boolean confirmRegistration(String token){
        Optional<RegistrationToken> registrationTokenOptional =
                this.registrationTokenRepository.findById(token);
        Optional<User> userOptional;

        // check if the token exists
        if (!registrationTokenOptional.isPresent()) {
            return false;
        }

        userOptional = this.userRepository.findById(registrationTokenOptional.get().getUserId());

        if (!userOptional.isPresent()) {
            return false;
        }

        userOptional.get().setActive(true);

        this.registrationTokenRepository.delete(registrationTokenOptional.get());

        return true;
    }

}
