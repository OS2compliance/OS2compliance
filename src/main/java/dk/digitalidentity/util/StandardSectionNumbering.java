package dk.digitalidentity.util;

import dk.digitalidentity.model.entity.StandardTemplateSection;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nummerering af krav under en gruppe i en standard.
 * <p>
 * Et krav har to numre der ikke maa udledes af hinanden: {@code identifier} er @Id og dermed
 * global for hele databasen, mens {@code section} kun skal vaere unikt inden for gruppen og bliver
 * skrevet om ved sletning og omsortering. Blev de udledt af samme vaerdi, slog en kollision med en
 * anden standard igennem som huller i det nummer brugeren ser.
 */
public final class StandardSectionNumbering {
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    private StandardSectionNumbering() {
    }

    /**
     * Naeste ledige nummer til visning i gruppen, baseret paa soeskendes {@code section} - aldrig
     * deres identifier.
     * <p>
     * Nye krav laegges bagerst (max + 1) og fylder bevidst ikke huller efter slettede krav; de
     * lukkes ved sletning eller naar brugeren selv flytter rundt med traek-og-slip.
     */
    public static String nextFreeDisplaySection(final String parentSection, final Collection<StandardTemplateSection> siblings) {
        // long, saa "max + 1" ikke kan trille rundt til et negativt nummer.
        long max = 0;
        for (StandardTemplateSection sibling : siblings) {
            final long number = trailingNumberOf(sibling.getSection());
            if (number > max) {
                max = number;
            }
        }
        return parentSection + "." + (max + 1);
    }

    /**
     * Sidste tal-segment af et sektionsnummer ("1.10" -&gt; 10). 0 for null og for et nummer uden
     * afsluttende tal, da gruppens nummer er brugerredigerbart og praefikses ind i kravets.
     * <p>
     * Et for stort tal klippes til {@link Integer#MAX_VALUE}, ikke til 0 - nulstilling ville lade
     * {@link #nextFreeDisplaySection} udstede 1 igen og dermed lave en dublet.
     */
    public static int trailingNumberOf(final String section) {
        if (section == null) {
            return 0;
        }
        final Matcher matcher = TRAILING_NUMBER.matcher(section.trim());
        if (!matcher.find()) {
            return 0;
        }
        final String digits = matcher.group(1);
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Sorteringsnoegle efter samme konkateneringsskema som resten af standard-modulet: foraelderens
     * cifre efterfulgt af kravets eget nummer ("1" + 10 -&gt; 110). Skemaet bevarer raekkefoelgen
     * inden for en gruppe, fordi alle soeskende deler praefiks. Falder tilbage til kravets eget
     * nummer hvis resultatet ikke kan rummes i en int - ellers ville "999.999.999" kaste.
     * <p>
     * Fallbacken afgoeres pr. kald, saa et praefiks lige under graensen kan give blandede noegler
     * og forkert orden ("21474836" + 47 passer, + 48 goer ikke). Kraever et ottecifret
     * gruppenummer; den rigtige rettelse er at goere {@code sortKey} til en long - kolonnen er
     * allerede bigint.
     */
    public static int sortKeyOf(final String parentSection, final int childNumber) {
        final String digits = parentSection == null ? "" : parentSection.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return childNumber;
        }
        try {
            final long key = Long.parseLong(digits + childNumber);
            return key > Integer.MAX_VALUE ? childNumber : (int) key;
        } catch (NumberFormatException e) {
            return childNumber;
        }
    }
}
