package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.model.entity.enums.RiskScaleType;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Transactional
@Service
public class ScaleService {
	@Autowired
	SettingsService settingsService;
    private static final List<ScaleSetting> SCALE_SETTINGS = List.of(
        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4)
            .colorsMatrix(
                new HashMap<>() {{
                    put("1,1", "#87AD27");
                    put("1,2", "#87AD27");
                    put("1,3", "#87AD27");
                    put("1,4", "#FFDE07");
                    put("2,1", "#87AD27");
                    put("2,2", "#87AD27");
                    put("2,3", "#FFDE07");
                    put("2,4", "#FFDE07");
                    put("3,1", "#87AD27");
                    put("3,2", "#FFDE07");
                    put("3,3", "#FFDE07");
                    put("3,4", "#DF5645");
                    put("4,1", "#FFDE07");
                    put("4,2", "#FFDE07");
                    put("4,3", "#DF5645");
                    put("4,4", "#DF5645");
                }}
            )
            .assessmentLookup(
                r -> {
                    if (r == null) {
                        return null;
                    }
                    if (r <= 4) {
                        return RiskAssessment.GREEN;
                    } else if (r <= 11) {
                        return RiskAssessment.YELLOW;
                    } else {
                        return RiskAssessment.RED;
                    }
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
            .build(),

        ScaleSetting.builder()
            .type(RiskScaleType.SCALE_1_4_KL)
            .colorsMatrix(
                    new HashMap<>() {{
                        put("1,1", "#1DB255");
                        put("1,2", "#1DB255");
                        put("1,3", "#1DB255");
                        put("1,4", "#93D259");
                        put("2,1", "#1DB255");
                        put("2,2", "#93D259");
                        put("2,3", "#93D259");
                        put("2,4", "#FDFF3C");
                        put("3,1", "#1DB255");
                        put("3,2", "#93D259");
                        put("3,3", "#FDFF3C");
                        put("3,4", "#FCC231");
                        put("4,1", "#93D259");
                        put("4,2", "#FDFF3C");
                        put("4,3", "#FCC231");
                        put("4,4", "#FA0020");
                    }}
            )
            .assessmentLookup(
                r -> {
                    if (r == null) {
                        return null;
                    }
                    if (r <= 3) {
                        return RiskAssessment.GREEN;
                    } else if (r <= 6) {
                        return RiskAssessment.LIGHT_GREEN;
                    } else if (r <= 11) {
                        return RiskAssessment.YELLOW;
                    } else if (r <= 14) {
                        return RiskAssessment.ORANGE;
                    } else {
                        return RiskAssessment.RED;
                    }
                }
            )
            .assessmentColorLookup(
                a -> switch (a) {
                    case RED -> "FA0020";
                    case ORANGE -> null;
                    case GREEN -> null;
                    case LIGHT_GREEN -> null;
                    case YELLOW -> null;
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
            .build()
    );
    @Builder
    @Getter
    public static class ScaleSetting {
        RiskScaleType type;
        List<String> riskScore;
        List<String> probabilityScore;
        List<String> consequenceNumber;
        Map<String, String> colorsMatrix;
        Function<Integer, RiskAssessment> assessmentLookup;
        Function<RiskAssessment, String> assessmentColorLookup;
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

	public Map<Integer, String> getScale() {
		return getScaleType().getValue();
	}

    public RiskAssessment getRiskAssessmentForRisk(final int score) {
        return scaleSettingsForType(getScaleType()).assessmentLookup.apply(score);
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

    private boolean isVariantOf(final String s1, final String s2) {
        return StringUtils.getDigits(s1).length() == StringUtils.getDigits(s2).length();
    }
}
