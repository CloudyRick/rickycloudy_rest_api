package dev.rickcloudy.restapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = "dev.rickcloudy.restapi")
@EnableR2dbcRepositories(basePackages = "dev.rickcloudy.restapi.repository")
public class RestApiApplication {

    public static void main(String[] args) {
        String serverPortStr = System.getenv("SERVER_PORT");
        int serverPort = 8080;  // Default port

        if (serverPortStr != null) {
            try {
                serverPort = Integer.parseInt(serverPortStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid SERVER_PORT value, using default: 8080");
            }
        }

        System.out.println("Server port: " + serverPort);
        SpringApplication.run(RestApiApplication.class, args);
    }


}
