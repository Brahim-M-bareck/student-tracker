package esp.irt.studenttracker.rest;

import esp.irt.studenttracker.model.Student;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Retourne la liste des étudiants.
     *
     * <p>Version volontairement naïve de l'exercice 5.2 : la liste est recréée
     * à chaque appel. C'est une mauvaise pratique expliquée dans REPONSES.md.</p>
     *
     * @return les étudiants
     */
    @GetMapping("/students")
    public List<Student> getStudents() {
        List<Student> students = new ArrayList<>();
        students.add(new Student("Poornima", "Patel"));
        students.add(new Student("Mario", "Rossi"));
        students.add(new Student("Mary", "Smith"));
        return students;
    }
}
