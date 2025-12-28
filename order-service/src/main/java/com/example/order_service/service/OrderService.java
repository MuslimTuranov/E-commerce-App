package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.event.OrderPlacedEvent;
import com.example.order_service.kafka.OrderEventProducer;
import com.example.order_service.model.Order;
import com.example.order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository orderRepository, OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        // Create new order
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setSkuCode(orderRequest.skuCode());
        order.setQuantity(orderRequest.quantity());
        order.setPrice(orderRequest.price());

        // Save order to database
        Order savedOrder = orderRepository.save(order);

        // Publish order placed event to Kafka
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(
                savedOrder.getOrderNumber(),
                "customer@example.com" // This would typically come from the request or authentication context
        );
        orderEventProducer.sendOrderPlacedEvent(orderPlacedEvent);

        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        Optional<Order> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            return mapToResponse(optionalOrder.get());
        } else {
            throw new RuntimeException("Order not found with id: " + id);
        }
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Optional<Order> optionalOrder = orderRepository.findByOrderNumber(orderNumber);
        if (optionalOrder.isPresent()) {
            return mapToResponse(optionalOrder.get());
        } else {
            throw new RuntimeException("Order not found with order number: " + orderNumber);
        }
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersBySkuCode(String skuCode) {
        return orderRepository.findBySkuCode(skuCode).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        Optional<Order> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            // In a real implementation, you would have a status field and update it
            // For now, we'll just return the order as-is
            return mapToResponse(order);
        } else {
            throw new RuntimeException("Order not found with id: " + id);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSkuCode(),
                order.getQuantity(),
                order.getPrice()
        );
    }
}
