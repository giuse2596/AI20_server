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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Optional;

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
        user.setPathImage("src/main/resources/images/users/default_user.png");
        user.setActive(false);

        user = this.userRepository.save(user);

        student.setId(userDTO.getUsername());
        student.setEmail(userDTO.getEmail());
        student.setName(userDTO.getName());
        student.setFirstname(userDTO.getFirstname());
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
        user.setPathImage("src/main/resources/images/users/default_user.png");
        user.setActive(false);

        user = this.userRepository.save(user);

        teacher.setId(userDTO.getUsername());
        teacher.setEmail(userDTO.getEmail());
        teacher.setName(userDTO.getName());
        teacher.setFirstname(userDTO.getFirstname());
        teacher.setUserId(user.getId());

        this.teacherRepository.save(teacher);

        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public User getActiveUser(String username) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);

        if(!userOptional.isPresent()){
            throw new UserServiceException();
        }

        if(!userOptional.get().isActive()){
            throw new UserNotActiveException();
        }

        return userOptional.get();
    }

    @Override
    public void modifyUserImage(String username, MultipartFile multipartFile) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        File newFile;
        InputStream inputStream;
        OutputStream outputStream;

        if(!userOptional.isPresent()){
            throw new UserServiceException("User not found");
        }

        if(!userOptional.get().getPathImage().equals("src/main/resources/images/users/default_user.png")){
            newFile = new File(userOptional.get().getPathImage());
        } else {
            newFile = new File("src/main/resources/images/users/" + username + ".png");
        }

        try {
            inputStream = multipartFile.getInputStream();

            if (!newFile.exists()) {
                newFile.getParentFile().mkdir();
            }
            outputStream = new FileOutputStream(newFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e)
        {
            throw new UserServiceException("Error saving the file");
        }
    }

    @Override
    public byte[] getUserImage(String username) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if(!userOptional.isPresent()){
            throw new UserServiceException("User not found");
        }

        try {
            bufferedImage = ImageIO.read(new File(userOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        }
        catch (IOException e){
            throw new UserServiceException("Error reading the file");
        }

        return byteArrayOutputStream.toByteArray();

    }
}
