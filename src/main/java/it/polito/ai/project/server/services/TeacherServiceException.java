package it.polito.ai.project.server.services;

public class TeacherServiceException extends RuntimeException{

    public TeacherServiceException(){

    }

    public TeacherServiceException(String message){
        super(message);
    }

}
