package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.RiskScaleType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.digitalidentity.Constants.SCALE_COLOR_GREEN;
import static dk.digitalidentity.Constants.SCALE_COLOR_LIGHT_GREEN;
import static dk.digitalidentity.Constants.SCALE_COLOR_ORANGE;
import static dk.digitalidentity.Constants.SCALE_COLOR_RED;
import static dk.digitalidentity.Constants.SCALE_COLOR_STD_GREEN;
import static dk.digitalidentity.Constants.SCALE_COLOR_STD_RED;
import static dk.digitalidentity.Constants.SCALE_COLOR_STD_YELLOW;
import static dk.digitalidentity.Constants.SCALE_COLOR_YELLOW;

@Transactional
@Service
@RequiredArgsConstructor
public class ScaleService {
	private final SettingsService settingsService;
    private static final List<ScaleSetting> SCALE_SETTINGS = List.of(
        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4)
            .colorsMatrix(
                new HashMap<>() {{
                    put("1,1", SCALE_COLOR_STD_GREEN);
                    put("1,2", SCALE_COLOR_STD_GREEN);
                    put("1,3", SCALE_COLOR_STD_GREEN);
                    put("1,4", SCALE_COLOR_STD_YELLOW);
                    put("2,1", SCALE_COLOR_STD_GREEN);
                    put("2,2", SCALE_COLOR_STD_GREEN);
                    put("2,3", SCALE_COLOR_STD_YELLOW);
                    put("2,4", SCALE_COLOR_STD_YELLOW);
                    put("3,1", SCALE_COLOR_STD_GREEN);
                    put("3,2", SCALE_COLOR_STD_YELLOW);
                    put("3,3", SCALE_COLOR_STD_YELLOW);
                    put("3,4", SCALE_COLOR_STD_RED);
                    put("4,1", SCALE_COLOR_STD_YELLOW);
                    put("4,2", SCALE_COLOR_STD_YELLOW);
                    put("4,3", SCALE_COLOR_STD_RED);
                    put("4,4", SCALE_COLOR_STD_RED);
                }}
            )
            .assessmentLookup(
                (p, c) -> {
                    if (c == null || p == null) {
                        return null;
                    }
                    final String lookup = "" + c + p;
                    return switch (lookup) {
                        case "11", "12", "13", "21", "22", "31" -> RiskAssessment.GREEN;
                        case "14", "23", "24", "32", "33", "41", "42" -> RiskAssessment.YELLOW;
                        case "34", "43", "44" -> RiskAssessment.RED;
                        default -> throw new IllegalStateException("Unexpected value: " + lookup);
                    };
                }
            )
            .riskScore(List.of(
                "Risikoscore = sandsynlighed * konsekvens",
                "1-4 = Lav risiko (grøn)",
                "5-11 = Middel risiko (gul)",
                "12-16 = Høj risiko (rød)"
            ))
            .probabilityScore(List.of(
                "1 = Usandsynligt",
                "2 = Mindre sandsynligt",
                "3 = Sandsynligt",
                "4 = Forventet"
            ))
            .consequenceNumber(List.of(
                "1 = Ubetydelig",
                "2 = Mindre alvorlig",
                "3 = Meget alvorlig",
                "4 = Graverende/ødelæggende"
            ))
            .consequenceScale(Map.of(1, "GRØN", 2, "GUL", 3, "ORANGE", 4, "RØD"))
            .build(),
        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4_KL)
            .colorsMatrix(
                    new HashMap<>() {{
                        put("1,1", SCALE_COLOR_GREEN);
                        put("1,2", SCALE_COLOR_GREEN);
                        put("1,3", SCALE_COLOR_GREEN);
                        put("1,4", SCALE_COLOR_LIGHT_GREEN);
                        put("2,1", SCALE_COLOR_GREEN);
                        put("2,2", SCALE_COLOR_LIGHT_GREEN);
                        put("2,3", SCALE_COLOR_LIGHT_GREEN);
                        put("2,4", SCALE_COLOR_YELLOW);
                        put("3,1", SCALE_COLOR_GREEN);
                        put("3,2", SCALE_COLOR_LIGHT_GREEN);
                        put("3,3", SCALE_COLOR_YELLOW);
                        put("3,4", SCALE_COLOR_ORANGE);
                        put("4,1", SCALE_COLOR_LIGHT_GREEN);
                        put("4,2", SCALE_COLOR_YELLOW);
                        put("4,3", SCALE_COLOR_ORANGE);
                        put("4,4", SCALE_COLOR_RED);
                    }}
            )
            .assessmentLookup(
                (p, c) -> {
                    if (c == null || p == null) {
                        return null;
                    }
                    final String lookup = "" + c + p;
                    return switch (lookup) {
                        case "11", "12", "13", "21", "31" -> RiskAssessment.GREEN;
                        case "14", "22", "23", "32", "41" -> RiskAssessment.LIGHT_GREEN;
                        case "24", "33", "42" -> RiskAssessment.YELLOW;
                        case "34", "43" -> RiskAssessment.ORANGE;
                        case "44" -> RiskAssessment.RED;
                        case "00" -> null;
                        default -> throw new IllegalStateException("Unexpected value: " + lookup);
                    };
                }
            )
            .riskScore(List.of(
                "Risikoscore = sandsynlighed * konsekvens",
                "1-3 = Lav risiko (grøn)",
                "4-6 = Under middel risiko (lysegrøn)",
                "7-11 = Middel risiko (gul)",
                "12-14 = Over middel risiko (orange)",
                "15-16 = Høj risiko (rød)"
            ))
            .probabilityScore(List.of(
                "1 = Usandsynligt",
                "2 = Mindre sandsynligt",
                "3 = Sandsynligt",
                "4 = Forventet"
            ))
            .consequenceNumber(List.of(
                "1 = Lav",
                "2 = Medium",
                "3 = Høj",
                "4 = Meget høj"
            ))
            .consequenceScale(Map.of(1, "GRØN", 2, "GUL", 3, "ORANGE", 4, "RØD"))
            .build(),
        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4_HERNING)
            .colorsMatrix(
                    new HashMap<>() {{
                        put("1,1", SCALE_COLOR_GREEN);
                        put("1,2", SCALE_COLOR_GREEN);
                        put("1,3", SCALE_COLOR_LIGHT_GREEN);
                        put("1,4", SCALE_COLOR_YELLOW);
                        put("2,1", SCALE_COLOR_GREEN);
                        put("2,2", SCALE_COLOR_LIGHT_GREEN);
                        put("2,3", SCALE_COLOR_YELLOW);
                        put("2,4", SCALE_COLOR_ORANGE);
                        put("3,1", SCALE_COLOR_LIGHT_GREEN);
                        put("3,2", SCALE_COLOR_YELLOW);
                        put("3,3", SCALE_COLOR_ORANGE);
                        put("3,4", SCALE_COLOR_RED);
                        put("4,1", SCALE_COLOR_YELLOW);
                        put("4,2", SCALE_COLOR_ORANGE);
                        put("4,3", SCALE_COLOR_RED);
                        put("4,4", SCALE_COLOR_RED);
                    }}
            )
            .assessmentLookup(
                (p, c) -> {
                    if (c == null || p == null) {
                        return null;
                    }
                    final String lookup = "" + c + p;
                    return switch (lookup) {
                        case "11", "12", "21" -> RiskAssessment.GREEN;
                        case "13", "31", "22" -> RiskAssessment.LIGHT_GREEN;
                        case "14", "23", "32", "41" -> RiskAssessment.YELLOW;
                        case "24", "33", "42" -> RiskAssessment.ORANGE;
                        case "34", "43", "44" -> RiskAssessment.RED;
                        case "00" -> null;
                        default -> throw new IllegalStateException("Unexpected value: " + lookup);
                    };
                }
            )
            .riskScore(List.of(
                "Risikoscore = sandsynlighed * konsekvens",
                "1-2 = Lav (grøn)",
                "3-7 = Under middel (gul)",
                "8-11 = Over middel (orange)",
                "12-16 = Høj (rød)"
            ))
            .probabilityScore(List.of(
                "1 = Usandsynligt",
                "2 = Mindre sandsynligt",
                "3 = Sandsynligt",
                "4 = Forventet"
            ))
            .consequenceNumber(List.of(
                "1 = Ubetydelig",
                "2 = Mindre alvorlig",
                "3 = Meget alvorlig",
                "4 = Ødelæggende"
            ))
            .consequenceScale(Map.of(1, "GRØN", 2, "GUL", 3, "ORANGE", 4, "RØD"))
            .build(),

        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4_DATATILSYNET)
            .colorsMatrix(
                new HashMap<>() {{
                    // consequence, probability
                    put("1,1", SCALE_COLOR_LIGHT_GREEN);
                    put("1,2", SCALE_COLOR_LIGHT_GREEN);
                    put("1,3", SCALE_COLOR_YELLOW);
                    put("1,4", SCALE_COLOR_ORANGE);
                    put("2,1", SCALE_COLOR_LIGHT_GREEN);
                    put("2,2", SCALE_COLOR_YELLOW);
                    put("2,3", SCALE_COLOR_ORANGE);
                    put("2,4", SCALE_COLOR_ORANGE);
                    put("3,1", SCALE_COLOR_YELLOW);
                    put("3,2", SCALE_COLOR_ORANGE);
                    put("3,3", SCALE_COLOR_ORANGE);
                    put("3,4", SCALE_COLOR_RED);
                    put("4,1", SCALE_COLOR_ORANGE);
                    put("4,2", SCALE_COLOR_RED);
                    put("4,3", SCALE_COLOR_RED);
                    put("4,4", SCALE_COLOR_RED);
                }}
            )
            .assessmentLookup(
                (p, c) -> {
                    if (c == null || p == null) {
                        return null;
                    }
                    final String lookup = "" + c + p;
                    return switch (lookup) {
                        case "11", "12", "21" -> RiskAssessment.GREEN;
                        case "13", "22", "31" -> RiskAssessment.YELLOW;
                        case "14", "41", "23", "24", "32", "33" -> RiskAssessment.ORANGE;
                        case "34", "42", "43", "44" -> RiskAssessment.RED;
                        case "00" -> null;
                        default -> throw new IllegalStateException("Unexpected value: " + lookup);
                    };
                }
            )
            .riskScore(List.of(
                "Risikoscore = sandsynlighed * konsekvens",
                "1-2 = Lav (grøn)",
                "3-7 = Under middel (gul)",
                "8-11 = Over middel (orange)",
                "12-16 = Høj (rød)"
            ))
            .probabilityScore(List.of(
                "1 = Lav (Usandsynligt)",
                "2 = Medium (Mindre sandsynligt)",
                "3 = Høj (Sandsynligt)",
                "4 = Meget høj (Forventet)"
            ))
            .consequenceNumber(List.of(
                "1 = Lav (Ubetydelig, uvæsentlig)",
                "2 = Medium (Mindre alvorlig, generende)",
                "3 = Høj (Alvorlig, kritisk)",
                "4 = Meget høj (Graverende, meget kritisk)"
            ))
            .consequenceScale(Map.of(1, "GRØN", 2, "GUL", 3, "ORANGE", 4, "RØD"))
            .build()
    );
    @Builder
    @Getter
    public static class ScaleSetting {
        RiskScaleType type;
        List<String> riskScore;
        List<String> probabilityScore;
        List<String> consequenceNumber;
        Map<Integer, String> consequenceScale;
        Map<String, String> colorsMatrix;
        BiFunction<Integer, Integer, RiskAssessment> assessmentLookup;
    }

    public Map<Integer, String> getConsequenceNumberDescriptions() {
        final List<String> consequenceNumber = scaleSettingsForType(getScaleType()).consequenceNumber;
        return IntStream.range(0, consequenceNumber.size())
            .boxed()
            .collect(Collectors.toMap(idx -> idx, consequenceNumber::get));
    }

    public RiskScaleType getScaleType() {
        return getCurrentScaleType(getScaleTypeString());
    }

	public Map<Integer, String> getConsequenceScale() {
        return scaleSettingsForType(getScaleType()).consequenceScale;
	}

    public RiskAssessment getRiskAssessmentForRisk(final int probability, final int consequence) {
        return scaleSettingsForType(getScaleType()).assessmentLookup.apply(probability, consequence);
    }

    public Map<String, String> getScaleRiskScoreColorMap() {
        return scaleSettingsForType(getScaleType()).colorsMatrix;
    }

    public String getScaleProbabilityNumberExplainer() {
        return getProbabilityNumberExplainer(getScaleType());
    }

    public String getScaleConsequenceNumberExplainer() {
        return getConsequenceNumberExplainer(getScaleType());
    }

    public String getScaleRiskScoreExplainer() {
        return getRiskScoreExplainer(getScaleType());
    }

    private String getScaleTypeString() {
        return settingsService.getString("scale", RiskScaleType.SCALE_1_4.name());
    }

    private String getRiskScoreExplainer(final RiskScaleType riskScaleType) {
        return explainerWithNewLines(riskScaleType, "Risikoscore skala", s -> s.riskScore);
    }

    private String getProbabilityNumberExplainer(final RiskScaleType riskScaleType) {
        return explainerWithNewLines(riskScaleType, "Sandsynlighed skala", s -> s.probabilityScore);
    }
    private String getConsequenceNumberExplainer(final RiskScaleType riskScaleType) {
        return explainerWithNewLines(riskScaleType, "Konsekvens skala", s -> s.consequenceNumber);
    }

    private static String explainerWithNewLines(final RiskScaleType riskScaleType, final String heading, final Function<ScaleSetting, List<String>> valueSupplier) {
        final StringBuilder builder = new StringBuilder();
        builder.append(heading).append('\n');
        final List<String> lines = valueSupplier.apply(scaleSettingsForType(riskScaleType));
        lines.forEach(l -> builder.append(l).append('\n'));
        return builder.toString();
    }

    public static ScaleSetting scaleSettingsForType(final RiskScaleType type) {
        return SCALE_SETTINGS.stream().filter(e -> e.type == type).findFirst().orElseThrow();
    }

    public RiskScaleType getCurrentScaleType(final String s) {
        try {
            return RiskScaleType.valueOf(s);
        } catch (final IllegalArgumentException ex) {
            return RiskScaleType.SCALE_1_4;
        }
	}

}
