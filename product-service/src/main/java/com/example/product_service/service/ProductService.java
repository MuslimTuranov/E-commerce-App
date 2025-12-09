package com.example.product_service.service;

import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.model.Product;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.kafka.ProductEventProducer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer producer; // отдельный продюсер

    public ProductService(ProductRepository productRepository, ProductEventProducer producer) {
        this.productRepository = productRepository;
        this.producer = producer;
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product(
                null,
                productRequest.skuCode(),
                productRequest.name(),
                productRequest.description(),
                productRequest.price()
        );

        productRepository.save(product);

        producer.sendProductCreatedEvent(product.getSkuCode());

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                0
        );
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        0
                ))
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                0
        );
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

        Product updatedProduct = productRepository.save(existingProduct);

        producer.sendProductUpdatedEvent(updatedProduct.getSkuCode());

        return new ProductResponse(
                updatedProduct.getId(),
                updatedProduct.getName(),
                updatedProduct.getDescription(),
                updatedProduct.getPrice(),
                0
        );
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        String skuCode = product.getSkuCode();

        productRepository.delete(product);

        producer.sendProductDeletedEvent(skuCode);
    }

    public ProductResponse partialUpdateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (productRequest.skuCode() != null) {
            if (!existingProduct.getSkuCode().equals(productRequest.skuCode())) {
                productRepository.findBySkuCode(productRequest.skuCode())
                        .ifPresent(p -> {
                            throw new RuntimeException("SKU code already exists: " + productRequest.skuCode());
                        });
            }
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

        Product updatedProduct = productRepository.save(existingProduct);
        producer.sendProductUpdatedEvent(updatedProduct.getSkuCode());

        return new ProductResponse(
                updatedProduct.getId(),
                updatedProduct.getName(),
                updatedProduct.getDescription(),
                updatedProduct.getPrice(),
                0
        );
    }
}


