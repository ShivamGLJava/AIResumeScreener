#!/bin/bash

# PostgreSQL Database Setup Script

echo "Setting up PostgreSQL database for Resume Screener..."

# Create database
psql -U postgres -c "CREATE DATABASE resumescreener;" 2>/dev/null || echo "Database 'resumescreener' already exists or error occurred"

# Verify database was created
psql -U postgres -c "\l" | grep resumescreener

echo ""
echo "Database setup complete!"
echo ""
echo "Now set environment variables and run the application:"
echo ""
echo "On Linux/Mac:"
echo "export DB_URL='jdbc:postgresql://localhost:5432/resumescreener'"
echo "export DB_USERNAME='postgres'"
echo "export DB_PASSWORD='postgres'"
echo "export HF_API_KEY='your_huggingface_api_key'"
echo ""
echo "On Windows (PowerShell):"
echo "\$env:DB_URL='jdbc:postgresql://localhost:5432/resumescreener'"
echo "\$env:DB_USERNAME='postgres'"
echo "\$env:DB_PASSWORD='postgres'"
echo "\$env:HF_API_KEY='your_huggingface_api_key'"
echo ""
echo "Then run: mvn clean package"
