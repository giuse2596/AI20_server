package it.polito.ai.project.server.controllers;

import it.polito.ai.project.server.dtos.*;
import it.polito.ai.project.server.services.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/API/students")
public class StudentController {

    @Autowired
    TeamService teamService;

    @Autowired
    TeacherService teacherService;

    @Autowired
    StudentService studentService;

    @Autowired
    GeneralService generalService;

    ModelHelper modelHelper;

    /**
     * URL to retrieve all the students
     * @return the list of all the students
     */
    @GetMapping({"", "/"})
    public List<StudentDTO> all(){
        return generalService.getAllStudents()
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    /**
     * URL to retreive a student
     * @param id the id of the student
     * @return the student enriched with the URLs to the student services
     */
    @GetMapping("/{id}")
    public StudentDTO getOne(@PathVariable String id){
        Optional<StudentDTO> student = generalService.getStudent(id);

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }
        return modelHelper.enrich(student.get());
    }

    /**
     * URL to retrieve all the courses where a student is enrolled in
     * @param userDetails the user
     * @return the list of all courses where the student is enrolled in
     */
    @GetMapping("/{id}/courses")
    public List<CourseDTO> getCourses(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable String id){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, userDetails.getUsername());
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return teamService
                .getCourses(userDetails.getUsername())
                .stream()
                .map(x -> modelHelper.enrich(x))
                .collect(Collectors.toList());
    }

    /**
     * URL to retrieve all the teams where a student is in
     * @param id the id of the student
     * @return the list of the teams where the student is in
     */
    @GetMapping("/{id}/teams")
    public List<TeamDTO> getTeams(@PathVariable String id,
                                  @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return teamService
                .getTeamsForStudent(id);
    }

    /**
     * Retrieve the students that confirm to join a team
     * @param id the student id
     * @param teamid the team id
     * @param userDetails the user who make the request
     * @return the list of students that confirm to join a team
     */
    @GetMapping("/{id}/teams/{teamid}/confirmed_members")
    public List<StudentDTO> getConfirmedMembers(@PathVariable String id,
                                                @PathVariable Long teamid,
                                                @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // check if the student is a member of the team
        try{
            if(!this.teamService.getMembers(teamid)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(student.get().getId())
            ){
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try{
            return this.teamService.getConfirmedMembersTeam(teamid);
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    /**
     * Retrieve the students that have not confirmed jet to join a team
     * @param id the student id
     * @param teamid the team id
     * @param userDetails the user who make the requst
     * @return the list of students that have not confirmed jet to join a team
     */
    @GetMapping("/{id}/teams/{teamid}/pendent_members")
    public List<StudentDTO> getPendentMembers(@PathVariable String id,
                                              @PathVariable Long teamid,
                                              @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // check if the student is a member of the team
        try{
            if(!this.teamService.getMembers(teamid)
                    .stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList())
                    .contains(student.get().getId())
            ){
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try{
            return this.teamService.getPendentMembersTeam(teamid);
        }
        catch (TeamNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    /**
     * Create a virtual machine
     * @param id the student id
     * @param teamid the team id
     * @param userDetails the user who make the request
     * @param virtualMachineDTO the virtual machine to create
     */
    @PostMapping("/{id}/teams/{teamid}/virtual_machines")
    @ResponseStatus(HttpStatus.CREATED)
    public void createVirtualMachine(@PathVariable String id,
                                     @PathVariable Long teamid,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @RequestBody VirtualMachineDTO virtualMachineDTO){

        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            studentService.createVirtualMachine(virtualMachineDTO, teamid, student.get().getId());
        }
        catch (TeamNotFoundException | StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Modify a virtual machine
     * @param id the student id
     * @param userDetails the user who make the request
     * @param virtualMachineDTO the virtual machine modified
     */
    @PutMapping("/{id}/teams/{teamid}/virtual_machines/{vmid}/modify")
    @ResponseStatus(HttpStatus.OK)
    public void modifyVirtualMachine(@PathVariable String id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @RequestBody VirtualMachineDTO virtualMachineDTO){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            studentService.changeVirtualMachineParameters(virtualMachineDTO, student.get().getId());
        }
        catch (StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Start a virtual machine
     * @param id the student id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     */
    @PutMapping("/{id}/teams/{teamid}/virtual_machines/{vmid}/start")
    @ResponseStatus(HttpStatus.OK)
    public void startVirtualMachine(@PathVariable String id,
                                    @PathVariable Long vmid,
                                    @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            studentService.startVirtualMachine(vmid, id);
        }
        catch (StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Stop a virtual machine
     * @param id the student id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     */
    @PutMapping("/{id}/teams/{teamid}/virtual_machines/{vmid}/stop")
    @ResponseStatus(HttpStatus.OK)
    public void stopVirtualMachine(@PathVariable String id,
                                   @PathVariable Long vmid,
                                   @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            studentService.stopVirtualMachine(vmid, id);
        }
        catch (StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Delete a virtual machine
     * @param id the student id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     */
    @DeleteMapping("/{id}/teams/{teamid}/virtual_machines/{vmid}/delete")
    @ResponseStatus(HttpStatus.OK)
    public void deleteVirtualMachine(@PathVariable String id,
                                     @PathVariable Long vmid,
                                     @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            studentService.deleteVirtualMachine(vmid, id);
        }
        catch (StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Add owners to a virtual machine
     * @param id the student id
     * @param vmid the virtual machine id
     * @param userDetails the user who make the request
     * @param newOwners the list of owners to add
     */
    @PutMapping("/{id}/teams/{teamid}/virtual_machines/{vmid}/add_owners")
    @ResponseStatus(HttpStatus.OK)
    public void addOwnersVirtualMachine(@PathVariable String id,
                                        @PathVariable Long vmid,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody List<String> newOwners){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            studentService.addVirtualMachineOwners(id, newOwners, vmid);
        }
        catch (StudentNotFoundExeption e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

    }

    /**
     * Upload a delivery
     * @param id the student id
     * @param homeworkid the homework id
     * @param userDetails the user who make the request
     * @param multipartFile the delivery to upload
     */
    @PostMapping("/{id}/{coursename}/{assignmentid}/{homeworkid}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadDelivery(@PathVariable String id,
                               @PathVariable Long homeworkid,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestBody MultipartFile multipartFile){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());
        Tika tika = new Tika();
        String mediaType;
        List<String> supportedMediaTypes = new ArrayList<>();

        supportedMediaTypes.add("image/png");

        // check media type of the file
        try {
            mediaType = tika.detect(multipartFile.getInputStream());
            if(supportedMediaTypes.stream().noneMatch(x -> x.equals(mediaType))){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
        }
        catch (IOException e){
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            studentService.uploadDelivery(homeworkid, multipartFile);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Retrieve the image of the assignment
     * @param id the student id
     * @param assignmentid the assignment id
     * @param userDetails the user who make the request
     * @return the image of the assignment
     */
    @GetMapping(value = "/{id}/{coursename}/{assignmentid}",
            produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public byte[] getAssignmentImage(@PathVariable String id,
                                     @PathVariable Long assignmentid,
                                     @AuthenticationPrincipal UserDetails userDetails){
        Optional<StudentDTO> student = generalService.getStudent(userDetails.getUsername());

        if(!student.isPresent()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, id);
        }

        // check if the student is the same of {id}
        if(!student.get().getId().equals(id)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try{
            return studentService.getAssignmentImage(assignmentid, id);
        }
        catch (StudentServiceException e){
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

}
