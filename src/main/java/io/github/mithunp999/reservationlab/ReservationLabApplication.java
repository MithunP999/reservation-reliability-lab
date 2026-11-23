package io.github.mithunp999.reservationlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReservationLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationLabApplication.class, args);
	}

}
