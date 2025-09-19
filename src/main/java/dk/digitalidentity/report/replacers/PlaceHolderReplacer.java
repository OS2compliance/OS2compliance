package dk.digitalidentity.report.replacers;

import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.StandardTemplate;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.Map;

public interface PlaceHolderReplacer {
    boolean supports(final PlaceHolder placeHolder);
    void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters, StandardTemplate template);
}
