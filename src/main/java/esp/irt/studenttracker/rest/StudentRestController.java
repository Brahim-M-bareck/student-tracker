package esp.irt.studenttracker.rest;

import esp.irt.studenttracker.model.Student;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur REST de l'application Student Tracker.
 */
@RestController
@RequestMapping("/api")
public class StudentRestController {

    /** Liste chargée une seule fois au démarrage du contrôleur. */
    private List<Student> theStudents;

    /**
     * Initialise la liste après l'injection des dépendances.
     * La méthode n'est appelée qu'une fois car le contrôleur est un singleton.
     */
    @PostConstruct
    public void loadData() {
        theStudents = new ArrayList<>();
        theStudents.add(new Student("Poornima", "Patel"));
        theStudents.add(new Student("Mario", "Rossi"));
        theStudents.add(new Student("Mary", "Smith"));
    }

    /**
     * Premier endpoint de test.
     *
     * @return un message de bienvenue
     */
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World!";
    }

    /**
     * Retourne la liste des étudiants chargée au démarrage.
     *
     * @return les étudiants
     */
    @GetMapping("/students")
    public List<Student> getStudents() {
        return theStudents;
    }

    /**
     * Retourne un étudiant à partir de son index dans la liste.
     *
     * @param studentId index de l'étudiant
     * @return l'étudiant correspondant
     */
    @GetMapping("/students/{studentId}")
    public Student getStudent(@PathVariable int studentId) {
        return theStudents.get(studentId);
    }
}
