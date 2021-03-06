package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {
    List<Token> findAllByExpiryDateBefore(Timestamp t);
    List<Token> findAllByTeamId(Long teamId);
}
