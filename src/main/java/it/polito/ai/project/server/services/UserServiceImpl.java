package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Teacher;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeacherRepository;
import it.polito.ai.project.server.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
@Transactional
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public void registerStudent(UserDTO userDTO) {
        User user = new User();
        Student student = new Student();

        if(this.userRepository.findByUsername(userDTO.getEmail()).isPresent()){
            throw new UserServiceException();
        }

        user.setName(userDTO.getName());
        user.setFirstname(userDTO.getFirstname());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_STUDENT"));
        user.setActive(false);

        user = this.userRepository.save(user);

        student.setId(userDTO.getSerialNumber());
        student.setEmail(userDTO.getEmail());
        student.setName(userDTO.getName());
        student.setFirstname(userDTO.getFirstname());
        student.setPathImage("default/path/image"); // CHANGE
        student.setUserId(this.userRepository.findByUsername(userDTO.getEmail()).get().getId());

        this.studentRepository.save(student);

        // send activation email
        userDTO.setId(user.getId());
        this.notificationService.notifyInscription(userDTO);
    }

    @Override
    public void registerTeacher(UserDTO userDTO) {
        User user = new User();
        Teacher teacher = new Teacher();

        if(this.userRepository.findByUsername(userDTO.getEmail()).isPresent()){
            throw new UserServiceException();
        }

        user.setName(userDTO.getName());
        user.setFirstname(userDTO.getFirstname());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_TEACHER"));
        user.setActive(false);

        user = this.userRepository.save(user);

        teacher.setId(userDTO.getSerialNumber());
        teacher.setEmail(userDTO.getEmail());
        teacher.setName(userDTO.getName());
        teacher.setFirstname(userDTO.getFirstname());
        teacher.setPathImage("default/path/image"); // CHANGE
        teacher.setUserId(this.userRepository.findByUsername(userDTO.getEmail()).get().getId());

        this.teacherRepository.save(teacher);

        // send activation email
        userDTO.setId(user.getId());
        this.notificationService.notifyInscription(userDTO);
    }
}
