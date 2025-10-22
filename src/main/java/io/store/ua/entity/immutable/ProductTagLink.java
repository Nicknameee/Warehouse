package io.store.ua.entity.immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
}
