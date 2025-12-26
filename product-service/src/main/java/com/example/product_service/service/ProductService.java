package com.example.product_service.service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.kafka.ProductEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.product_service.external.client.InventoryClient;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer producer;
    private final InventoryClient inventoryClient;

    public ProductService(ProductRepository productRepository, ProductEventProducer producer, InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.producer = producer;
        this.inventoryClient = inventoryClient;
    }


    public ProductResponse createProduct(ProductRequest productRequest) {
        productRepository.findBySkuCode(productRequest.skuCode())
                .ifPresent(product -> {
                    throw new RuntimeException("SKU code already exists: " + productRequest.skuCode());
                });

        Product product = new Product();
        product.setSkuCode(productRequest.skuCode());
        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setQuantity(productRequest.quantity() != null ? productRequest.quantity() : 0);

        Product savedProduct = productRepository.save(product);
        producer.sendProductCreatedEvent(savedProduct.getSkuCode(), savedProduct.getQuantity());

        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponse(product);
    }


    private ProductResponse mapToResponse(Product product) {
        Integer actualQuantity = getActualQuantityFromInventory(product.getSkuCode());

        return new ProductResponse(
                product.getId(),
                product.getSkuCode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                actualQuantity
        );
    }


    private Integer getActualQuantityFromInventory(String skuCode) {
        try {
            log.info("Fetching actual quantity for SKU: {}", skuCode);
            var response = inventoryClient.getInventoryBySkuCode(skuCode);
            if (response.getBody() != null) {
                return response.getBody().quantity();
            }
        } catch (Exception e) {
            log.error("Could not fetch inventory for SKU {}: {}. Using DB fallback.", skuCode, e.getMessage());
        }
        return 0;
    }


    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (!existingProduct.getSkuCode().equals(productRequest.skuCode())) {
            productRepository.findBySkuCode(productRequest.skuCode())
                    .ifPresent(product -> {
                        throw new RuntimeException("SKU code already exists: " + productRequest.skuCode());
                    });
        }

        existingProduct.setSkuCode(productRequest.skuCode());
        existingProduct.setName(productRequest.name());
        existingProduct.setDescription(productRequest.description());
        existingProduct.setPrice(productRequest.price());

        if (productRequest.quantity() != null) {
            existingProduct.setQuantity(productRequest.quantity());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        producer.sendProductUpdatedEvent(updatedProduct.getSkuCode());

        return mapToResponse(updatedProduct);
    }

    public ProductResponse partialUpdateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (productRequest.skuCode() != null && !productRequest.skuCode().equals(existingProduct.getSkuCode())) {
            productRepository.findBySkuCode(productRequest.skuCode())
                    .ifPresent(p -> {
                        throw new RuntimeException("SKU code already exists: " + productRequest.skuCode());
                    });
            existingProduct.setSkuCode(productRequest.skuCode());
        }

        if (productRequest.name() != null) {
            existingProduct.setName(productRequest.name());
        }

        if (productRequest.description() != null) {
            existingProduct.setDescription(productRequest.description());
        }

        if (productRequest.price() != null) {
            existingProduct.setPrice(productRequest.price());
        }

        if (productRequest.quantity() != null) {
            existingProduct.setQuantity(productRequest.quantity());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        producer.sendProductUpdatedEvent(updatedProduct.getSkuCode());

        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        String skuCode = product.getSkuCode();
        productRepository.delete(product);
        producer.sendProductDeletedEvent(skuCode);
    }
}