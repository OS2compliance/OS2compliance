package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.annotations.RequireAuthenticated;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;

@RequireAuthenticated
@Slf4j
@Controller
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {
    private final S3Service s3Service;

	@RequireReadOwnerOnly
    @GetMapping(value = "img")
    public @ResponseBody ResponseEntity <ByteArrayResource> getImage (@RequestParam String key) throws java.io.IOException {
        try (InputStream inputStream = s3Service.downloadAsStream(key)) {
            ByteArrayResource resource = new ByteArrayResource(inputStream.readAllBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; "+key);
            headers.add("Cache-Control", "private, max-age=86400");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .headers(headers)
                .body(resource);
        }
    }
}
