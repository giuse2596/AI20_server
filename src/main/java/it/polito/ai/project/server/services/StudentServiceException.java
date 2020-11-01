package it.polito.ai.project.server.services;

public class StudentServiceException extends RuntimeException{
    public StudentServiceException(String s) {
       super(s);
    }
    public StudentServiceException() {
    }
}
