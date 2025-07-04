package dk.digitalidentity.service;

import dk.digitalidentity.kle_client.KLEClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Slf4j
@Service
public class KLEService {
	private final KLEClient kleClient;

}
