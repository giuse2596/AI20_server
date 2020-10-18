package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.VirtualMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualMachinesRepository  extends JpaRepository<VirtualMachine, String> {
}
