package dk.digitalidentity.controller.mvc;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.lowagie.text.DocumentException;
import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.entity.S3Document;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.ThreatAssessmentReportApprovalStatus;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.S3DocumentService;
import dk.digitalidentity.service.S3Service;
import dk.digitalidentity.service.ThreatAssessmentService;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Controller
@RequireUser
@RequestMapping("sign")
@RequiredArgsConstructor
public class SigningController {
    private final ThreatAssessmentService threatAssessmentService;
    private final S3DocumentService s3DocumentService;
    private final S3Service s3Service;
    private final UserService userService;
    private final OS2complianceConfiguration configuration;

    @GetMapping("view/{S3DocumentId}")
    public String viewDocumentToSign(final Model model, @PathVariable("S3DocumentId") final long s3DocumentId) {
        final S3Document s3Document = s3DocumentService.get(s3DocumentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatAssessment threatAssessment = threatAssessmentService.findByS3Document(s3Document);

        boolean canSign = false;
        if (threatAssessment != null) {
            if (!threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.SIGNED)) {
                String approverUuid = threatAssessment.getThreatAssessmentReportApprover() != null ? threatAssessment.getThreatAssessmentReportApprover().getUuid() : null;
                if (Objects.equals(approverUuid, SecurityUtil.getLoggedInUserUuid())) {
                    canSign = true;
                }
            }
        }

        model.addAttribute("canSign", canSign);
        model.addAttribute("id", s3DocumentId);

        return "sign/view";
    }

    @GetMapping("preview/{S3DocumentId}")
    public String previewDocumentToSign(final Model model, @PathVariable("S3DocumentId") final long s3DocumentId) {
        final S3Document s3Document = s3DocumentService.get(s3DocumentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatAssessment threatAssessment = threatAssessmentService.findByS3Document(s3Document);

        if (threatAssessment != null) {
            if (!threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.WAITING)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            String approverUuid = threatAssessment.getThreatAssessmentReportApprover() != null ? threatAssessment.getThreatAssessmentReportApprover().getUuid() : null;
            if (!Objects.equals(approverUuid, SecurityUtil.getLoggedInUserUuid())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }

        model.addAttribute("id", s3DocumentId);

        return "sign/preview";
    }

    @GetMapping("{S3DocumentId}")
    public String signDocument(final Model model, @PathVariable("S3DocumentId") final long s3DocumentId) throws IOException, GeneralSecurityException {
        final S3Document s3Document = s3DocumentService.get(s3DocumentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ThreatAssessment threatAssessment = threatAssessmentService.findByS3Document(s3Document);

        if (threatAssessment != null) {
            if (!threatAssessment.getThreatAssessmentReportApprovalStatus().equals(ThreatAssessmentReportApprovalStatus.WAITING)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            String approverUuid = threatAssessment.getThreatAssessmentReportApprover() != null ? threatAssessment.getThreatAssessmentReportApprover().getUuid() : null;
            if (!Objects.equals(approverUuid, SecurityUtil.getLoggedInUserUuid())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            threatAssessment.setThreatAssessmentReportApprovalStatus(ThreatAssessmentReportApprovalStatus.SIGNED);
            threatAssessmentService.save(threatAssessment);

            model.addAttribute("threatAssessmentId", threatAssessment.getId());
        }

        byte[] signedPDF = signPdf(s3Document);
        s3Service.uploadWithKey(s3Document.getS3FileKey(), signedPDF);

        model.addAttribute("id", s3DocumentId);

        return "sign/signed";
    }

    @GetMapping("pdf/{S3DocumentId}")
    public ResponseEntity<ByteArrayResource> getPdf(@PathVariable("S3DocumentId") final long s3DocumentId, HttpServletResponse response) throws IOException, DocumentException {
        final S3Document s3Document = s3DocumentService.get(s3DocumentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        byte[] byteData = s3Service.downloadBytes(s3Document.getS3FileKey());
        ByteArrayResource resource = new ByteArrayResource(byteData);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport.pdf");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(byteData.length)
            .contentType(MediaType.parseMediaType("application/pdf"))
            .body(resource);
    }

    @GetMapping("pdf/signed/{S3DocumentId}")
    public ResponseEntity<ByteArrayResource> getSignedPdf(@PathVariable("S3DocumentId") final long s3DocumentId, HttpServletResponse response) throws IOException, GeneralSecurityException {
        final S3Document s3Document = s3DocumentService.get(s3DocumentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        byte[] updatedByteData = signPdf(s3Document);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=signeret_rapport.pdf");
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        ByteArrayResource resource = new ByteArrayResource(updatedByteData);
        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(updatedByteData.length)
            .contentType(MediaType.parseMediaType("application/pdf"))
            .body(resource);
    }

    private byte[] signPdf(S3Document s3Document) throws IOException, GeneralSecurityException {
        final User user = userService.currentUser();
        final String loggedInUserName = user != null ? user.getName() : "";
        byte[] byteData = s3Service.downloadBytes(s3Document.getS3FileKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(byteData));
        PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties());
        Document document = new Document(signer.getDocument());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(configuration.getPdfCertificate().getPath()), configuration.getPdfCertificate().getPassword().toCharArray());
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(configuration.getPdfCertificate().getAlias(), configuration.getPdfCertificate().getPassword().toCharArray());
        Certificate[] chain = keyStore.getCertificateChain(configuration.getPdfCertificate().getAlias());

        int numberOfPages = signer.getDocument().getNumberOfPages();
        document.showTextAligned(new Paragraph("Signeret af: " + loggedInUserName),
            36, 36, numberOfPages, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);
        document.showTextAligned(new Paragraph("Dato: " + LocalDate.now()),
            36, 24, numberOfPages, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
            .setReason("Dokument signeret i OS2compliance")
            .setSignatureCreator(loggedInUserName)
            .setLocation("Denmark")
            .setPageRect(new Rectangle(400, 24, 200, 50))
            .setPageNumber(signer.getDocument().getNumberOfPages())
            .setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);

        IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, "BC");
        IExternalDigest digest = new BouncyCastleDigest();
        signer.signDetached(digest, pks, chain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);

        reader.close();
        signer.getDocument().close();

        return outputStream.toByteArray();
    }

}
