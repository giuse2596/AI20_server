package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, String> {
}
