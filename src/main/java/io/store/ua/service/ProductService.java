package io.store.ua.service;

import io.store.ua.entity.Product;
import io.store.ua.entity.Tag;
import io.store.ua.exceptions.NotFoundException;
import io.store.ua.models.dto.ProductDTO;
import io.store.ua.repository.ProductRepository;
import io.store.ua.repository.TagRepository;
import io.store.ua.utility.CodeGenerator;
import io.store.ua.validations.FieldValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.time.LocalDateTime;
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
    private final TagRepository tagRepository;

    public List<Product> findBy(String titlePart,
                                String codePart,
                                BigInteger minimumPrice,
                                BigInteger maximumPrice,
                                String currency,
                                List<@NotNull(message = "Tag ID can't be null")
                                @Min(value = 1, message = "Tag ID can't be less than 1") Long> tagIds,
                                LocalDateTime from,
                                LocalDateTime to,
                                @Min(value = 1, message = "A size of page can't be less than one") int pageSize,
                                @Min(value = 1, message = "A number of page can't be less than one") int pageNumber) {
        if (to != null && from != null && to.isBefore(from)) {
            throw new ValidationException("A 'to' can't be before 'from'");
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> root = criteriaQuery.from(Product.class);
        List<Predicate> predicateList = new ArrayList<>();

        if (!StringUtils.isBlank(titlePart)) {
            predicateList.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(Product.Fields.title)),
                    "%" + titlePart.toLowerCase() + "%"));
        }

        if (!StringUtils.isBlank(codePart)) {
            predicateList.add(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get(Product.Fields.code)),
                            "%" + codePart.toLowerCase() + "%"
                    )
            );
        }

        if (Objects.nonNull(minimumPrice) && Objects.nonNull(maximumPrice) && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new ValidationException("Minimum price can't be greater than max price");
        }

        if (minimumPrice != null) {
            predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Product.Fields.price), minimumPrice));
        }

        if (maximumPrice != null) {
            predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get(Product.Fields.price), maximumPrice));
        }

        if (!StringUtils.isBlank(currency)) {
            predicateList.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get(Product.Fields.currency)), currency.toLowerCase()));
        }

        if (from != null) {
            predicateList.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Product.Fields.createdAt), from));
        }

        if (to != null) {
            predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get(Product.Fields.createdAt), to));
        }

        Join<Product, Tag> tagsJoin;
        Expression<Long> distinctTagCount;

        if (tagIds != null && !tagIds.isEmpty()) {
            tagsJoin = root.join(Product.Fields.tags, JoinType.INNER);
            predicateList.add(tagsJoin.get(Tag.Fields.id).in(tagIds));
            criteriaQuery.groupBy(root.get(Product.Fields.id));
            distinctTagCount = criteriaBuilder.countDistinct(tagsJoin.get(Tag.Fields.id));
            criteriaQuery.having(criteriaBuilder.equal(distinctTagCount, (long) tagIds.size()));
        }

        criteriaQuery
                .select(root)
                .where(predicateList.toArray(new Predicate[0]))
                .orderBy(criteriaBuilder.asc(root.get(Product.Fields.id)));

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult(pageSize * (pageNumber - 1))
                .setMaxResults(pageSize)
                .getResultList();
    }

    public Product save(@NotNull(message = "Product can't be null") ProductDTO productDTO) {
        fieldValidator.validate(productDTO, true,
                ProductDTO.Fields.title,
                ProductDTO.Fields.price,
                ProductDTO.Fields.currency);
        fieldValidator.validate(productDTO, false,
                ProductDTO.Fields.description,
                ProductDTO.Fields.weight,
                ProductDTO.Fields.length,
                ProductDTO.Fields.width,
                ProductDTO.Fields.height);

        Product product = Product.builder()
                .code(CodeGenerator.StockCodeGenerator.generate())
                .title(productDTO.getTitle())
                .description(productDTO.getDescription())
                .price(productDTO.getPrice())
                .currency(productDTO.getCurrency())
                .weight(productDTO.getWeight())
                .length(productDTO.getLength())
                .width(productDTO.getWidth())
                .height(productDTO.getHeight())
                .build();

        if (productDTO.getTags() != null) {
            fieldValidator.validateObject(productDTO, ProductDTO.Fields.tags, true);
            var foundTags = tagRepository.findDistinctByIdIn(productDTO.getTags());

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

        if (productDTO.getCurrency() != null) {
            fieldValidator.validate(productDTO, ProductDTO.Fields.currency, true);
            product.setCurrency(productDTO.getCurrency());
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
                var foundTags = tagRepository.findDistinctByIdIn(productDTO.getTags());
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
