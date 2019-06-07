package uk.ac.ebi.subs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.subs.accessionidimporter.AccessionIdImporterService;

@SpringBootApplication
public class AccessionIdImporterApplication implements CommandLineRunner {

	private AccessionIdImporterService accessionIdImporterService;

	public AccessionIdImporterApplication(AccessionIdImporterService accessionIdImporterService) {
		this.accessionIdImporterService = accessionIdImporterService;
	}

	public static void main(String[] args) {
		SpringApplication.run(AccessionIdImporterApplication.class, args);
	}

	@Override
	public void run(String... args) {
		accessionIdImporterService.importNotExistingAccessionIds();
	}
}
