#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Error handling
set -e
trap 'log_error "Script failed at line $LINENO"' ERR

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

log_info "Starting Warehouse Management System setup..."
echo ""

# Check if Docker is installed
log_info "Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed. Please install Docker first."
    echo "Installation guide: https://docs.docker.com/get-docker/"
    exit 1
fi
log_success "Docker is installed"

# Check if Docker daemon is running
log_info "Checking Docker daemon status..."
if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running. Please start Docker first."
    echo "On Linux: sudo systemctl start docker"
    echo "On macOS/Windows: Start Docker Desktop"
    exit 1
fi
log_success "Docker daemon is running"

# Check for docker-compose or docker compose
log_info "Checking Docker Compose availability..."
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
    log_success "Found docker-compose command"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
    log_success "Found docker compose command (v2)"
else
    log_error "Docker Compose is not installed. Please install Docker Compose first."
    echo "Installation guide: https://docs.docker.com/compose/install/"
    exit 1
fi

# Check if Java and Maven are installed (for building)
log_info "Checking build prerequisites..."
if ! command -v java &> /dev/null; then
    log_warning "Java is not installed. Skipping Maven build."
    log_warning "Make sure the application JAR is already built or build it manually."
    BUILD_APP=false
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    log_info "Java version: $JAVA_VERSION"
    
    if ! command -v mvn &> /dev/null; then
        log_warning "Maven is not installed. Skipping Maven build."
        BUILD_APP=false
    else
        MVN_VERSION=$(mvn -version | head -n 1)
        log_info "Maven: $MVN_VERSION"
        BUILD_APP=true
    fi
fi

# Build application if prerequisites are available
if [ "$BUILD_APP" = true ]; then
    log_info "Building application with Maven..."
    if mvn clean package -DskipTests; then
        log_success "Application built successfully"
    else
        log_error "Maven build failed"
        exit 1
    fi
else
    log_warning "Skipping application build. Using existing JAR if available."
fi

# Check if required ports are available
log_info "Checking port availability..."
PORTS=(9009 5432 6379 9092 2181 8081 9200 5601 5000 9600)
PORTS_IN_USE=()

for port in "${PORTS[@]}"; do
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -tuln 2>/dev/null | grep -q ":$port "; then
        PORTS_IN_USE+=($port)
    fi
done

if [ ${#PORTS_IN_USE[@]} -gt 0 ]; then
    log_warning "The following ports are already in use: ${PORTS_IN_USE[*]}"
    log_warning "This may cause conflicts. Please stop services using these ports or modify docker-compose.yml"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Setup cancelled by user"
        exit 0
    fi
else
    log_success "All required ports are available"
fi

# Build Docker image
log_info "Building Docker image..."
if docker build -t warehouse:latest .; then
    log_success "Docker image built successfully"
else
    log_error "Docker image build failed"
    exit 1
fi

# Create Docker network if it doesn't exist
log_info "Creating Docker network..."
if docker network inspect backend-network &> /dev/null; then
    log_warning "Network 'backend-network' already exists"
else
    if docker network create backend-network; then
        log_success "Network 'backend-network' created"
    else
        log_error "Failed to create network"
        exit 1
    fi
fi

# Start services with docker-compose
log_info "Starting services with Docker Compose..."
if $DOCKER_COMPOSE up -d; then
    log_success "Services started successfully"
else
    log_error "Failed to start services"
    exit 1
fi

# Wait for services to be healthy
log_info "Waiting for services to be healthy..."
sleep 5

# Check service status
log_info "Checking service status..."
$DOCKER_COMPOSE ps

echo ""
log_success "Setup completed successfully!"
echo ""
log_info "Service URLs:"
echo "  - API: http://localhost:9009"
echo "  - Kafka UI: http://localhost:8081 (admin/admin123)"
echo "  - Kibana: http://localhost:5601"
echo "  - Elasticsearch: http://localhost:9200"
echo ""
log_info "To view logs: $DOCKER_COMPOSE logs -f"
log_info "To stop services: $DOCKER_COMPOSE down"
log_info "To restart services: $DOCKER_COMPOSE restart"
echo ""
