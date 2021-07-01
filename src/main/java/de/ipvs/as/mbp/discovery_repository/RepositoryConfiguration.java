package de.ipvs.as.mbp.discovery_repository;

import de.ipvs.as.mbp.discovery_repository.service.repository.RepositoryClient;
import de.ipvs.as.mbp.discovery_repository.service.repository.impl.elasticsearch.ElasticSearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of a client that enables interaction with a repository to store, manage and search device descriptions.
 */
@Configuration
public class RepositoryConfiguration {

    /**
     * Creates a bean that represents a client for interacting with a repository that can be used
     * to store, manage and search device descriptions. This bean offers an uniform and technology-agnostic
     * interface, so that the actually used repository technology is hidden from other components.
     *
     * @return The configured client for interacting with the repository
     */
    @Bean
    public RepositoryClient repositoryClient() {
        //Use elasticsearch repository
        return new ElasticSearchClient();
    }
}
