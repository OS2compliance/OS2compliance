package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.enums.RiskScaleType;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Transactional
@Service
public class ScaleService {
	@Autowired
	SettingsService settingsService;
    private static final List<ScaleExplainers> EXPLAINERS = List.of(
        ScaleExplainers.builder()
            .type(RiskScaleType.SCALE_1_4)
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
        ScaleExplainers.builder()
            .type(RiskScaleType.SCALE_1_4_KL)
            .riskScore(List.of(
                "Risikoscore = sandsynlighed * konsekvens",
                "1-3 = Lav risiko (grøn)",
                "4-6 = Under middel risiko (lysegrøn)",
                "7-11 = Middel risiko (gul)",
                "12-14 = Ovr middel risiko (orange)",
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
            .build(),
        ScaleExplainers.builder()
            .type(RiskScaleType.SCALE_1_10)
            .riskScore(Collections.emptyList())
            .probabilityScore(Collections.emptyList())
            .consequenceNumber(Collections.emptyList())
            .build()
    );
    @Builder
    @Getter
    public static class ScaleExplainers {
        RiskScaleType type;
        List<String> riskScore;
        List<String> probabilityScore;
        List<String> consequenceNumber;
    }


    public RiskScaleType getScaleType() {
        return switchScale(getScaleTypeString());
    }

	public Map<Integer, String> getScale() {
		return getScaleType().getValue();
	}

    public Map<String, String> getScaleRiskScoreColorMap() {
        return getRiskScoreColorMap(getScaleType());
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

    private static String explainerWithNewLines(final RiskScaleType riskScaleType, final String heading, final Function<ScaleExplainers, List<String>> valueSupplier) {
        final StringBuilder builder = new StringBuilder();
        builder.append(heading).append('\n');
        final List<String> lines = valueSupplier.apply(scaleExplainerFor(riskScaleType));
        lines.forEach(l -> builder.append(l).append('\n'));
        return builder.toString();
    }

    private Map<String, String> getRiskScoreColorMap(final RiskScaleType riskScaleType) {
        final Map<String, String> map = new HashMap<>();
        switch (riskScaleType) {
            case SCALE_1_4 -> {
                map.put("1,1", "#87AD27");
                map.put("1,2", "#87AD27");
                map.put("1,3", "#87AD27");
                map.put("1,4", "#FFDE07");
                map.put("2,1", "#87AD27");
                map.put("2,2", "#87AD27");
                map.put("2,3", "#FFDE07");
                map.put("2,4", "#FFDE07");
                map.put("3,1", "#87AD27");
                map.put("3,2", "#FFDE07");
                map.put("3,3", "#FFDE07");
                map.put("3,4", "#DF5645");
                map.put("4,1", "#FFDE07");
                map.put("4,2", "#FFDE07");
                map.put("4,3", "#DF5645");
                map.put("4,4", "#DF5645");
                return map;
            }
            case SCALE_1_4_KL -> {
                map.put("1,1", "#1DB255");
                map.put("1,2", "#1DB255");
                map.put("1,3", "#1DB255");
                map.put("1,4", "#93D259");
                map.put("2,1", "#1DB255");
                map.put("2,2", "#93D259");
                map.put("2,3", "#93D259");
                map.put("2,4", "#FDFF3C");
                map.put("3,1", "#1DB255");
                map.put("3,2", "#93D259");
                map.put("3,3", "#FDFF3C");
                map.put("3,4", "#FCC231");
                map.put("4,1", "#93D259");
                map.put("4,2", "#FDFF3C");
                map.put("4,3", "#FCC231");
                map.put("4,4", "#FA0020");
                return map;
            }
            case SCALE_1_10 -> {
                // TODO farver for denne
                return map;
            }
        }
        return map;
    }

    public static ScaleExplainers scaleExplainerFor(final RiskScaleType type) {
        return EXPLAINERS.stream().filter(e -> e.type == type).findFirst().orElseThrow();
    }

    public RiskScaleType switchScale(final String s) {
        try {
            return RiskScaleType.valueOf(s);
        } catch (final IllegalArgumentException ex) {
            if(isVariantOf(s, "{4=RØD, 3=GUL, 2=GUL, 1=GRØN}")) {
                return RiskScaleType.SCALE_1_4;
            }
            if(isVariantOf(s, "{1=GRØN, 2=GRØN, 3=GRØN, 4=GUL, 5=GUL, 6=GUL, 7=GUL, 8=RØD, 9=RØD, 10=RØD}")) {
                return RiskScaleType.SCALE_1_10;
            }
            return RiskScaleType.SCALE_1_4;
        }
	}

    private boolean isVariantOf(final String s1, final String s2) {
        return StringUtils.getDigits(s1).length() == StringUtils.getDigits(s2).length();
    }
}
