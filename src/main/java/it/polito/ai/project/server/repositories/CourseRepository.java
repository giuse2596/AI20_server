package it.polito.ai.project.server.repositories;

import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course,String> {

    @Query("SELECT s FROM Student s INNER JOIN s.teams t INNER JOIN t.course c WHERE c.name=:courseName AND t.active=true")
    List<Student> getStudentsInTeams(String courseName);

    @Query("SELECT s FROM Student s INNER JOIN s.courses c WHERE c.name=:courseName AND s NOT IN " +
            "(SELECT s FROM Student s INNER JOIN s.teams t INNER JOIN t.course c WHERE c.name=:courseName AND t.active=true)")
    List<Student> getStudentsNotInTeams(String courseName);

    @Query("SELECT t FROM Teacher t WHERE t NOT IN " +
            "(SELECT t FROM Teacher t INNER JOIN t.courses c WHERE c.name=:courseName)")
    List<Teacher> getTeachersNotInCourse(String courseName);
}
