package com.example.product_service.service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.kafka.ProductEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer producer;

    public ProductService(ProductRepository productRepository, ProductEventProducer producer) {
        this.productRepository = productRepository;
        this.producer = producer;
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
        producer.sendProductCreatedEvent(savedProduct.getSkuCode());

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

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSkuCode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}