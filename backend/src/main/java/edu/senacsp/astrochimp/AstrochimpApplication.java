package edu.senacsp.astrochimp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication ativa a configuração automática e a busca de controllers, services e outras configurações neste pacote e nos subpacotes.
@SpringBootApplication
public class AstrochimpApplication {

	public static void main(String[] args) {
		SpringApplication.run(AstrochimpApplication.class, args);
	}

}
