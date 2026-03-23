#!/bin/bash

##############################################################################
# Deployment Script for HikariCP Connection Timeout Fix
#
# This script automates the deployment of the fixed auction application
# with improved HikariCP connection pool configuration.
#
# Usage:
#   ./deploy.sh [dev|prod] [docker|native]
#
# Examples:
#   ./deploy.sh dev docker      # Deploy dev version to Docker
#   ./deploy.sh prod native     # Deploy prod version natively
#
##############################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-dev}
DEPLOYMENT_TYPE=${2:-docker}
PROJECT_NAME="auction"
BUILD_DIR="target"
JAR_FILE="auction-0.0.1-SNAPSHOT.jar"
APP_PORT=${PORT:-8080}
DOCKER_IMAGE="${PROJECT_NAME}-app:latest"
LOG_DIR="/var/log/${PROJECT_NAME}"

##############################################################################
# Functions
##############################################################################

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

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed"
        exit 1
    fi

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed"
        exit 1
    fi

    # Check Docker if needed
    if [ "$DEPLOYMENT_TYPE" = "docker" ]; then
        if ! command -v docker &> /dev/null; then
            log_error "Docker is not installed"
            exit 1
        fi
    fi

    log_success "All prerequisites met"
}

build_application() {
    log_info "Building application with Maven..."

    # Clean previous build
    mvn clean

    # Build with skip tests (tests run separately)
    if [ "$ENVIRONMENT" = "prod" ]; then
        mvn package -DskipTests -Dspring.profiles.active=prod
    else
        mvn package -DskipTests
    fi

    if [ ! -f "$BUILD_DIR/$JAR_FILE" ]; then
        log_error "Build failed: JAR file not found at $BUILD_DIR/$JAR_FILE"
        exit 1
    fi

    log_success "Application built successfully"
}

run_tests() {
    log_info "Running unit tests..."

    mvn test -Dspring.profiles.active=$ENVIRONMENT

    log_success "All tests passed"
}

create_log_directory() {
    log_info "Creating log directory..."

    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        chmod 755 "$LOG_DIR"
    fi

    log_success "Log directory ready at $LOG_DIR"
}

deploy_docker() {
    log_info "Deploying to Docker..."

    # Build Docker image
    log_info "Building Docker image: $DOCKER_IMAGE"
    docker build -t "$DOCKER_IMAGE" .

    # Stop existing container if running
    if docker ps -a --format '{{.Names}}' | grep -q "^${PROJECT_NAME}$"; then
        log_info "Stopping existing container..."
        docker stop $PROJECT_NAME || true
        docker rm $PROJECT_NAME || true
    fi

    # Run new container
    log_info "Starting new Docker container..."
    docker run -d \
        --name $PROJECT_NAME \
        -e SPRING_PROFILES_ACTIVE=$ENVIRONMENT \
        -e DB_URL="$DB_URL" \
        -e DB_USERNAME="$DB_USERNAME" \
        -e DB_PASSWORD="$DB_PASSWORD" \
        -e JWT_SECRET="$JWT_SECRET" \
        -p "$APP_PORT:8080" \
        --restart unless-stopped \
        --log-driver json-file \
        --log-opt max-size=10m \
        --log-opt max-file=3 \
        "$DOCKER_IMAGE"

    log_success "Docker deployment complete"
}

deploy_native() {
    log_info "Deploying native (non-Docker)..."

    # Create systemd service file
    SERVICE_FILE="/etc/systemd/system/${PROJECT_NAME}.service"

    log_info "Creating systemd service file..."
    sudo tee "$SERVICE_FILE" > /dev/null <<EOF
[Unit]
Description=Auction Application
After=network.target

[Service]
Type=simple
User=app
WorkingDirectory=/opt/${PROJECT_NAME}
ExecStart=/usr/bin/java -Xmx512m -Xms256m \\
    -Dspring.profiles.active=${ENVIRONMENT} \\
    -Dspring.application.name=${PROJECT_NAME} \\
    -jar ${JAR_FILE}
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    # Copy JAR to deployment directory
    log_info "Copying application JAR..."
    sudo mkdir -p /opt/$PROJECT_NAME
    sudo cp "$BUILD_DIR/$JAR_FILE" /opt/$PROJECT_NAME/
    sudo chown app:app /opt/$PROJECT_NAME/$JAR_FILE

    # Reload systemd and start service
    log_info "Starting application service..."
    sudo systemctl daemon-reload
    sudo systemctl enable $PROJECT_NAME
    sudo systemctl restart $PROJECT_NAME

    # Check status
    sleep 2
    if sudo systemctl is-active --quiet $PROJECT_NAME; then
        log_success "Service started successfully"
    else
        log_error "Failed to start service"
        sudo systemctl status $PROJECT_NAME
        exit 1
    fi
}

verify_deployment() {
    log_info "Verifying deployment..."

    sleep 3

    # Check health endpoint
    if [ "$DEPLOYMENT_TYPE" = "docker" ]; then
        HEALTH_URL="http://localhost:$APP_PORT/api/health/status"
    else
        HEALTH_URL="http://localhost:$APP_PORT/api/health/status"
    fi

    if curl -sf "$HEALTH_URL" > /dev/null; then
        log_success "Health check passed"
    else
        log_error "Health check failed"
        log_info "Trying diagnostic endpoint..."
        curl -v "$HEALTH_URL" || true
        exit 1
    fi

    # Check pool status
    log_info "Checking connection pool status..."
    curl -s "http://localhost:$APP_PORT/api/health/db-connections" | jq '.' || true

    log_success "Deployment verified"
}

show_post_deployment_info() {
    log_info "===== Deployment Complete ====="
    echo ""
    log_success "Application deployed successfully!"
    echo ""
    echo "Environment: $ENVIRONMENT"
    echo "Deployment Type: $DEPLOYMENT_TYPE"
    echo "Application Port: $APP_PORT"
    echo ""

    if [ "$DEPLOYMENT_TYPE" = "docker" ]; then
        echo "Docker Container: $PROJECT_NAME"
        echo "View logs: docker logs -f $PROJECT_NAME"
    else
        echo "Systemd Service: $PROJECT_NAME"
        echo "View logs: sudo journalctl -u $PROJECT_NAME -f"
        echo "Service status: sudo systemctl status $PROJECT_NAME"
    fi

    echo ""
    echo "Health Endpoints:"
    echo "  Status:      http://localhost:$APP_PORT/api/health/status"
    echo "  Connections: http://localhost:$APP_PORT/api/health/db-connections"
    echo "  Diagnostic:  http://localhost:$APP_PORT/api/health/db-diagnostic"
    echo ""
    echo "Documentation: See HIKARICP_*.md files in project root"
    echo ""
}

validate_environment() {
    if [ "$ENVIRONMENT" != "dev" ] && [ "$ENVIRONMENT" != "prod" ]; then
        log_error "Invalid environment: $ENVIRONMENT"
        log_info "Valid options: dev, prod"
        exit 1
    fi

    if [ "$DEPLOYMENT_TYPE" != "docker" ] && [ "$DEPLOYMENT_TYPE" != "native" ]; then
        log_error "Invalid deployment type: $DEPLOYMENT_TYPE"
        log_info "Valid options: docker, native"
        exit 1
    fi

    # For production, check required environment variables
    if [ "$ENVIRONMENT" = "prod" ]; then
        log_info "Validating production environment variables..."

        required_vars=("DB_URL" "DB_USERNAME" "DB_PASSWORD" "JWT_SECRET")
        for var in "${required_vars[@]}"; do
            if [ -z "${!var:-}" ]; then
                log_error "Required environment variable not set: $var"
                exit 1
            fi
        done

        log_success "All required environment variables set"
    fi
}

##############################################################################
# Main Execution
##############################################################################

main() {
    log_info "===== Auction Application Deployment ====="
    log_info "Environment: $ENVIRONMENT"
    log_info "Deployment Type: $DEPLOYMENT_TYPE"
    echo ""

    validate_environment
    check_prerequisites

    # Offer to run tests
    read -p "Run tests before deployment? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        run_tests
    fi

    build_application
    create_log_directory

    if [ "$DEPLOYMENT_TYPE" = "docker" ]; then
        deploy_docker
    else
        deploy_native
    fi

    verify_deployment
    show_post_deployment_info
}

# Trap errors
trap 'log_error "Deployment failed"; exit 1' ERR

# Run main function
main "$@"

