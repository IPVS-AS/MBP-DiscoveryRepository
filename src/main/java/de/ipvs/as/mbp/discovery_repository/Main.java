package de.ipvs.as.mbp.discovery_repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class of the discovery repository.
 */
@SpringBootApplication(scanBasePackages = {Main.BASE_PACKAGES})
public class Main {

    //Base package of the repository application
    public static final String BASE_PACKAGES = "de.ipvs.as.mbp.discovery_repository";

    /**
     * Main method for launching the discovery repository application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        //Execute the spring boot application
        SpringApplication.run(Main.class, args);
    }
}