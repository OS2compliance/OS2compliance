package dk.digitalidentity.integration.os2sync;

import dk.digitalidentity.integration.os2sync.api.HierarchyOU;
import dk.digitalidentity.integration.os2sync.api.HierarchyPosition;
import dk.digitalidentity.integration.os2sync.api.HierarchyUser;

public abstract class TestDataGenerator {

    public static HierarchyPosition generatePosition(final String name, final String ouUuid) {
        final var position = new HierarchyPosition();
        position.setName(name);
        position.setOuUuid(ouUuid);
        return position;
    }

    public static HierarchyUser generateUser(final String uuid, final String userId, final String name, final HierarchyPosition... positions) {
        final var user = new HierarchyUser();
        user.setUuid(uuid);
        user.setUserId(userId);
        user.setName(name);
        for (HierarchyPosition position : positions) {
            user.getPositions().add(position);
        }
        return user;
    }

    public static HierarchyOU generateOU(final String uuid, final String name, final String parentOu) {
        final var ou = new HierarchyOU();
        ou.setUuid(uuid);
        ou.setName(name);
        ou.setParentUUID(parentOu);
        return ou;
    }

}
