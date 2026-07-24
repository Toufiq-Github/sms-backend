# DMS — Data Management & Reporting System

A full-stack enterprise-style web application for secure document upload, automated data extraction, structured storage, and report generation — built with Spring Boot, Angular, and PostgreSQL.

---

# 🧭 Overview

DMS (Data Management & Reporting System) is an enterprise-style full-stack application designed to simplify document processing, structured data storage, and automated reporting.

The system allows authenticated users to upload PDF, Excel, and CSV files. Instead of only storing uploaded files, DMS automatically reads and parses file contents, detects data types (String, Integer, Float), and stores extracted data into PostgreSQL using a **schema-based dynamic table architecture**.

Files with identical structures reuse existing database tables, while structurally different files are stored separately in isolated dynamic tables.

Users can preview uploaded data, view processed datasets, filter records by date, generate reports, and permanently delete their uploaded data.

Administrators have additional capabilities including user management, account activation/deactivation, role management, and complete system monitoring.

---

# ✨ Features

## 🔐 Authentication & Security

- JWT-based authentication system
- Secure user registration and login
- BCrypt password hashing
- Stateless authentication using JWT tokens
- Protected API endpoints using Spring Security
- Role-based access control (Admin/User)

---

## 📂 File Management

- Upload multiple files simultaneously
- Supported formats:
  - Excel (.xlsx, .xls)
  - CSV
  - PDF

- Automatic file processing
- File metadata storage
- Upload history tracking
- User-specific file ownership
- Permanent file deletion

---

## 📊 Automated Data Extraction

- Reads uploaded documents automatically
- Extracts structured information
- Detects column data types:
  - String
  - Integer
  - Float

- Provides instant preview before saving
- Supports large datasets through dynamic processing

---

## 🗄️ Dynamic Database Architecture

The system uses a schema-based dynamic table approach.

Features:

- Creates database tables dynamically based on uploaded file structure
- Detects duplicate structures
- Reuses existing tables when column structures match
- Creates separate tables for different schemas
- Maintains dataset metadata and relationships

Example:
