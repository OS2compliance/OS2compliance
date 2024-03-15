package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.DocumentMapper;
import dk.digitalidentity.model.api.DocumentCreateEO;
import dk.digitalidentity.model.api.DocumentEO;
import dk.digitalidentity.model.api.DocumentUpdateEO;
import dk.digitalidentity.model.api.ErrorEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.model.entity.enums.DocumentRevisionInterval;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.DocumentType;
import dk.digitalidentity.service.DocumentService;
import dk.digitalidentity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static dk.digitalidentity.util.NullSafe.nullSafe;


@RestController
@RequestMapping(value = "/api/v1/documents")
@Tag(name = "Document resource")
@RequiredArgsConstructor
public class DocumentApiController {
    private final UserService userService;
    private final DocumentService documentService;
    private final DocumentMapper documentMapper;


    @Operation(summary = "Fetch a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get a document"),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(value = "{id}", produces = "application/json")
    public DocumentEO read(@PathVariable final Long id) {
        final Document document = documentService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return documentMapper.toEO(document);
    }

    @Operation(summary = "List all documents")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All documents"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @GetMapping(produces = "application/json")
    public PageEO<DocumentEO> list(@Parameter(description = "Page size to fetch, max is 500")  @RequestParam(value = "pageSize", defaultValue = "100") @Max(500) final int pageSize,
                                   @Parameter(description = "The page to fetch, first page is 0") @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero final int page) {
        return documentMapper.toEO(documentService.getPaged(pageSize, page));
    }

    @Operation(summary = "Create a new document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The created document"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentEO create(@Valid @RequestBody final DocumentCreateEO documentCreateEO) {
        final User responsibleUser = userService.get(nullSafe(() -> documentCreateEO.getResponsibleUser().getUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsible user not valid"));
        final Document document = documentMapper.fromEO(documentCreateEO);
        document.setResponsibleUser(responsibleUser);
        return documentMapper.toEO(documentService.create(document));
    }

    @Operation(summary = "Update a document", description = "Updates a document, the client should make a GET request first to ensure they have the newest version, update the fields they need and then call this method with the complete entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "409", description = "Conflicting version", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PutMapping(value = "{id}", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable final Long id, @Valid @RequestBody final DocumentUpdateEO documentUpdateEO) {
        final Document document = documentService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (document.getVersion() != documentUpdateEO.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version mismatch");
        }
        final User responsibleUser = userService.get(nullSafe(() -> documentUpdateEO.getResponsibleUser().getUuid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsible user not valid"));
        document.setName(documentUpdateEO.getName());
        document.setResponsibleUser(responsibleUser);
        document.setStatus(DocumentStatus.valueOf(documentUpdateEO.getStatus().name()));
        document.setDocumentType(DocumentType.valueOf(documentUpdateEO.getDocumentType().name()));
        document.setDescription(documentUpdateEO.getDescription());
        document.setLink(documentUpdateEO.getLink());
        document.setDocumentVersion(documentUpdateEO.getDocumentVersion());
        document.setRevisionInterval(nullSafe(() -> DocumentRevisionInterval.valueOf(documentUpdateEO.getRevisionInterval().name())));
        document.setNextRevision(documentUpdateEO.getNextRevision());
        documentService.update(document);
    }

    @Operation(summary = "Delete a document", description = "Deletes a document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @DeleteMapping(value = "{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        final Document document = documentService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        documentService.delete(document);
    }

}
