package it.polito.ai.project.server.services;

public class UserServiceException extends RuntimeException{

    public UserServiceException(){

    }

    public UserServiceException(String message){
        super(message);
    }

}
