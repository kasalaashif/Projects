# Event-Driven Order Processing System (Microservices)

## Project Overview

This project implements a scalable e-commerce system using microservices architecture and event-driven design patterns. The system handles the complete lifecycle of order processing, from order creation to notification delivery, ensuring reliability and consistency across distributed services.

### Key Features

- **Event-Driven Architecture**: Utilizes Apache Kafka for asynchronous communication between services
- **Distributed Transaction Management**: Implements the Saga pattern for maintaining data consistency
- **Scalable Design**: Each service can be scaled independently based on load
- **Fault Tolerance**: Implements circuit breakers and retry mechanisms
- **Real-time Notifications**: Supports both email and SMS notifications
- **Data Consistency**: Uses event sourcing for maintaining data consistency across services

## Prerequisites

### Required Tools

1. **Java Development Kit (JDK)**
   - Version: 17 or higher
   - Installation:
     ```bash
     # For macOS (using Homebrew)
     brew install openjdk@17
     
     # For Ubuntu
     sudo apt-get install openjdk-17-jdk
     ```

2. **Maven**
   - Version: 3.6 or higher
   - Installation:
     ```bash
     # For macOS
     brew install maven
     
     # For Ubuntu
     sudo apt-get install maven
     ```

3. **Docker & Docker Compose**
   - Installation:
     ```bash
     # For macOS
     brew install docker docker-compose
     
     # For Ubuntu
     sudo apt-get install docker.io docker-compose
     ```

4. **Apache Kafka**
   - Version: 3.0 or higher
   - Installation via Docker will be covered in setup instructions

## Architecture Overview

### High-Level Architecture
```mermaid
graph TB
    Client[Client Applications] --> API[API Gateway]
    API --> OS[Order Service]
    API --> PS[Payment Service]
    API --> IS[Inventory Service]
    API --> NS[Notification Service]
    
    OS --> |Events| KB[Kafka Broker]
    PS --> |Events| KB
    IS --> |Events| KB
    KB --> NS
    
    subgraph Data Stores
        OS --> OrderDB[(Order DB)]
        PS --> PaymentDB[(Payment DB)]
        IS --> InventoryDB[(Inventory DB)]
        NS --> NotificationDB[(Notification DB)]
    end

    subgraph Event Types
        KB --- OE[Order Events]
        KB --- PE[Payment Events]
        KB --- IE[Inventory Events]
        KB --- NE[Notification Events]
    end
```

### Service Communication Flow
```mermaid
sequenceDiagram
    participant C as Client
    participant OS as Order Service
    participant PS as Payment Service
    participant IS as Inventory Service
    participant NS as Notification Service
    
    C->>OS: Create Order
    OS->>IS: Check Inventory
    IS-->>OS: Inventory Status
    OS->>PS: Process Payment
    PS-->>OS: Payment Status
    PS->>NS: Payment Notification
    OS->>NS: Order Status Update
    NS-->>C: Email/SMS Notification
```

## Low Level Design

### Class Diagrams

#### Order Service
```mermaid
classDiagram
    class Order {
        -Long orderId
        -String customerId
        -List~OrderItem~ items
        -OrderStatus status
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +calculateTotal()
        +updateStatus()
    }
    
    class OrderItem {
        -Long itemId
        -String productId
        -int quantity
        -BigDecimal price
        +calculateSubtotal()
    }
    
    class OrderService {
        -OrderRepository orderRepo
        -KafkaTemplate kafka
        +createOrder()
        +updateOrder()
        +cancelOrder()
        +getOrderById()
    }
    
    class OrderRepository {
        +save()
        +findById()
        +findByCustomerId()
    }
    
    Order "1" *-- "*" OrderItem
    OrderService --> OrderRepository
    OrderService --> Order
```

#### Payment Service
```mermaid
classDiagram
    class Payment {
        -Long paymentId
        -Long orderId
        -BigDecimal amount
        -PaymentStatus status
        -String transactionId
        -LocalDateTime processedAt
    }
    
    class PaymentProcessor {
        -validatePayment()
        -processPayment()
        -handleRefund()
    }
    
    class PaymentService {
        -PaymentRepository paymentRepo
        -PaymentProcessor processor
        -KafkaTemplate kafka
        +processPayment()
        +refundPayment()
        +getPaymentStatus()
    }
    
    PaymentService --> PaymentProcessor
    PaymentService --> Payment
```

#### Inventory Service
```mermaid
classDiagram
    class Product {
        -String productId
        -String name
        -int quantity
        -BigDecimal price
        +updateStock()
        +reserveStock()
    }
    
    class InventoryService {
        -ProductRepository productRepo
        -KafkaTemplate kafka
        +checkAvailability()
        +updateInventory()
        +reserveInventory()
    }
    
    class StockReservation {
        -String reservationId
        -String productId
        -int quantity
        -LocalDateTime expiresAt
    }
    
    InventoryService --> Product
    InventoryService --> StockReservation
```

#### Notification Service
```mermaid
classDiagram
    class Notification {
        -Long notificationId
        -String recipient
        -NotificationType type
        -String content
        -NotificationStatus status
        -LocalDateTime sentAt
    }
    
    class EmailNotifier {
        -EmailConfig config
        +sendEmail()
    }
    
    class SMSNotifier {
        -SMSConfig config
        +sendSMS()
    }
    
    class NotificationService {
        -NotificationRepository notificationRepo
        -EmailNotifier emailNotifier
        -SMSNotifier smsNotifier
        +sendNotification()
        +retryFailedNotifications()
    }
    
    NotificationService --> EmailNotifier
    NotificationService --> SMSNotifier
    NotificationService --> Notification
```

### Database Schemas

#### Order Database
```sql
CREATE TABLE orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

#### Payment Database
```sql
CREATE TABLE payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(100),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refunds (
    refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reason VARCHAR(200),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
);
```

#### Inventory Database
```sql
CREATE TABLE products (
    product_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE stock_reservations (
    reservation_id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```

#### Notification Database
```sql
CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_templates (
    template_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subject VARCHAR(200),
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL
);
```

### Component Interactions

#### Order Creation Flow
1. **API Gateway** receives order creation request
2. **Order Service**:
   - Validates order data
   - Creates order record with PENDING status
   - Publishes OrderCreatedEvent
3. **Inventory Service**:
   - Consumes OrderCreatedEvent
   - Checks product availability
   - Creates stock reservation
   - Publishes StockReservedEvent
4. **Payment Service**:
   - Consumes StockReservedEvent
   - Processes payment
   - Publishes PaymentProcessedEvent
5. **Order Service**:
   - Updates order status based on PaymentProcessedEvent
   - Publishes OrderConfirmedEvent
6. **Notification Service**:
   - Sends confirmation to customer
   - Updates notification status

#### Error Handling Flow
1. **Service Exception**:
   - Publishes ErrorEvent to dedicated error topic
   - Implements retry mechanism with exponential backoff
2. **Circuit Breaker**:
   - Monitors service health
   - Opens circuit on repeated failures
   - Implements fallback mechanisms
3. **Compensation Transactions**:
   - Reverses completed operations
   - Maintains system consistency
   - Publishes compensation events

## Setup and Installation

### 1. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd event-driven-order-processing

# Build all services
mvn clean install
```

### 2. Database Setup

```bash
# Start H2 Database (Development)
# H2 console will be available at http://localhost:8081/h2-console

# For Production (PostgreSQL)
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

### 3. Start Kafka

```bash
# Using Docker Compose
docker-compose up -d kafka zookeeper

# Verify Kafka is running
docker ps
```

### 4. Start Services

```bash
# Start each service in separate terminals

# Notification Service
cd notification-service
mvn spring-boot:run

# Order Service
cd order-service
mvn spring-boot:run

# Payment Service
cd payment-service
mvn spring-boot:run

# Inventory Service
cd inventory-service
mvn spring-boot:run
```

## Testing the Services

### 1. Notification Service Endpoints

#### Create Notification
```bash
curl -X POST "http://localhost:8081/api/notifications" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 12345,
    "customerId": "CUST123",
    "type": "EMAIL",
    "recipient": "test@example.com",
    "subject": "Order Confirmation",
    "content": "Your order has been confirmed"
  }'
```

#### Send Email
```bash
curl -X POST "http://localhost:8081/api/notifications/email" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "subject": "Test Email",
    "content": "This is a test email"
  }'
```

#### Send SMS
```bash
curl -X POST "http://localhost:8081/api/notifications/sms" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "+1234567890",
    "content": "This is a test SMS"
  }'
```

#### Get Notifications by Order ID
```bash
curl "http://localhost:8081/api/notifications/order/12345"
```

### 2. Monitoring

- **H2 Console**: http://localhost:8081/h2-console
- **Actuator Endpoints**: http://localhost:8081/actuator
- **Swagger UI**: http://localhost:8081/swagger-ui.html

## Error Handling

The system implements robust error handling:

1. **Retry Mechanism**
   - Failed notifications are automatically retried
   - Configurable retry count and delay
   - Dead Letter Queue for failed messages

2. **Circuit Breaker**
   - Prevents cascade failures
   - Automatic service degradation
   - Fallback mechanisms

3. **Error Logging**
   - Detailed error logging
   - Error tracking and monitoring
   - Alert mechanisms for critical failures

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check ports in use
   lsof -i :<port-number>
   
   # Kill process using port
   kill -9 <PID>
   ```

2. **Database Connection Issues**
   - Verify H2 console is accessible
   - Check database credentials
   - Ensure database service is running

3. **Kafka Connection Issues**
   - Verify Kafka and Zookeeper are running
   - Check Kafka connection properties
   - Ensure topics are created properly

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Spring Boot and Spring Cloud teams
- Apache Kafka community
- All contributors to this project 