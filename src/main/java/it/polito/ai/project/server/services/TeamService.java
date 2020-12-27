package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.dtos.TeamDTO;
import it.polito.ai.project.server.dtos.VirtualMachineDTO;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

public interface TeamService {
    List<TeamDTO> getEnabledTeamsForCourse(String courseName);
    List<TeamDTO> getTeamsForCourse(String courseName);
    List<TeamDTO> getTeamsForStudent(String studentId);
    TeamDTO getTeamForStudent(String studentId, String courseName);
    List<TeamDTO> getStudentTeamsNotEnabled(String courseName, String studentId);
    List<TeamDTO> getStudentTeamsEnabled(String courseName, String studentId);
    List<StudentDTO>getMembers(Long teamId);
    TeamDTO proposeTeam(String courseId, String name, String proposer, List<String> memberIds);
    List<StudentDTO> getStudentsInTeams(String courseName);
    List<StudentDTO> getAvailableStudents(String courseName);
    void enableTeam(Long teamId);
    void evictTeam(Long teamId);
    List<StudentDTO> getConfirmedMembersTeam(Long teamId);
    List<StudentDTO> getPendentMembersTeam(Long teamId);
}
