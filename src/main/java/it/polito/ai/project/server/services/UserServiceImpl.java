package it.polito.ai.project.server.services;

import it.polito.ai.project.server.dtos.UserDTO;
import it.polito.ai.project.server.entities.Student;
import it.polito.ai.project.server.entities.Teacher;
import it.polito.ai.project.server.entities.User;
import it.polito.ai.project.server.repositories.StudentRepository;
import it.polito.ai.project.server.repositories.TeacherRepository;
import it.polito.ai.project.server.repositories.UserRepository;
import org.modelmapper.ModelMapper;
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
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public UserDTO registerStudent(UserDTO userDTO) {
        User user = new User();
        Student student = new Student();

        if(this.userRepository.findByUsername(userDTO.getEmail()).isPresent()){
            throw new UserServiceException();
        }

        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setFirstname(userDTO.getFirstname());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_STUDENT"));
        user.setActive(false);

        user = this.userRepository.save(user);

        student.setId(userDTO.getUsername());
        student.setEmail(userDTO.getEmail());
        student.setName(userDTO.getName());
        student.setFirstname(userDTO.getFirstname());
        student.setPathImage("default/path/image"); // CHANGE
        student.setUserId(user.getId());

        this.studentRepository.save(student);

        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO registerTeacher(UserDTO userDTO) {
        User user = new User();
        Teacher teacher = new Teacher();

        if(this.userRepository.findByUsername(userDTO.getEmail()).isPresent()){
            throw new UserServiceException();
        }

        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setFirstname(userDTO.getFirstname());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_TEACHER"));
        user.setActive(false);

        user = this.userRepository.save(user);

        teacher.setId(userDTO.getUsername());
        teacher.setEmail(userDTO.getEmail());
        teacher.setName(userDTO.getName());
        teacher.setFirstname(userDTO.getFirstname());
        teacher.setPathImage("default/path/image"); // CHANGE
        teacher.setUserId(user.getId());

        this.teacherRepository.save(teacher);

        return modelMapper.map(user, UserDTO.class);
    }
}
