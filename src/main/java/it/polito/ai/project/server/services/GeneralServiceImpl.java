package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.AssignmentDTO;
import it.polito.ai.project.server.dtos.CourseDTO;
import it.polito.ai.project.server.dtos.DeliveryDTO;
import it.polito.ai.project.server.dtos.StudentDTO;
import it.polito.ai.project.server.entities.Assignment;
import it.polito.ai.project.server.entities.Course;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.repositories.AssignmentRepository;
import it.polito.ai.project.server.repositories.CourseRepository;
import it.polito.ai.project.server.repositories.StudentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class GeneralServiceImpl implements GeneralService{

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    ModelMapper modelMapper;

    /**
     * Retrieve an existing course
     * @param name the name of the existing course
     * @return an Optional<CourseDTO>
     */
    @Override
    public Optional<CourseDTO> getCourse(String name) {
        return courseRepository.findById(name).map(x -> modelMapper.map(x, CourseDTO.class));
    }

    /**
     * Retrieve a student
     * @param studentId the student id string of the student to retrieve
     * @return an Optional<StudentDTO>
     */
    @Override
    public Optional<StudentDTO> getStudent(String studentId) {
        return studentRepository.findById(studentId)
                .map(x -> modelMapper.map(x, StudentDTO.class));
    }

    /**
     * Retrieve all students present in the db
     * @return a list with all the students present in the db
     */
    @Override
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(x -> modelMapper.map(x, StudentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the assignment object
     * @param assignmentId the id of the assignment
     * @return the assignment DTO
     */
    @Override
    public AssignmentDTO getAssignment(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new GeneralServiceException("The assignment does not exists");
        }

        return modelMapper.map(assignmentOptional.get(), AssignmentDTO.class);
    }

    /**
     * Retrieve all the assignments of the course
     * @param courseName the name of the course
     * @return all the assignments of the course
     */
    @Override
    public List<AssignmentDTO> getCourseAssignments(String courseName) {
        Optional<Course> courseOptional = this.courseRepository.findById(courseName);
        List<Assignment> assignments;

        //check if the course exists
        if(!courseOptional.isPresent()){
            throw new TeacherServiceException("Course not found");
        }

        assignments = courseOptional.get().getAssignments();

        return assignments
                .stream()
                .map(x -> modelMapper.map(x, AssignmentDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all the students' last deliveries for an assignment
     * @param assignmentId the id of the assignment
     * @return all the students' last deliveries for an assignment
     */
    @Override
    public List<DeliveryDTO> getAssignmentLastDeliveries(Long assignmentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        List<DeliveryDTO> deliveries;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new TeacherServiceException("Assignment not found");
        }

        deliveries = assignmentOptional.get().getHomeworks()
                .stream()
                .map(x -> x.getDeliveries().get(x.getDeliveries().size()-1))
                .map(x -> modelMapper.map(x, DeliveryDTO.class))
                .collect(Collectors.toList());

        return deliveries;
    }

    /**
     * Retrieve the list of the deliveries of one student
     * @param assignmentId the assignment id
     * @param studentId the student id
     * @return the list of the assignment of the student
     */
    @Override
    public List<DeliveryDTO> getAssignmentStudentDeliveries(Long assignmentId, String studentId) {
        Optional<Assignment> assignmentOptional = this.assignmentRepository.findById(assignmentId);
        Optional<Student> studentOptional = this.studentRepository.findById(studentId);
        List<DeliveryDTO> deliveries;

        // check if the assignment exists
        if(!assignmentOptional.isPresent()){
            throw new TeacherServiceException("Assignment not found");
        }

        // check if the student exists
        if(!studentOptional.isPresent()){
            throw new TeacherServiceException("Student not found");
        }

        deliveries =  assignmentOptional.get().getHomeworks()
                .stream()
                .filter(x -> x.getStudent().getId().equals(studentId))
                .flatMap(x -> x.getDeliveries().stream())
                .map(x -> modelMapper.map(x, DeliveryDTO.class))
                .collect(Collectors.toList());

        return deliveries;
    }

}
