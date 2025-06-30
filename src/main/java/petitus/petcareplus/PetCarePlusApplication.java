package petitus.petcareplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PetCarePlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetCarePlusApplication.class, args);
    }

}
