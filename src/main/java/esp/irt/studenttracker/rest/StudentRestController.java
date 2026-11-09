package esp.irt.studenttracker.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
