package esp.irt.studenttracker.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestionnaire global des exceptions de l'API REST.
 *
 * <p>Centralise le format des erreurs pour tous les contrôleurs.</p>
 */
@ControllerAdvice
public class StudentRestExceptionHandler {

    /**
     * Transforme une StudentNotFoundException en réponse HTTP 404.
     *
     * @param exc exception métier
     * @return une réponse JSON avec le statut 404
     */
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<StudentErrorResponse> handleStudentNotFoundException(StudentNotFoundException exc) {
        StudentErrorResponse error = new StudentErrorResponse();
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Filet de sécurité pour les autres exceptions (ici 400).
     *
     * @param exc exception non gérée
     * @return une réponse JSON avec le statut 400
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StudentErrorResponse> handleException(Exception exc) {
        StudentErrorResponse error = new StudentErrorResponse();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
