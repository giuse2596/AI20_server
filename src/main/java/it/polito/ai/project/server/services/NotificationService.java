package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.UserDTO;

import java.sql.Date;
import java.util.List;

public interface NotificationService {
    void sendMessage(String address, String subject, String body);
    boolean confirm(String token);
    boolean reject(String token);
    void notifyTeam(TeamDTO dto, List<String> memberIds, Date expiryDate);
    void notifyInscription(UserDTO userDTO);
    boolean confirmRegistration(String token);
}
