package dk.digitalidentity.util;

import dk.digitalidentity.model.entity.StandardTemplateSection;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StandardSectionNumberingTest {

    private static StandardTemplateSection child(final String identifier, final String section) {
        StandardTemplateSection templateSection = new StandardTemplateSection();
        templateSection.setIdentifier(identifier);
        templateSection.setSection(section);
        return templateSection;
    }

    @Test
    public void firstRequirementInAGroupIsNumberOne() {
        assertThat(StandardSectionNumbering.nextFreeDisplaySection("1", List.of())).isEqualTo("1.1");
    }

    @Test
    public void readsSectionNotIdentifier() {
        // Kernen i fejlen: kravene her har faaet identifier 1.5 og 1.10-1.13, fordi 1.1-1.4 og
        // 1.6-1.9 var optaget af en anden standard. Det viste nummer maa foelge section, ikke
        // identifieren, saa naeste krav bliver 1.6 - ikke 1.14.
        Collection<StandardTemplateSection> siblings = List.of(
                child("1.5", "1.1"),
                child("1.10", "1.2"),
                child("1.11", "1.3"),
                child("1.12", "1.4"),
                child("1.13", "1.5"));

        assertThat(StandardSectionNumbering.nextFreeDisplaySection("1", siblings)).isEqualTo("1.6");
    }

    @Test
    public void appendsAfterHighestNumberRatherThanFillingGaps() {
        Collection<StandardTemplateSection> siblings = List.of(
                child("0.1", "0.1"), child("0.2", "0.2"), child("0.3", "0.3"),
                child("0.4", "0.4"), child("0.6", "0.6"), child("0.7", "0.7"),
                child("0.8", "0.8"), child("0.9", "0.9"));

        assertThat(StandardSectionNumbering.nextFreeDisplaySection("0", siblings)).isEqualTo("0.10");
    }

    @Test
    public void toleratesMissingAndNonNumericSections() {
        Collection<StandardTemplateSection> siblings = List.of(
                child("2.1", null),
                child("2.2", "uden nummer"),
                child("2.3", "2.4"));

        assertThat(StandardSectionNumbering.nextFreeDisplaySection("2", siblings)).isEqualTo("2.5");
    }

    @Test
    public void trailingNumberReadsLastSegment() {
        assertThat(StandardSectionNumbering.trailingNumberOf("1.10")).isEqualTo(10);
        assertThat(StandardSectionNumbering.trailingNumberOf("3.1.1.12")).isEqualTo(12);
        assertThat(StandardSectionNumbering.trailingNumberOf(" 0.5 ")).isEqualTo(5);
        assertThat(StandardSectionNumbering.trailingNumberOf(null)).isZero();
        assertThat(StandardSectionNumbering.trailingNumberOf("ingen tal")).isZero();
    }

    @Test
    public void sortKeyKeepsSiblingsInNumericOrder() {
        int one = StandardSectionNumbering.sortKeyOf("1", 1);
        int two = StandardSectionNumbering.sortKeyOf("1", 2);
        int nine = StandardSectionNumbering.sortKeyOf("1", 9);
        int ten = StandardSectionNumbering.sortKeyOf("1", 10);
        int eleven = StandardSectionNumbering.sortKeyOf("1", 11);

        assertThat(one).isLessThan(two);
        assertThat(two).isLessThan(nine);
        assertThat(nine).isLessThan(ten);
        assertThat(ten).isLessThan(eleven);
    }

    @Test
    public void sortKeyMatchesTheExistingConcatenationScheme() {
        assertThat(StandardSectionNumbering.sortKeyOf("1", 10)).isEqualTo(110);
        assertThat(StandardSectionNumbering.sortKeyOf("0", 5)).isEqualTo(5);
        assertThat(StandardSectionNumbering.sortKeyOf("3.1.1", 2)).isEqualTo(3112);
    }

    @Test
    public void sortKeyFallsBackWhenConcatenationOverflowsAnInt() {
        // "999999999" + "1" kan ikke rummes i en int. Fallbacken sorterer stadig soeskende
        // korrekt indbyrdes, fordi de deler praefiks.
        assertThat(StandardSectionNumbering.sortKeyOf("999.999.999", 1)).isEqualTo(1);
        assertThat(StandardSectionNumbering.sortKeyOf("999.999.999", 2)).isEqualTo(2);
    }

    @Test
    public void sortKeyHandlesAParentWithoutDigits() {
        assertThat(StandardSectionNumbering.sortKeyOf(null, 3)).isEqualTo(3);
        assertThat(StandardSectionNumbering.sortKeyOf("bilag", 3)).isEqualTo(3);
    }

    @Test
    public void anOverflowingSectionNumberDoesNotProduceADuplicate() {
        // Et sektionsnummer for stort til en int maa ikke laese som 0 - saa ville naeste nummer
        // blive 1 og kollidere med et eksisterende krav i gruppen.
        assertThat(StandardSectionNumbering.trailingNumberOf("1.99999999999")).isEqualTo(Integer.MAX_VALUE);

        Collection<StandardTemplateSection> siblings = List.of(
                child("1.1", "1.1"),
                child("1.2", "1.99999999999"));

        assertThat(StandardSectionNumbering.nextFreeDisplaySection("1", siblings)).isNotEqualTo("1.1");
    }

    @Test
    public void nextNumberNeverWrapsToNegative() {
        Collection<StandardTemplateSection> siblings = List.of(child("1.1", "1." + Integer.MAX_VALUE));

        assertThat(StandardSectionNumbering.nextFreeDisplaySection("1", siblings))
                .isEqualTo("1." + (Integer.MAX_VALUE + 1L));
    }
}
