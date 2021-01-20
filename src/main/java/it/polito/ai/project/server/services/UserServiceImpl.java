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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
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
    public Optional<UserDTO> getUser(String username) {
        return this.userRepository.findByUsername(username).map(x -> modelMapper.map(x, UserDTO.class));
    }

    @Override
    public UserDTO registerStudent(UserDTO userDTO) {
        User user = new User();
        Student student = new Student();

        if(this.userRepository.findByUsername(userDTO.getUsername()).isPresent()){
            throw new UserServiceException();
        }

        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setFirstName(userDTO.getFirstName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_STUDENT"));
        user.setPathImage("src/main/resources/images/users/default_user.png");
        user.setActive(false);

        user = this.userRepository.save(user);

        student.setId(userDTO.getUsername());
        student.setEmail(userDTO.getEmail());
        student.setName(userDTO.getName());
        student.setFirstName(userDTO.getFirstName());
        student.setUserId(user.getId());

        this.studentRepository.save(student);

        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO registerTeacher(UserDTO userDTO) {
        User user = new User();
        Teacher teacher = new Teacher();

        if(this.userRepository.findByUsername(userDTO.getUsername()).isPresent()){
            throw new UserServiceException();
        }

        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setFirstName(userDTO.getFirstName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        user.setRoles(Arrays.asList("ROLE_TEACHER"));
        user.setPathImage("src/main/resources/images/users/default_user.png");
        user.setActive(false);

        user = this.userRepository.save(user);

        teacher.setId(userDTO.getUsername());
        teacher.setEmail(userDTO.getEmail());
        teacher.setName(userDTO.getName());
        teacher.setFirstName(userDTO.getFirstName());
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
    public UserDTO modifyUser(String username, HashMap<String, String> passwords) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        String newPassword = bCryptPasswordEncoder.encode(passwords.get("newPassword"));
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        // check if user exists
        if(!userOptional.isPresent()){
            throw new GeneralServiceException("User not found");
        }

        // check if the first password is the same of the original one
        if(!encoder.matches(passwords.get("oldPassword"), userOptional.get().getPassword())){
            throw new GeneralServiceException("Password does not match");
        }

        userOptional.get().setPassword(newPassword);

        this.userRepository.save(userOptional.get());

        return modelMapper.map(userOptional.get(), UserDTO.class);
    }

    @Override
    public void modifyUserImage(String username, MultipartFile multipartFile) {
        Optional<User> userOptional = this.userRepository.findByUsername(username);
        Optional<String> extension;
        File newFile;
        InputStream inputStream;
        OutputStream outputStream;

        if(!userOptional.isPresent()){
            throw new UserServiceException("User not found");
        }

        // get the extension of the file
        extension = Optional.ofNullable(multipartFile.getOriginalFilename())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(multipartFile.getOriginalFilename().lastIndexOf(".") + 1));

        // check if the file has an extension
        if(!extension.isPresent()){
            throw new NoExtensionException();
        }

        // check if the image is the default one if not delete the old image
        if(!userOptional.get().getPathImage().equals("src/main/resources/images/users/default_user.png")){

            // delete the image
            try {
                Files.delete(Paths.get(userOptional.get().getPathImage()));
            } catch (IOException e) {
                throw new UserServiceException("Error deleting the old image");
            }

        }

        // set the new path to the image
        userOptional.get().setPathImage(
                "src/main/resources/images/users/" + username + "." + extension.get()
        );

        // get the user image
        newFile = new File(userOptional.get().getPathImage());

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
        Optional<String> extension;
        BufferedImage bufferedImage;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if(!userOptional.isPresent()){
            throw new UserServiceException("User not found");
        }

        // get the extension of the file
        extension = Optional.ofNullable(userOptional.get().getPathImage())
                .filter(x -> x.contains("."))
                .map(x -> x.substring(userOptional.get().getPathImage().lastIndexOf(".") + 1));

        try {
            bufferedImage = ImageIO.read(new File(userOptional.get().getPathImage()));
            ImageIO.write(bufferedImage, extension.get(), byteArrayOutputStream);
        }
        catch (IOException e){
            throw new UserServiceException("Error reading the file");
        }

        return byteArrayOutputStream.toByteArray();

    }
}
