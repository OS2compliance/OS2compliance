package dk.digitalidentity.controller.mvc;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.DPIAReportService;
import dk.digitalidentity.service.S3DocumentService;
import dk.digitalidentity.service.S3Service;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;

import java.io.ByteArrayOutputStream;
import java.security.Security;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SigningControllerTest {
	private final ThreatAssessmentService threatAssessmentService = mock(ThreatAssessmentService.class);
	private final S3DocumentService s3DocumentService = mock(S3DocumentService.class);
	private final S3Service s3Service = mock(S3Service.class);
	private final UserService userService = mock(UserService.class);

	@BeforeAll
	static void registerBouncyCastle() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@AfterEach
	void logout() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void signingRiskAssessmentUploadsSignedPdf() throws Exception {
		final User approver = new User();
		approver.setUuid("godkender");
		final ThreatAssessment threatAssessment = new ThreatAssessment();
		threatAssessment.setThreatAssessmentReportApprover(approver);
		threatAssessment.setThreatAssessmentReportApprovalStatus(ThreatAssessmentReportApprovalStatus.WAITING);
		final S3Document report = new S3Document();
		report.setS3FileKey("rapport");

		when(s3DocumentService.get(1L)).thenReturn(Optional.of(report));
		when(threatAssessmentService.findByS3Document(report)).thenReturn(threatAssessment);
		when(s3Service.downloadBytes("rapport")).thenReturn(emptyPdf());
		login(approver);

		assertEquals("sign/signed", controller().signDocument(new ExtendedModelMap(), 1L));
		verify(s3Service).uploadWithKey(eq("rapport"), any());
		assertEquals(ThreatAssessmentReportApprovalStatus.SIGNED, threatAssessment.getThreatAssessmentReportApprovalStatus());
	}

	private SigningController controller() {
		final OS2complianceConfiguration configuration = new OS2complianceConfiguration();
		configuration.getPdfCertificate().setPath("src/main/resources/OS2compliance.p12");
		configuration.getPdfCertificate().setPassword("Test1234");
		configuration.getPdfCertificate().setAlias("digital identity aps");
		return new SigningController(threatAssessmentService, s3DocumentService, s3Service, userService, configuration, mock(DPIAReportService.class));
	}

	private static void login(final User user) {
		final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getUuid(), null, List.of());
		auth.setDetails(TokenUser.builder().username(user.getUuid()).build());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private static byte[] emptyPdf() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
			pdf.addNewPage();
		}
		return out.toByteArray();
	}
}
