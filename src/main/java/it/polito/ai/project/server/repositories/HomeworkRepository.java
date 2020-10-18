package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeworkRepository  extends JpaRepository<Homework, String> {
}
