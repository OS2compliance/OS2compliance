package dk.digitalidentity.report;

import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.report.replacers.PlaceHolderReplacer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DocxService {
    private final List<PlaceHolderReplacer> replacers;

    public DocxService(final List<PlaceHolderReplacer> replacers) {
        this.replacers = replacers;
    }

    public XWPFDocument readDocument(final String filename) throws IOException {
        return new XWPFDocument(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(filename)));
    }
    public void replacePlaceHolders(final XWPFDocument document) {
        Arrays.stream(PlaceHolder.values())
                .forEach(p -> replacers.stream()
                    .filter(r -> r.supports(p))
                    .forEach(r -> r.replace(p, document)));
    }

}
