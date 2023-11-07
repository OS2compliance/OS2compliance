package dk.digitalidentity.service;

import dk.digitalidentity.model.entity.enums.RiskScaleType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Transactional
@Service
public class ScaleService {
	@Autowired
	SettingsService settingsService;

	public String getScaleType() {

		return settingsService.getString("scale","scale-1-4");
	}

	public String getColor(final int scaleValue){
		return switchScale(getScaleType()).getValue().get(scaleValue);
	}

	public Map<Integer, String> getScale() {
		return switchScale(getScaleType()).getValue();
	}

    public Map<String, String> getScaleRiskScoreColorMap() {
        return getRiskScoreColorMap(switchScale(getScaleType()));
    }

    public String getScaleProbabilityNumberExplainer() {
        return getProbabilityNumberExplainer(switchScale(getScaleType()));
    }

    public String getScaleConsequenceNumberExplainer() {
        return getConsequenceNumberExplainer(switchScale(getScaleType()));
    }

    public String getScaleRiskScoreExplainer() {
        return getRiskScoreExplainer(switchScale(getScaleType()));
    }

    private String getRiskScoreExplainer(final RiskScaleType riskScaleType) {
        final StringBuilder builder = new StringBuilder();
        switch (riskScaleType) {
            case SCALE_1_4 -> {
                builder.append("Risikoscore skala\n");
                builder.append("Risikoscore = sandsynlighed * konsekvens\n");
                builder.append("1-4 = Lav risiko (grøn)\n");
                builder.append("5-11 = Middel risiko (gul)\n");
                builder.append("12-16 = Høj risiko (rød)\n");
                return builder.toString();
            }
            case SCALE_1_10 -> {
                // TODO explainer for denne
                return builder.toString();
            }
        }
        return builder.toString();
    }

    private String getProbabilityNumberExplainer(final RiskScaleType riskScaleType) {
        final StringBuilder builder = new StringBuilder();
        switch (riskScaleType) {
            case SCALE_1_4 -> {
                builder.append("Sandsynlighed skala\n");
                builder.append("1 = Usandsynligt\n");
                builder.append("2 = Mindre sandsynligt\n");
                builder.append("3 = Sandsynligt\n");
                builder.append("4 = Forventet");
                return builder.toString();
            }
            case SCALE_1_10 -> {
                // TODO explainer for denne
                return builder.toString();
            }
        }
        return builder.toString();
    }
    private String getConsequenceNumberExplainer(final RiskScaleType riskScaleType) {
        final StringBuilder builder = new StringBuilder();
        switch (riskScaleType) {
            case SCALE_1_4 -> {
                builder.append("Konsekvens skala\n");
                builder.append("1 = Ubetydelig\n");
                builder.append("2 = Mindre alvorlig\n");
                builder.append("3 = Meget alvorlig\n");
                builder.append("4 = Graverende/ødelæggende");
                return builder.toString();
            }
            case SCALE_1_10 -> {
                // TODO explainer for denne
                return builder.toString();
            }
        }
        return builder.toString();
    }

    private Map<String, String> getRiskScoreColorMap(final RiskScaleType riskScaleType) {
        final Map<String, String> map = new HashMap<>();
        switch (riskScaleType) {
            case SCALE_1_4 -> {
                map.put("1,1", "GRØN");
                map.put("1,2", "GRØN");
                map.put("1,3", "GRØN");
                map.put("1,4", "GUL");
                map.put("2,1", "GRØN");
                map.put("2,2", "GRØN");
                map.put("2,3", "GUL");
                map.put("2,4", "GUL");
                map.put("3,1", "GRØN");
                map.put("3,2", "GUL");
                map.put("3,3", "GUL");
                map.put("3,4", "RØD");
                map.put("4,1", "GUL");
                map.put("4,2", "GUL");
                map.put("4,3", "RØD");
                map.put("4,4", "RØD");
                return map;
            }
            case SCALE_1_10 -> {
                // TODO farver for denne
                return map;
            }
        }
        return map;
    }

    public RiskScaleType switchScale(final String s){
        if(isVariantOf(s, "{4=RØD, 3=GUL, 2=GUL, 1=GRØN}")) {
            return RiskScaleType.SCALE_1_4;
        }
        if(isVariantOf(s, "{1=GRØN, 2=GRØN, 3=GRØN, 4=GUL, 5=GUL, 6=GUL, 7=GUL, 8=RØD, 9=RØD, 10=RØD}")) {
            return RiskScaleType.SCALE_1_10;
        }

        return RiskScaleType.SCALE_1_4;
	}

    private boolean isVariantOf(String s1, String s2) {
        return StringUtils.getDigits(s1).length() == StringUtils.getDigits(s2).length();
    }
}
