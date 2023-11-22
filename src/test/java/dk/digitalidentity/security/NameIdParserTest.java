package dk.digitalidentity.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NameIdParserTest {

    @Test
    public void canParseUuid() {
        assertThat(NameIdParser.parseNameId("51323e8c-b1a9-bd45-af57-f5c202cca97b"))
            .isPresent().contains("51323e8c-b1a9-bd45-af57-f5c202cca97b");
    }

    @Test
    public void canParseX509Format() {
        assertThat(NameIdParser.parseNameId("C=DK,O=123456,CN=Et Navn.,Serial=51323e8c-b1a9-bd45-af57-f5c202cca97b"))
            .isPresent().contains("51323e8c-b1a9-bd45-af57-f5c202cca97b");
    }

    @Test
    public void canParseBase64ObjectGuid() {
        assertThat(NameIdParser.parseNameId("UTI+jLGpvUWvV/XCAsypew=="))
            .isPresent().contains("51323e8c-b1a9-bd45-af57-f5c202cca97b");
    }

}
