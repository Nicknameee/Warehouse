package io.store.ua.entity.immutable;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "product_tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTagLink {
    @Id
    private Long id;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @PreUpdate
    private void notSupported() {
        throw new UnsupportedOperationException();
    }
}
