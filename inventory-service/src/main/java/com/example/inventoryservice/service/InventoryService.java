package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.kafka.InventoryEventProducer;
import com.example.inventoryservice.model.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {


    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryService(InventoryRepository inventoryRepository, InventoryEventProducer inventoryEventProducer) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    public boolean isInStock(String skuCode, Integer quantity){
        return inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual(skuCode, quantity);
    }

    @Transactional
    public InventoryResponse upsertInventory(InventoryRequest request){

        int updatedCount = inventoryRepository.increaseInventoryQuantity(request.skuCode(), request.quantity());

        if(updatedCount == 0){
            Inventory newInventory = new Inventory();
            newInventory.setSkuCode(request.skuCode());
            newInventory.setQuantity(request.quantity());
            inventoryRepository.save(newInventory);
            inventoryEventProducer.sendInventoryUpdatedEvent(request.skuCode(), request.quantity());
            return mapToResponse(newInventory);
        }

        Optional<Inventory> optionalInventory = inventoryRepository.findBySkuCode(request.skuCode());

        if(optionalInventory.isPresent()){
            Inventory inventory = optionalInventory.get();
            inventoryEventProducer.sendInventoryUpdatedEvent(request.skuCode(), inventory.getQuantity());
            return mapToResponse(inventory);
        }else{
            throw new IllegalStateException("Inventory not found after update");
        }
    }

    @Transactional
    public InventoryResponse decreaseInventory(InventoryRequest request){
        int updatedCount = inventoryRepository.decreaseInventoryQuantity(request.skuCode(), request.quantity());

        if(updatedCount == 0){
            boolean exists = inventoryRepository.findBySkuCode(request.skuCode()).isPresent();
            if(!exists){
                throw new ResourceNotFoundException("Inventory not found for skuCode: " + request.skuCode());
            }
            throw new IllegalStateException("Insufficient stock for skuCode " + request.skuCode());
        }
        Optional<Inventory> optionalInventory = inventoryRepository.findBySkuCode(request.skuCode());
        if(optionalInventory.isPresent()){
            Inventory inventory = optionalInventory.get();
            inventoryEventProducer.sendInventoryUpdatedEvent(request.skuCode(), inventory.getQuantity());

            // Check for low stock
            if(inventory.getQuantity() <= 5) {
                inventoryEventProducer.sendInventoryLowStockEvent(request.skuCode(), inventory.getQuantity());
            }

            // Check for out of stock
            if(inventory.getQuantity() == 0) {
                inventoryEventProducer.sendInventoryOutOfStockEvent(request.skuCode());
            }

            return mapToResponse(inventory);
        }else{
            throw new IllegalStateException("Failed to retrieve updated inventory");
        }
    }

    public InventoryResponse getInventoryBySkuCode(String skuCode){
        Optional<Inventory> optionalInventory = inventoryRepository.findBySkuCode(skuCode);
        if(optionalInventory.isPresent()){
            return mapToResponse(optionalInventory.get());
        }else{
            throw new ResourceNotFoundException("Inventory not found for skuCode: " + skuCode);
        }

    }

    private InventoryResponse mapToResponse(Inventory inventory){
        return new InventoryResponse(
                inventory.getId(), inventory.getSkuCode(), inventory.getQuantity()
        );
    }


}
