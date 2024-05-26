package com.contact_reconciliation;

import com.contact_reconciliation.entity.Contact;
import com.contact_reconciliation.enums.LinkPrecedence;
import com.contact_reconciliation.repository.ContactRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.List;

@EnableWebMvc
@SpringBootApplication
public class ContactReconciliationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContactReconciliationApplication.class, args);
	}

	@Bean
	CommandLineRunner initContactRepository(ContactRepository contactRepository) {
		return args -> {
			contactRepository.saveAll(List.of(
					Contact.builder()
							.email("abc@gmail.com")
							.phoneNumber("12345")
							.linkPrecedence(LinkPrecedence.PRIMARY)
							.createdAt(LocalDateTime.now())
							.updatedAt(LocalDateTime.now())
							.build(),
					Contact.builder()
							.email("def@gmail.com")
							.phoneNumber("987654")
							.linkPrecedence(LinkPrecedence.PRIMARY)
							.createdAt(LocalDateTime.now())
							.updatedAt(LocalDateTime.now())
							.build()
			));
		};
	}
}
