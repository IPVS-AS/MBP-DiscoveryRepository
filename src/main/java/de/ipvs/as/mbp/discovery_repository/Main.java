package de.ipvs.as.mbp.discovery_repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class of the discovery repository.
 */
@SpringBootApplication(scanBasePackages = {"de.ipvs.as.mbp.discovery_repository"})
public class Main {

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