package it.polito.ai.project.server.services;

public class GeneralServiceException extends RuntimeException{

    public GeneralServiceException(){

    }

    public GeneralServiceException(String message){
        super(message);
    }

}
