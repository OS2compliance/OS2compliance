package dk.digitalidentity.controller.api;

import dk.digitalidentity.mapping.UserMapper;
import dk.digitalidentity.model.api.ErrorEO;
import dk.digitalidentity.model.api.PageEO;
import dk.digitalidentity.model.api.PositionEO;
import dk.digitalidentity.model.api.UserCreateEO;
import dk.digitalidentity.model.api.UserEO;
import dk.digitalidentity.model.api.UserUpdateEO;
import dk.digitalidentity.model.entity.Position;
import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.service.NotifyService;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/users")
@Tag(name = "Users resource")
@RequiredArgsConstructor
public class UserApiController {
    private static final String SYNC_NOTICE = "<br><u>NOTICE! If organisation data is synchronised from FK Organisation (OS2sync), users not present in the synchronised hierarchy will be deactivated by the next synchronisation run</u>";

    private final UserService userService;
    private final UserMapper userMapper;
    private final OrganisationService organisationService;
    private final NotifyService notifyService;

    @Operation(summary = "Fetch a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "{uuid}", produces = "application/json")
    public UserEO read(@PathVariable final String uuid) {
        final User user = userService.get(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return userMapper.toEO(user);
    }


    @Operation(summary = "Find a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The user"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(value = "find", produces = "application/json")
    public UserEO find(@RequestParam("userId") final String userId) {
        final User user = userService.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return userMapper.toEO(user);
    }

    @Operation(summary = "List all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping(produces = "application/json")
    public PageEO<UserEO> list(@Parameter(description = "Page size to fetch, max is 500")  @RequestParam(value = "pageSize", defaultValue = "100") @Max(500) final int pageSize,
                               @Parameter(description = "The page to fetch, first page is 0") @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero final int page) {
        return userMapper.toEO(userService.getPaged(pageSize, page));
    }

    @Operation(summary = "Create a new user", description = "Creates a user along with the positions that link the user to organisation units. The userId must be unique across all users, active as well as inactive." + SYNC_NOTICE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The created user"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "409", description = "A user with the given uuid or userId already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public UserEO create(@Valid @RequestBody final UserCreateEO userCreateEO) {
        final String uuid = StringUtils.trimToNull(userCreateEO.getUuid());
        if (userService.get(uuid).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with the given uuid already exists");
        }
        assertUserIdAvailable(userCreateEO.getUserId(), null);
        final User user = userMapper.fromEO(userCreateEO);
        user.setUuid(uuid != null ? uuid : UUID.randomUUID().toString());
        if (user.getActive() == null) {
            user.setActive(true);
        }
        user.getPositions().addAll(toPositions(user, userCreateEO.getPositions()));
        try {
            return userMapper.toEO(userService.create(user));
        } catch (final DataIntegrityViolationException | PersistenceException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with the given uuid already exists");
        }
    }

    @Operation(summary = "Update a user", description = "Updates a user, the client should make a GET request first to ensure they have the newest version, update the fields they need and then call this method with the complete entity. "
            + "The supplied positions replace all existing positions on the user, and positions are not guaranteed to keep their ids between updates. "
            + "Note that there is no version check, concurrent updates of the same user are last-write-wins." + SYNC_NOTICE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No content", content = @Content(mediaType = "application/json", schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class))),
            @ApiResponse(responseCode = "409", description = "Another user with the given userId already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorEO.class)))
    })
    @PutMapping(value = "{uuid}", consumes = "application/json")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable final String uuid, @Valid @RequestBody final UserUpdateEO userUpdateEO) {
        final User user = userService.get(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        assertUserIdAvailable(userUpdateEO.getUserId(), user.getUuid());
        final boolean deactivated = Boolean.TRUE.equals(user.getActive()) && Boolean.FALSE.equals(userUpdateEO.getActive());
        user.setUserId(userUpdateEO.getUserId());
        user.setName(userUpdateEO.getName());
        user.setEmail(userUpdateEO.getEmail());
        user.setActive(userUpdateEO.getActive());
        user.getPositions().clear();
        user.getPositions().addAll(toPositions(user, userUpdateEO.getPositions()));
        userService.save(user);
        if (deactivated) {
            // the sync path notifies when responsible users go inactive, keep the API path consistent
            notifyService.notifyAboutInactiveUsers(Set.of(uuid));
        }
    }

    private void assertUserIdAvailable(final String userId, final String ownUuid) {
        final boolean taken = userService.findAllByUserIdIncludingInactive(userId).stream()
                .anyMatch(other -> !other.getUuid().equals(ownUuid));
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with the given userId already exists");
        }
    }

    private Set<Position> toPositions(final User user, final Set<PositionEO> positionEOs) {
        if (positionEOs == null) {
            return Collections.emptySet();
        }
        return positionEOs.stream()
                .map(positionEO -> {
                    organisationService.get(positionEO.getOuUuid())
                            .filter(ou -> Boolean.TRUE.equals(ou.getActive()))
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active organisation unit not found: " + positionEO.getOuUuid()));
                    final Position position = userMapper.fromEO(positionEO);
                    position.setUser(user);
                    return position;
                })
                .collect(Collectors.toSet());
    }

}
