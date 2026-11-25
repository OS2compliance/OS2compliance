package dk.digitalidentity.integration.kitos;

import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Property;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.SettingsService;
import dk.digitalidentity.service.SupplierService;
import dk.kitos.api.model.ItSystemResponseDTO;
import dk.kitos.api.model.ShallowOrganizationResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dk.digitalidentity.integration.kitos.KitosConstants.KITOS_UUID_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitosSyncServiceSupplierTest {

    @Mock private AssetService assetService;
    @Mock private SupplierService supplierService;
    @Mock private SettingsService settingsService;

    @InjectMocks private KitosSyncService kitosSyncService;

    @Test
    void assetWithoutSupplierGetsTheRightsHolder() {
        final Asset asset = new Asset();
        final Supplier rightsHolder = new Supplier();
        when(supplierService.findByCvr("12345678")).thenReturn(Optional.of(rightsHolder));

        sync(asset);

        assertThat(asset.getSupplier()).isSameAs(rightsHolder);
    }

    @Test
    void supplierCreatedByKitosIsKept() {
        final Supplier kitosSupplier = new Supplier();
        kitosSupplier.getProperties().add(Property.builder().key(KITOS_UUID_PROPERTY_KEY).value("x").entity(kitosSupplier).build());
        final Asset asset = new Asset();
        asset.setSupplier(kitosSupplier);

        sync(asset);

        assertThat(asset.getSupplier()).isSameAs(kitosSupplier);
    }

    private void sync(final Asset asset) {
        final UUID systemUuid = UUID.randomUUID();
        when(assetService.findByProperty(KITOS_UUID_PROPERTY_KEY, systemUuid.toString())).thenReturn(Optional.of(asset));
        kitosSyncService.syncItSystems(List.of(new ItSystemResponseDTO()
            .uuid(systemUuid)
            .rightsHolder(new ShallowOrganizationResponseDTO().cvr("12345678"))));
    }
}
