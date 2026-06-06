package dev.vivim.filecloud.exception.handler;

import dev.vivim.filecloud.dto.ErrorResponse;
import dev.vivim.filecloud.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@RestControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(UserExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserExists(UserExistsException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthorizationError(AuthorizationException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(NoSuchKeyException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoSuchS3Key(NoSuchKeyException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(FileAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFileAlreadyExists(FileAlreadyExistsException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(InvalidPathException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPath(InvalidPathException e) { return new ErrorResponse(e.getMessage()); }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return new ErrorResponse("Validation failed! " + e.getMessage());
    }

}
