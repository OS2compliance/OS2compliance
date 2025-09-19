package dk.digitalidentity.report.replacers;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.PlaceHolder;
import dk.digitalidentity.model.entity.StandardTemplate;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static dk.digitalidentity.Constants.DK_DATE_FORMATTER;
import static dk.digitalidentity.model.PlaceHolder.DATE;
import static dk.digitalidentity.model.PlaceHolder.MUNICIPAL_NAME;

@Component
public class CommonPropertiesReplacer implements PlaceHolderReplacer {
    private final OS2complianceConfiguration configuration;

    public CommonPropertiesReplacer(final OS2complianceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean supports(final PlaceHolder placeHolder) {
        return placeHolder == DATE || placeHolder == MUNICIPAL_NAME;
    }

    @Override
    public void replace(final PlaceHolder placeHolder, final XWPFDocument document, final Map<String, String> parameters, StandardTemplate template) {
        document.getBodyElementsIterator().forEachRemaining(
                part -> {
                    if (part.getBody() != null) {
                        part.getBody().getParagraphs().forEach(pg -> replaceParagraph(pg, placeHolder.getPlaceHolder(), lookupPlaceHolderValue(placeHolder)));
                    }
                }
        );
    }

    private void replaceParagraph(final XWPFParagraph p, final String placeHolder, final String value) {
        p.getRuns().forEach(r -> replaceRun(r, placeHolder, value));
    }

    private void replaceRun(final XWPFRun run, final String placeHolder, final String value) {
        final String text = run.getText(0);
        final String replaceWith = ObjectUtils.defaultIfNull(value, "");
        if (text != null && text.contains(placeHolder)) {
            run.setText(text.replace(placeHolder, replaceWith), 0);
        }
    }

    public String lookupPlaceHolderValue(final PlaceHolder placeHolder) {
        return switch (placeHolder) {
            case DATE -> DK_DATE_FORMATTER.format(LocalDate.now());
            case MUNICIPAL_NAME -> configuration.getMunicipal().getName();
            default -> placeHolder.getPlaceHolder();
        };
    }
}
