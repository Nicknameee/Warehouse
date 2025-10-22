package io.store.ua.service;

import io.store.ua.entity.Product;
import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ProductDTO;
import io.store.ua.repository.ProductRepository;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Validated
public class ProductService {
    private final ProductRepository productRepository;
    private final EntityManager entityManager;
    private final FieldValidator fieldValidator;
    private final TagService tagService;

    public List<Product> findAll(@Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                 @Min(value = 1, message = "A page number can't be less than 1") int page) {
        return productRepository.findAll(Pageable.ofSize(pageSize).withPage(page - 1)).getContent();
    }

    public List<Product> findWithTags(@NotEmpty(message = "Tag list can't be empty") List<@NotNull(message = "Tag ID can't e blank") Long> tagIDS,
                                      @Min(value = 1, message = "Size of page can't be less than 1") int pageSize,
                                      @Min(value = 1, message = "A page number can't be less than 1") int page) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> product = criteriaQuery.from(Product.class);

        Join<Product, Tag> tagJoin = product.join(Product.Fields.tags, JoinType.INNER);

        criteriaQuery.select(product)
                .where(tagJoin.get(Tag.Fields.id).in(tagIDS))
                .distinct(true);

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public List<Product> findBy(String titlePart,
                                BigDecimal minimumPrice,
                                BigDecimal maximumPrice,
                                List<@NotNull(message = "Tag ID can't be null") Long> tagIds,
                                ZonedDateTime createdFromInclusive,
                                ZonedDateTime createdToInclusive,
                                @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> productRoot = criteriaQuery.from(Product.class);
        List<Predicate> predicateList = new ArrayList<>();

        if (!StringUtils.isBlank(titlePart)) {
            predicateList.add(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(productRoot.get(Product.Fields.title)),
                            "%" + titlePart.toLowerCase() + "%"
                    )
            );
        }

        if (Objects.nonNull(minimumPrice) && Objects.nonNull(maximumPrice) && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new ValidationException("Minimum price can't be greater than max price");
        }

        if (minimumPrice != null) {
            predicateList.add(criteriaBuilder.greaterThanOrEqualTo(productRoot.get(Product.Fields.price), minimumPrice));
        }
        if (maximumPrice != null) {
            predicateList.add(criteriaBuilder.lessThanOrEqualTo(productRoot.get(Product.Fields.price), maximumPrice));
        }

        if (createdFromInclusive != null && createdToInclusive != null && createdFromInclusive.isAfter(createdToInclusive)) {
            ZonedDateTime swap = createdFromInclusive;
            createdFromInclusive = createdToInclusive;
            createdToInclusive = swap;
        }
        if (createdFromInclusive != null) {
            predicateList.add(criteriaBuilder.greaterThanOrEqualTo(productRoot.get(Product.Fields.createdAt), createdFromInclusive));
        }
        if (createdToInclusive != null) {
            predicateList.add(criteriaBuilder.lessThanOrEqualTo(productRoot.get(Product.Fields.createdAt), createdToInclusive));
        }

        Join<Product, Tag> tagsJoin;
        Expression<Long> distinctTagCount;

        if (tagIds != null && !tagIds.isEmpty()) {
            tagsJoin = productRoot.join(Product.Fields.tags, JoinType.INNER);
            predicateList.add(tagsJoin.get(Tag.Fields.id).in(tagIds));
            criteriaQuery.groupBy(productRoot.get(Product.Fields.id));
            distinctTagCount = criteriaBuilder.countDistinct(tagsJoin.get(Tag.Fields.id));
            criteriaQuery.having(criteriaBuilder.equal(distinctTagCount, (long) tagIds.size()));
        }

        criteriaQuery.where(predicateList.toArray(new Predicate[0]));
        criteriaQuery.orderBy(criteriaBuilder.asc(productRoot.get(Product.Fields.id)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult(pageSize * (pageNumber - 1))
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Product findByCode(@NotBlank String code) {
        return productRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Product with code '%s' was not found".formatted(code)));
    }

    public Product save(@NotNull(message = "Product can't be null") ProductDTO productDTO) {
        fieldValidator.validate(productDTO, true,
                ProductDTO.Fields.code,
                ProductDTO.Fields.title,
                ProductDTO.Fields.description,
                ProductDTO.Fields.price,
                ProductDTO.Fields.weight,
                ProductDTO.Fields.length,
                ProductDTO.Fields.width,
                ProductDTO.Fields.height);

        var existingProduct = productRepository.findByCode(productDTO.getCode());

        if (existingProduct.isPresent()) {
            return existingProduct.get();
        }

        Product product = Product.builder()
                .code(productDTO.getCode())
                .title(productDTO.getTitle())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build();

        if (productDTO.getTags() != null && !productDTO.getTags().isEmpty()) {
            var foundTags = tagService.findAllByIDs(productDTO.getTags());
            if (foundTags.size() != productDTO.getTags().size()) {
                throw new NotFoundException("Certain tags were not found, IDs: [%s], found IDs: [%s]"
                        .formatted(productDTO.getTags(), foundTags.stream().map(Tag::getId).toList()));
            }

            product.setTags(foundTags);
        }

        return productRepository.save(product);
    }

    public Product update(@NotNull(message = "Product can't be null") ProductDTO productDTO) {
        fieldValidator.validate(productDTO, ProductDTO.Fields.code, true);

        Product product = productRepository.findByCode(productDTO.getCode())
                .orElseThrow(() -> new NotFoundException("Product with code '%s' was not found".formatted(productDTO.getCode())));

        if (productDTO.getTitle() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.title, true);
            product.setTitle(productDTO.getTitle());
        }

        if (productDTO.getDescription() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.description, true);
            product.setDescription(productDTO.getDescription());
        }

        if (productDTO.getPrice() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.price, true);
            product.setPrice(productDTO.getPrice());
        }

        if (productDTO.getWeight() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.weight, true);
            product.setWeight(productDTO.getWeight());
        }

        if (productDTO.getLength() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.length, true);
            product.setLength(productDTO.getLength());
        }

        if (productDTO.getWidth() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.width, true);
            product.setWidth(productDTO.getWidth());
        }

        if (productDTO.getHeight() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.height, true);
            product.setHeight(productDTO.getHeight());
        }

        if (productDTO.getTags() != null) {
            if (productDTO.getTags().isEmpty()) {
                product.setTags(new ArrayList<>());
            } else {
                var foundTags = tagService.findAllByIDs(productDTO.getTags());
                if (foundTags.size() != productDTO.getTags().size()) {
                    throw new NotFoundException("Certain tags were not found, IDs: [%s], found IDs: [%s]"
                            .formatted(productDTO.getTags(), foundTags.stream().map(Tag::getId).toList()));
                }

                product.setTags(foundTags);
            }
        }

        return productRepository.save(product);
    }
}
