package dk.digitalidentity.report.riskimage;

import dk.digitalidentity.report.XlsUtil;
import dk.digitalidentity.report.riskimage.dto.ThreatRow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dk.digitalidentity.report.XlsUtil.*;

public class RiskImageView extends AbstractXlsView {
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {

		List<ThreatRow> threatAssessments = threatRowListFromModel(model);
		createRiskImageSheet(workbook, threatAssessments);

	}

	private void createRiskImageSheet(Workbook workbook, List<ThreatRow> threatRows) {
		String sheetName = "Risikobillede";

		Map<String, XlsUtil.CellValueSetter<ThreatRow, Cell>> converterMap = new LinkedHashMap<>();
		converterMap.put("Trussel", (t, c) -> c.setCellValue(t.name()));
		converterMap.put("Trusselskatalog", (t, c) -> c.setCellValue(t.threatCatalogName()));

		List<String> threatAssessmentNames = threatRows.stream()
				.flatMap(tr -> tr.assetThreatScoresByThreatAssessmentName().keySet().stream())
				.sorted()
				.toList();

		for (String threatAssessmentName : threatAssessmentNames) {
			converterMap.put(
					threatAssessmentName,
					(t, c) -> {
						if (t.assetThreatScoresByThreatAssessmentName() != null && t.assetThreatScoresByThreatAssessmentName().containsKey(threatAssessmentName)) {
							c.setCellValue(t.assetThreatScoresByThreatAssessmentName().get(threatAssessmentName));
						} else {
							c.setCellValue("");
						}
					}
					);
		}

		converterMap.put("SAMLET", (t, c) -> c.setCellValue(t.totalThreatScore()));

		createSheet(sheetName, threatRows, converterMap, workbook);

	}

	private <T> List<T> threatRowListFromModel(Map<String, Object> model) {
		String propertyName = "threats";
		try {
			Object modelObject = model.get(propertyName);
			if (modelObject instanceof List<?> rawList && !rawList.isEmpty() && rawList.getFirst() instanceof ThreatRow) {
				@SuppressWarnings("unchecked")
				List<T> list = (List<T>) rawList;
				return list;
			}
			return Collections.emptyList();
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

}
