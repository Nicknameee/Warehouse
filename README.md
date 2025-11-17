# Warehouse Management System

A comprehensive enterprise-grade warehouse management system built with Spring Boot, designed to handle inventory tracking, shipments, financial transactions, and analytics for multi-warehouse operations.

## 🎯 Project Goal

The Warehouse Management System provides a complete solution for managing warehouse operations, including:
- Multi-warehouse inventory management
- Product catalog and stock tracking
- Shipment processing (internal and external)
- Financial transaction management
- Real-time analytics and reporting
- User and role-based access control

The system is designed to support businesses with multiple warehouse locations, enabling efficient inventory management, shipment tracking, and financial operations across the entire network.

## ✨ Key Features

### 🔐 Authentication & Authorization
- JWT-based authentication
- Role-based access control (OWNER, MANAGER, OPERATOR)
- User management with status tracking
- Secure token management with blacklisting

### 🏢 Warehouse Management
- Multi-warehouse support
- Warehouse details with addresses and working hours
- Storage section management
- Manager assignment
- Active/inactive status management

### 📦 Product Catalog
- Product management with unique codes
- Product categorization with tags
- Multiple photo support
- Price and currency management
- Product dimensions and weight tracking
- Advanced search and filtering

### 📊 Stock Management
- Real-time stock tracking across warehouses
- Stock item grouping and categorization
- Expiry date tracking
- Stock status management (AVAILABLE, OUT_OF_STOCK, OUT_OF_SERVICE)
- Stock movement history (immutable audit log)
- Multi-warehouse stock visibility

### 🚚 Shipment Management
- Internal shipments (warehouse-to-warehouse)
- External shipments with address management
- Shipment status tracking (PLANNED → INITIATED → SENT → DELIVERED)
- Rollback functionality
- Shipment direction tracking (INCOMING/OUTCOMING)
- Comprehensive shipment filtering

### 💰 Financial Management
- Transaction processing (incoming/outgoing)
- Multiple payment providers support (Google Pay, DataTrans, LiqPay, Cash)
- Beneficiary (financial account) management
- Transaction status tracking (INITIATED, SETTLED, FAILED, CANCELLED)
- Multiple currency support (USD, EUR, CHF, UAH)
- Currency rate caching
- Financial flow statistics

### 📈 Analytics & Reporting
- Item selling statistics
- Beneficiary financial flow statistics
- Sales volume and revenue tracking
- Date range filtering
- Export capabilities

### 🏷️ Supporting Features
- Tag management for product categorization
- Stock item group management
- Storage section organization
- Enum value endpoints for UI dropdowns
- Asynchronous processing via Kafka
- Real-time currency rate updates

## 🛠️ Technology Stack

### Backend Framework
- **Spring Boot 3.5.3** - Application framework
- **Java 23** - Programming language
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **Hibernate 7.0.5** - ORM framework

### Database & Caching
- **PostgreSQL 16** - Primary relational database
- **Redis 7.0** - Caching and session management
- **Flyway 11.10.2** - Database migration tool

### Messaging & Streaming
- **Apache Kafka 7.6.0** - Asynchronous message processing
- **Zookeeper** - Kafka coordination

### Monitoring & Logging
- **ELK Stack** (Elasticsearch, Logstash, Kibana 7.14.1) - Log aggregation and analysis
- **Spring Actuator** - Application monitoring and health checks

### Security
- **JWT (JJWT 0.11.5)** - Token-based authentication
- **Spring Security** - Security framework

### Additional Libraries
- **Lombok** - Boilerplate code reduction
- **MapStruct 1.5.2** - Object mapping
- **Jakarta Validation** - Input validation
- **Jackson** - JSON processing

### Infrastructure
- **Docker & Docker Compose** - Containerization and orchestration
- **Maven** - Build and dependency management

## 📋 Prerequisites

- **Java 23** or higher
- **Maven 3.6+**
- **Docker** and **Docker Compose**
- **PostgreSQL 16** (or use Docker)
- **Redis 7.0** (or use Docker)
- **Kafka** (or use Docker)

## 📥 Installation

### Step 1: Install Java 23

**Linux (Ubuntu/Debian):**
```bash
# Add OpenJDK repository
sudo apt update
sudo apt install openjdk-23-jdk

# Verify installation
java -version
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install java-23-openjdk-devel
java -version
```

**macOS:**
```bash
# Using Homebrew
brew install openjdk@23

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@23/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

java -version
```

**Windows:**
1. Download OpenJDK 23 from [Adoptium](https://adoptium.net/)
2. Install the MSI package
3. Set `JAVA_HOME` environment variable
4. Add Java to PATH

### Step 2: Install Maven

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
mvn -version
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install maven
mvn -version
```

**macOS:**
```bash
brew install maven
mvn -version
```

**Windows:**
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Extract to a directory (e.g., `C:\Program Files\Apache\maven`)
3. Set `MAVEN_HOME` environment variable
4. Add `%MAVEN_HOME%\bin` to PATH

### Step 3: Install Docker

**Linux (Ubuntu/Debian):**
```bash
# Remove old versions
sudo apt remove docker docker-engine docker.io containerd runc

# Install prerequisites
sudo apt update
sudo apt install ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group (optional, to run without sudo)
sudo usermod -aG docker $USER
# Log out and log back in for group changes to take effect

# Verify installation
docker --version
docker compose version
```

**Linux (Fedora/RHEL):**
```bash
# Install Docker
sudo dnf install docker docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add user to docker group
sudo usermod -aG docker $USER

docker --version
```

**macOS:**
1. Download [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)
2. Install the DMG package
3. Start Docker Desktop from Applications
4. Verify installation:
   ```bash
   docker --version
   docker compose version
   ```

**Windows:**
1. Download [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)
2. Install the installer
3. Start Docker Desktop
4. Enable WSL 2 backend if prompted
5. Verify installation:
   ```bash
   docker --version
   docker compose version
   ```

### Step 4: Verify Docker Daemon

**Linux:**
```bash
# Check if Docker daemon is running
sudo systemctl status docker

# If not running, start it
sudo systemctl start docker
```

**macOS/Windows:**
- Ensure Docker Desktop is running (check system tray/status bar)

**Verify Docker is working:**
```bash
docker info
```

### Step 5: Clone the Repository

```bash
git clone <repository-url>
cd Warehouse
```

### Step 6: Make Script Executable (Linux/macOS)

```bash
chmod +x warehouse.sh
```

## 🚀 Quick Start

### Using Automated Script (Recommended)

The `warehouse.sh` script automates the entire setup process:

```bash
# Make script executable (Linux/macOS)
chmod +x warehouse.sh

# Run the script
./warehouse.sh
# or with sudo if needed
sudo ./warehouse.sh
```

The script will:
- ✅ Check Docker installation and daemon status
- ✅ Verify Docker Compose availability
- ✅ Check Java and Maven (optional, for building)
- ✅ Build the application with Maven (if available)
- ✅ Check port availability
- ✅ Build Docker image
- ✅ Create Docker network
- ✅ Start all services
- ✅ Display service status and URLs

### Using Docker Compose (Manual)

1. **Create Docker network**
   ```bash
   docker network create backend-network
   ```

2. **Build the application**
   ```bash
   mvn clean package
   docker build -t warehouse:latest .
   ```

3. **Start all services**
   ```bash
   docker-compose up -d
   # or with Docker Compose v2:
   docker compose up -d
   ```

4. **Access the application**
   - API: http://localhost:9009
   - Kafka UI: http://localhost:8081 (admin/admin123)
   - Kibana: http://localhost:5601
   - Elasticsearch: http://localhost:9200

### Manual Setup (Without Docker)

1. **Install and start required services manually**
   - PostgreSQL 16
   - Redis 7.0
   - Kafka 7.6.0 with Zookeeper

2. **Create database**
   ```bash
   createdb warehouse
   ```

3. **Configure environment variables**
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/warehouse
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=1904
   export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   export SPRING_REDIS_HOST=localhost
   export SPRING_REDIS_PORT=6379
   export SPRING_REDIS_PASSWORD=1904
   ```

4. **Run database migrations**
   ```bash
   # Migrations run automatically on application start via Flyway
   ```

5. **Build and run the application**
   ```bash
   mvn clean package
   java -jar target/warehouse-1.0.0.jar
   # or
   mvn spring-boot:run
   ```

### Troubleshooting Installation

**Docker daemon not running:**
```bash
# Linux
sudo systemctl start docker
sudo systemctl status docker

# macOS/Windows: Start Docker Desktop application
```

**Port already in use:**
```bash
# Check what's using the port
sudo lsof -i :9009
# or
sudo netstat -tulpn | grep 9009

# Stop the service or change port in docker-compose.yml
```

**Network creation fails:**
```bash
# Check if network exists
docker network ls

# Remove existing network if needed
docker network rm backend-network

# Create network again
docker network create backend-network
```

**Maven build fails:**
- Ensure Java 23 is installed and JAVA_HOME is set
- Check Maven version: `mvn -version`
- Clear Maven cache: `mvn clean`

**Docker build fails:**
- Ensure Docker daemon is running
- Check disk space: `df -h`
- Try rebuilding: `docker build --no-cache -t warehouse:latest .`

## 📁 Project Structure

```
Warehouse/
├── src/
│   ├── main/
│   │   ├── java/io/store/ua/
│   │   │   ├── configuration/      # Spring configuration classes
│   │   │   ├── controllers/        # REST API controllers
│   │   │   ├── entity/             # JPA entities
│   │   │   ├── enums/              # Enumeration types
│   │   │   ├── events/             # Application events
│   │   │   ├── exceptions/         # Custom exceptions
│   │   │   ├── handlers/           # Exception handlers
│   │   │   ├── mappers/            # MapStruct mappers
│   │   │   ├── models/             # DTOs and data models
│   │   │   ├── producers/          # Kafka producers
│   │   │   ├── consumers/          # Kafka consumers
│   │   │   ├── repository/         # JPA repositories
│   │   │   ├── service/            # Business logic services
│   │   │   ├── utility/            # Utility classes
│   │   │   ├── validations/        # Custom validators
│   │   │   └── WarehouseStarter.java
│   │   └── resources/
│   │       ├── application*.yml    # Configuration files
│   │       ├── migrations/         # Flyway database migrations
│   │       └── logback-spring.xml  # Logging configuration
│   └── test/                       # Test classes
├── docker-compose.yml              # Docker services configuration
├── Dockerfile                      # Application Docker image
├── pom.xml                         # Maven dependencies
└── api-documentation.json          # Complete API documentation
```

## 🔌 API Documentation

Complete API documentation is available in `api-documentation.json`, including:
- All endpoints with request/response schemas
- Authentication requirements
- Query parameters and filters
- Data models and types
- Enum values
- Usage examples

### Key API Endpoints

**Authentication:**
- `POST /login` - User authentication
- `POST /logout` - Token invalidation

**Resources:**
- `GET /vars/*` - Enum values for UI dropdowns

**Core Entities:**
- `/api/v1/warehouses` - Warehouse management
- `/api/v1/products` - Product catalog
- `/api/v1/stockItems` - Stock management
- `/api/v1/shipments` - Shipment tracking
- `/api/v1/transactions` - Financial transactions
- `/api/v1/beneficiaries` - Financial accounts
- `/api/v1/users` - User management
- `/api/v1/analytics` - Analytics and statistics

All endpoints support:
- Pagination (page, pageSize)
- Advanced filtering
- Search capabilities
- Role-based access control

## 🔒 Security

- **JWT Authentication**: All API endpoints (except `/login` and `/vars/*`) require authentication
- **Role-Based Access Control**: 
  - **OWNER**: Full system access
  - **MANAGER**: Management operations
  - **OPERATOR**: Basic operations
- **Token Blacklisting**: Secure logout with token invalidation
- **Input Validation**: Comprehensive validation on all inputs
- **SQL Injection Protection**: Parameterized queries via JPA

## 📊 Database Schema

The system uses Flyway for database migrations. Key tables include:
- `users` - User accounts and authentication
- `warehouses` - Warehouse information
- `products` - Product catalog
- `stock_items` - Inventory tracking
- `stock_item_groups` - Product categorization
- `storage_sections` - Warehouse organization
- `shipments` - Shipment records
- `transactions` - Financial transactions
- `beneficiaries` - Financial accounts
- `tags` - Product tags
- `stock_item_history` - Immutable audit log

## 🔄 Asynchronous Processing

The system uses Kafka for asynchronous processing:
- **Shipment Processing**: Shipments can be queued for async processing
- **Transaction Processing**: Financial transactions can be processed asynchronously
- **Event-Driven Architecture**: Supports event-driven workflows

## 📈 Monitoring

- **Spring Actuator**: Health checks and metrics at `/actuator`
- **ELK Stack**: Centralized logging and log analysis
- **Kafka UI**: Kafka topic monitoring and management

## 🧪 Testing

The project includes comprehensive test coverage:
- Unit tests for services
- Integration tests for controllers
- Repository tests
- Security tests

Run tests with:
```bash
mvn test
```

## 🌐 Internationalization

The API supports multiple currencies:
- USD (US Dollar)
- EUR (Euro)
- CHF (Swiss Franc)
- UAH (Ukrainian Hryvnia)

Currency rates are cached in Redis and updated periodically.

## 📝 Configuration

Configuration is managed through Spring profiles:
- `default` - Base configuration
- `actuator` - Monitoring endpoints
- `database` - Database configuration
- `external` - External service configuration
- `kafka` - Kafka configuration
- `redis` - Redis configuration

Environment variables can override default values in `application.yml`.

## 🐳 Docker Services

The `docker-compose.yml` includes:
- **PostgreSQL** - Database server
- **Redis** - Cache and session store
- **Kafka + Zookeeper** - Message broker
- **Kafka UI** - Kafka management interface
- **Elasticsearch** - Log storage
- **Logstash** - Log processing
- **Kibana** - Log visualization
- **Warehouse Application** - Main application

## 📚 Additional Resources

- **Front-End Plan**: See `front-end-plan.md` for Next.js front-end development plan
- **API Documentation**: Complete API reference in `api-documentation.json`

## 🤝 Contributing

1. Follow Java coding standards
2. Write tests for new features
3. Update API documentation
4. Ensure all tests pass
5. Follow the existing code structure

## 📄 License

[Specify your license here]

## 👥 Authors

[Specify authors here]

## 🙏 Acknowledgments

Built with Spring Boot and modern Java technologies.

---

For detailed API documentation, see `api-documentation.json`.
For front-end development plan, see `front-end-plan.md`.

