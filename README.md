# 🛒 E-Commerce Microservices Platform

## A scalable, event-driven microservices architecture for an e-commerce platform built with **Spring Boot**.

---

## 🚀 Features

- **Product Service** — Product catalog management
- **Inventory Service** — Real-time inventory tracking
- **Order Service** — Order processing with event-driven architecture

- **API Gateway** — Single entry point with request routing and load balancing
- **Event-Driven Architecture** — Apache Kafka for asynchronous communication
- **Containerized** — Docker & Docker Compose for easy deployment
- **API Documentation** — Integrated Swagger UI for all services

---

## 🛠️ Tech Stack

- **Java**
- **Spring Boot**
- **PostgreSQL**
- **Apache Kafka**
- **Docker & Docker Compose**
- **OpenAPI 3.0 (Swagger)**

---

## 📦 Prerequisites

Ensure you have the following installed:

- Docker Desktop (with Docker Compose)
- Maven `3.9.x` or later
- Git

---

## 🚀 Quick Start

### 1️⃣ Clone the repository
```bash
git clone <repository-url>
cd spring-boot-microservices
```

2️⃣ Build the project
```
mvn clean install
```

3️⃣ Start the services
```
docker-compose up -d
```

This will start all microservices and required infrastructure.

4️⃣ Verify running services
```
docker-compose ps
```

## 🌐 Access Services
Service	URL	Port
- API Gateway	http://localhost:8080
	8080
- Product Service	http://localhost:8081
	8081
- Inventory Service	http://localhost:8082
	8082
- Order Service	http://localhost:8083
	8083
- Kafka UI	http://localhost:7080
	7080

## 📚 API Documentation

Swagger UI is available for each service:

Product Service
http://localhost:8081/swagger-ui.html

Inventory Service
http://localhost:8082/swagger-ui.html

Order Service
http://localhost:8083/swagger-ui.html

## 🧪 Running Tests

Run tests for all services:

mvn test


Run tests for a specific service:

cd <service-directory>
mvn test

🧩 Project Structure

├── api-gateway/            # API Gateway service

├── product-service/        # Product management service

├── order-service/          # Order processing service

├── inventory-service/      # Inventory management service

├── init-database/          # Database initialization scripts

├── docker-compose.yml      # Docker Compose configuration

└── README.md               # Project documentation

## 🔄 Service Communication

Synchronous — REST APIs (HTTP/HTTPS)

Asynchronous — Apache Kafka (event-driven communication)

## 🧹 Clean Up

To stop and remove all containers, networks, and volumes:

docker-compose down -v