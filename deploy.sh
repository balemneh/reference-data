#!/bin/bash

# CBP Reference Data Service - Docker Deployment Script

set -e

echo "🚀 CBP Reference Data Service - Docker Deployment"
echo "================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check prerequisites
echo -e "\n📋 Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { print_error "Docker is required but not installed."; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { print_error "Docker Compose is required but not installed."; exit 1; }
print_status "Prerequisites checked"

# Parse command line arguments
ACTION=${1:-"up"}
PROFILE=${2:-"dev"}

case $ACTION in
    build)
        echo -e "\n🔨 Building services..."
        
        # Build backend
        echo "Building backend services..."
        docker-compose build --no-cache reference-api
        print_status "Backend built successfully"
        
        # Build frontend
        echo "Building frontend..."
        docker-compose build --no-cache admin-ui
        print_status "Frontend built successfully"
        ;;
        
    up)
        echo -e "\n🚀 Starting services..."
        
        # Start infrastructure services first
        echo "Starting infrastructure services..."
        docker-compose up -d postgres redis redpanda opensearch keycloak
        
        # Wait for services to be healthy
        echo "Waiting for services to be healthy..."
        sleep 10
        
        # Run database migrations
        echo "Running database migrations..."
        docker-compose up liquibase
        print_status "Database migrations completed"
        
        # Start application services
        echo "Starting application services..."
        docker-compose up -d reference-api admin-ui kafka-ui opensearch-dashboards
        
        print_status "All services started successfully"
        
        echo -e "\n📌 Service URLs:"
        echo "  - Admin UI: http://localhost:4200"
        echo "  - API: http://localhost:8080"
        echo "  - API Docs: http://localhost:8080/swagger-ui.html"
        echo "  - Keycloak: http://localhost:8085"
        echo "  - Kafka UI: http://localhost:8082"
        echo "  - OpenSearch Dashboards: http://localhost:5601"
        echo "  - Health Check: http://localhost:8080/actuator/health"
        ;;
        
    down)
        echo -e "\n🛑 Stopping services..."
        docker-compose down
        print_status "Services stopped"
        ;;
        
    clean)
        echo -e "\n🧹 Cleaning up..."
        docker-compose down -v
        docker system prune -f
        print_status "Cleanup completed"
        ;;
        
    status)
        echo -e "\n📊 Service Status:"
        docker-compose ps
        ;;
        
    logs)
        SERVICE=${2:-""}
        if [ -z "$SERVICE" ]; then
            docker-compose logs -f --tail=100
        else
            docker-compose logs -f --tail=100 $SERVICE
        fi
        ;;
        
    health)
        echo -e "\n🏥 Health Check:"
        
        # Check API health
        echo -n "API Health: "
        if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
            print_status "Healthy"
        else
            print_error "Unhealthy"
        fi
        
        # Check UI health
        echo -n "UI Health: "
        if curl -f -s http://localhost:4200/health > /dev/null; then
            print_status "Healthy"
        else
            print_error "Unhealthy"
        fi
        
        # Check database
        echo -n "Database: "
        if docker exec refdata-postgres pg_isready -U refdata_user > /dev/null 2>&1; then
            print_status "Ready"
        else
            print_error "Not ready"
        fi
        
        # Check Kafka
        echo -n "Kafka: "
        if docker exec refdata-kafka rpk cluster health 2>/dev/null | grep -q "Healthy.*true"; then
            print_status "Healthy"
        else
            print_error "Unhealthy"
        fi
        ;;
        
    seed)
        echo -e "\n🌱 Seeding sample data..."
        # Add seed data commands here
        docker exec refdata-api java -jar reference-api.jar --spring.profiles.active=seed
        print_status "Sample data seeded"
        ;;
        
    test)
        echo -e "\n🧪 Running tests..."
        docker-compose exec reference-api ./mvnw test
        print_status "Tests completed"
        ;;
        
    *)
        echo "Usage: $0 {build|up|down|clean|status|logs|health|seed|test} [service]"
        echo ""
        echo "Commands:"
        echo "  build   - Build Docker images"
        echo "  up      - Start all services"
        echo "  down    - Stop all services"
        echo "  clean   - Stop services and remove volumes"
        echo "  status  - Show service status"
        echo "  logs    - Show logs (optionally for specific service)"
        echo "  health  - Check service health"
        echo "  seed    - Seed sample data"
        echo "  test    - Run tests"
        exit 1
        ;;
esac

echo -e "\n✅ Done!"