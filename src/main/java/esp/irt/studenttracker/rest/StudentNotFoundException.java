package esp.irt.studenttracker.rest;

/**
 * Exception levée lorsqu'un étudiant demandé n'existe pas.
 */
public class StudentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StudentNotFoundException(String message) {
        super(message);
    }
}
