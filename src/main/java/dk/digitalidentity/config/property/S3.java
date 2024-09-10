package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S3 {
    private String bucketName;
    private String folderName;
}
