package it.polito.ai.project.server.services;

import it.polito.ai.project.server.controllers.NotificationController;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Team;
import it.polito.ai.project.server.entities.Token;
import it.polito.ai.project.server.repositories.TeamRepository;
import it.polito.ai.project.server.repositories.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.*;

@Transactional
@Service
public class NotificationServiceImpl implements NotificationService{

    @Autowired
    public JavaMailSender mailSender = this.getJavaMailSender();

    @Autowired
    public TokenRepository tokenRepository;

    @Autowired
    public TeamRepository teamRepository;

    @Autowired
    public TeamServiceImpl teamService;

    @Autowired
    public TeacherServiceImp teacherService;

    @Value("${spring.mail.username}")
    private String USERNAME;

    @Value("${spring.mail.password}")
    private String PWD;

    @Value("${spring.mail.port}")
    private int MAIL_PORT;

    @Value("${spring.mail.host}")
    private String MAIL_HOST;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String SMTP_AUTH;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String SMTP_STARTTLS;

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
                    .forEach(x -> this.teamService.evictTeam(x.getId()));
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
    public void notifyTeam(TeamDTO dto, List<String> memberIds) {
        String s1, s2, email;
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(ts);
        cal.add(Calendar.HOUR, 1);
        ts = new Timestamp(cal.getTime().getTime());

        for(String student : memberIds){
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

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(MAIL_HOST);
        mailSender.setPort(MAIL_PORT);

        mailSender.setUsername(USERNAME);
        mailSender.setPassword(PWD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", SMTP_AUTH);
        props.put("mail.smtp.starttls.enable", SMTP_STARTTLS);
        props.put("mail.debug", "true");

        return mailSender;
    }
}
