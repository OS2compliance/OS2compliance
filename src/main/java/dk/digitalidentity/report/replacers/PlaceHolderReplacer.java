package dk.digitalidentity.report.replacers;

import dk.digitalidentity.model.PlaceHolder;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public interface PlaceHolderReplacer {
    boolean supports(final PlaceHolder placeHolder);
    void replace(final PlaceHolder placeHolder, final XWPFDocument document);
}
