package dk.digitalidentity.service.tag;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TagableServiceRegistry {
	private final Map<String, TagableService<?>> services;

	@Autowired
    public TagableServiceRegistry(List<TagableService<?>> serviceList) {
		// spring auto-magically supplies the list of all services with the interface Taggable
		this.services = serviceList.stream()
				.filter(service -> service.getEntityType() != null)
				.collect(Collectors.toMap(
						service -> service.getEntityType().getSimpleName().toLowerCase(),
						service -> service
				));
    }

	public TagableService<?> getService(String entityTypeName) {
		TagableService<?> service = services.get(entityTypeName.toLowerCase());
		if (service == null) {
			throw new IllegalArgumentException("Unknown entity type: " + entityTypeName);
		}
		return service;
	}
}