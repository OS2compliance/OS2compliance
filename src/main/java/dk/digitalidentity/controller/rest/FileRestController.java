package dk.digitalidentity.controller.rest;

import dk.digitalidentity.security.annotations.RequireAuthenticated;
import dk.digitalidentity.security.annotations.crud.RequireCreateOwnerOnly;
import dk.digitalidentity.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("rest/file")
@RequireAuthenticated
@RequiredArgsConstructor
public class FileRestController {
    private final S3Service s3Service;

    public record UploadDTO (MultipartFile upload ) {}
    public record CKEditorImageResponse(String url){}
    public record CKEditorImageError(String error){}
	@RequireCreateOwnerOnly
    @PostMapping(value = "img/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("upload") MultipartFile file) throws IOException {
        try {
            if (file != null) {
                String key = s3Service.upload(file.getOriginalFilename(), file.getBytes());
                return new ResponseEntity<>(new CKEditorImageResponse("/file/img?key="+key ), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new CKEditorImageError("No image file recieved"), HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            log.error("Could not upload image to s3", e);
            return new ResponseEntity<>(new CKEditorImageError(e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
