# DMS — Data Management & Reporting System

A full-stack enterprise-style web application for secure document upload, automated data extraction, structured storage, and report generation — built with Spring Boot, Angular, and PostgreSQL.

## 🧭 Overview

DMS allows authenticated users to upload PDF, Excel, and CSV files. Instead of just storing files, the system **reads and parses their content**, automatically detects data types (String, Integer, Float), and persists the extracted data into PostgreSQL using a **schema-based dynamic table architecture** — files sharing the same structure reuse a table, while structurally different files get isolated tables.

Users can preview, view, filter by date, and permanently delete their data. Admins get dedicated user management capabilities. The system also generates downloadable PDF/Excel reports from stored data.

---

## ✨ Features

- **JWT-based Authentication** — secure registration/login with BCrypt password hashing
- **Role-Based Access Control** — Admin vs. User permissions enforced at the API level
- **Multi-format File Upload** — PDF, Excel (.xlsx/.xls), CSV, with multi-file support
- **Automated Data Extraction** — reads file contents and detects data types automatically
- **Dynamic Schema-Based Storage** — one table per unique data structure; identical structures reuse existing tables
- **Instant Data Preview** — parsed data displayed immediately after upload
- **Standalone Data Viewer** — view any previously uploaded file's data in a new window
- **Date-based Filtering** — filter uploads by year/month/day
- **Permanent Deletion** — removes both the file record and its underlying stored data
- **Report Generation** — export PDF and Excel reports from live data
- **Per-user Data Isolation** — each user only sees and manages their own files/reports
- **Admin Panel** — user management, activation/deactivation, promotion, deletion

---

# 🏗️ Architecture
