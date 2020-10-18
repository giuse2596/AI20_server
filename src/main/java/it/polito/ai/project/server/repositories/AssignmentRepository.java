package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository  extends JpaRepository<Assignment, String> {
}
