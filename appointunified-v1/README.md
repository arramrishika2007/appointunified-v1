# AppointUnified 🌌

<div align="center">
  <h3>Unified Smart Appointment Engine for Healthcare, Government & Services</h3>
  <p>Mathematically perfecting time allocation by eliminating unoptimized calendars.</p>
</div>

---

## 📖 Overview

**AppointUnified** is a modern, highly scalable full-stack multi-tenant platform architected to eliminate scheduling fragmentation. It acts as a universal bridge connecting everyday users directly to verified endpoint nodes across three major verticals: 
1. **Healthcare** (Clinics, Specialists, Diagnostics)
2. **Government Infrastructure** (Municipal Offices, Forms, Documentation)
3. **Services** (Mechanics, Electricians, Consultants)

By utilizing adaptive scheduling logic, geospatial node mapping, and real-time socket communications, AppointUnified allows users to securely authenticate, discover services, and seamlessly book appointments on a unified global grid.

---

## 🚀 Key Features

* **Role-Based Intelligence Dashboard:** Dedicated, distinct UX flows for `USER`, `PROFESSIONAL`, `ADMIN`, and `SUPER_ADMIN`.
* **Geospatial Discovery:** Map-based exploration (via Leaflet & Cobe WebGL) allows citizens to visually locate professional nodes closest to them.
* **Unified Online Booking Engine:** Real-time generation of available time blocks, preventing ghost bookings and calendar overlaps.
* **Frictionless Finance:** Automated and secure payment integrations using **Razorpay** for direct deposit management.
* **Real-Time Encrypted Chat:** Persistent socket layer allowing securely authenticated users to converse directly with their booked professionals.
* **Cryptographic Trust & Onboarding:** Backend-assisted secure document uploads via **Cloudinary API** to verify professionals before making them visible to the public index.
* **Pastel-Premium UI/UX:** A stunning, motion-driven frontend engineered with Tailwind CSS, Framer Motion, and Aceternity UI.

---

## 🛠️ Technology Stack

AppointUnified employs a decoupled, micro-service ready architecture leveraging industry-standard paradigms.

### **Frontend Interface**
- **Framework:** Next.js 14 (App Router) / React 18
- **Styling:** Tailwind CSS (Multi-Pastel Premium custom theme)
- **UI Components:** Shadcn UI, Aceternity UI, Radix Primitives
- **Animations:** Framer Motion, Cobe (WebGL Globe)
- **Data Fetching:** SWR (Stale-While-Revalidate)
- **Maps:** React-Leaflet

### **Backend Engine**
- **Core Framework:** Java 17 / Spring Boot 3
- **Security Context:** Spring Security Native + Stateless JWT Tokens
- **Database Migrations:** Flyway
- **Real-Time Layer:** Spring WebSocket / STOMP
- **Cloud Storage:** Cloudinary SDK v1.38+ (Secure Signed Uploads)
- **Payment Gateway:** Razorpay SDK 

### **Data & Infrastructure**
- **Relational Database:** PostgreSQL (Core Entities, Transactions, Appointments)
- **NoSQL Database:** MongoDB (High velocity real-time Chat messages)
- **Caching Layer:** Redis (Session acceleration, transient state)

---

## 🏗️ Architecture & Security

### Secure Vault Architecture
AppointUnified heavily separates public vs. authenticated concerns:
- Custom `@PreAuthorize` routing in Spring restricts controllers based on Enum (`ROLE_USER`, `ROLE_ADMIN`, etc).
- Asset uploading utilizes a **Two-Tier Signature Protocol**: The frontend never holds the Cloudinary Secret; instead, it requests an ephemeral cryptographic signature from the Spring backend to upload files directly.

### Data Model
- **Professionals** contain geographic points, service types, and verification objects.
- **Appointments** act as the central source of truth locking a User, Professional, Time-range, and Payment Order.
- **Reviews** and **Chats** are tightly coupled to completed or active Appointments.

---

## 🚦 Getting Started

### Prerequisites
- Node.js 18.x or later
- Java JDK 17
- PostgreSQL (running locally or via Docker)
- Redis Server
- MongoDB

### 1. Backend Setup
Navigate to the backend directory:
```bash
cd backend
```
Define your `application.properties` (or `.env`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/appointunified
spring.datasource.username=postgres
spring.datasource.password=password
spring.data.mongodb.uri=mongodb://localhost:27017/appointunified_chat
jwt.secret=YOUR_JWT_SECRET
cloudinary.cloud_name=...
cloudinary.api_key=...
cloudinary.api_secret=...
razorpay.key_id=...
razorpay.key_secret=...
```
Initialize the Spring Boot Maven/Gradle server to apply Flyway migrations and spin up the API.

### 2. Frontend Setup
Navigate to the frontend directory:
```bash
cd frontend
```
Install dependencies and configure your `.env.local`:
```bash
npm install
```
Configure `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME=...
NEXT_PUBLIC_CLOUDINARY_UPLOAD_PRESET=...
```
Run the local development server:
```bash
npm run dev
```

---
<div align="center">
  <p>Engineered for the Modern Ecosystem. Developed by the AppointUnified Core Team.</p>
</div>
