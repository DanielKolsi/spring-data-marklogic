package io.github.malteseduck.springframework.data.marklogic.repository.support;

import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import io.github.malteseduck.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

public class MappingMarkLogicEntityInformation <T, ID> extends PersistentEntityInformation<T, ID>
		implements MarkLogicEntityInformation<T, ID> {

    private final MarkLogicPersistentEntity<T> entityMetadata;
    private final Class<ID> fallbackIdType;

    public MappingMarkLogicEntityInformation(MarkLogicPersistentEntity<T> entityMetadata) {
        this(entityMetadata, null);
    }

    public MappingMarkLogicEntityInformation(MarkLogicPersistentEntity<T> entityMetadata, Class<ID> idType) {
        super(entityMetadata);
        this.entityMetadata = entityMetadata;
        this.fallbackIdType =  idType;
    }

    @Override
    public String getCollectionName() {
        return entityMetadata.getCollection();
    }

    @Override
    public String getIdAttribute() {
        return entityMetadata.getIdProperty().getName();
    }

    @Override
    public Class<ID> getIdType() {
        if (this.entityMetadata.hasIdProperty()) {
            return super.getIdType();
        }
        throw new IllegalArgumentException("Entity must be annotated with @Id");
    }
}
