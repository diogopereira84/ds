package fox.fmc.partner.delivery.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FmcPartnerDeliveryTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(FmcPartnerDeliveryTestApplication.class, args);
	}

	/**
	 * Registers ObjectMapper as a Spring Bean.
	 * This fixes the UnsatisfiedDependencyException in AssetImmutabilityVerifier.
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
				.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}
}