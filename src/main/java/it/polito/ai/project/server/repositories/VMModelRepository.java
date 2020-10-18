package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.VMModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VMModelRepository  extends JpaRepository<VMModel, String> {
}
