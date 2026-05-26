@echo off
REM PostgreSQL Database Setup Script for Windows

echo Setting up PostgreSQL database for Resume Screener...

REM Create database
psql -U postgres -c "CREATE DATABASE resumescreener;" 2>nul

REM Verify database was created
psql -U postgres -c "\l" | find "resumescreener"

echo.
echo Database setup complete!
echo.
echo Now set environment variables in PowerShell or Command Prompt:
echo.
echo For PowerShell:
echo $env:DB_URL='jdbc:postgresql://localhost:5432/resumescreener'
echo $env:DB_USERNAME='postgres'
echo $env:DB_PASSWORD='postgres'
echo $env:HF_API_KEY='your_huggingface_api_key'
echo.
echo For Command Prompt (cmd.exe):
echo set DB_URL=jdbc:postgresql://localhost:5432/resumescreener
echo set DB_USERNAME=postgres
echo set DB_PASSWORD=postgres
echo set HF_API_KEY=your_huggingface_api_key
echo.
echo Then run: mvn clean package
