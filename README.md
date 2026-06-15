# ExpenseWise - Backend

ExpenseWise Backend is a robust, secure, and high-performance RESTful API built to power the ExpenseWise personal finance application. It handles user authentication, budget management, transaction processing, and savings goal tracking.

## ✨ Features

- **JWT Authentication:** Secure, stateless token-based authentication.
- **Strong Security:** Strict password enforcement and encrypted password storage.
- **Email Integration:** Automated email delivery for user registration via Google SMTP.
- **Relational Data:** Complex relational mappings between Users, Categories, Transactions, and Budgets using Hibernate/JPA.
- **Docker Ready:** Includes a multi-stage Dockerfile for instant cloud deployment.

## 🛠️ Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.3.0
- **Security:** Spring Security + JSON Web Tokens (JJWT)
- **Database:** MySQL 8+ & Spring Data JPA
- **Build Tool:** Maven
- **Utilities:** Lombok (for boilerplate reduction)

## 🚀 Getting Started Locally

### Prerequisites
Make sure you have [Java 21](https://adoptium.net/) and [Maven](https://maven.apache.org/) installed, as well as a local MySQL server running on port `3306`.

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/expensewise-backend.git
   cd expensewise-backend
   ```

2. **Configure Environment Variables:**
   Create a `.env` file in the root directory (or inject them directly in your IDE) containing your secure credentials:
   ```env
   DB_URL=jdbc:mysql://localhost:3306/expensewise?createDatabaseIfNotExist=true
   DB_USERNAME=root
   DB_PASSWORD=your_mysql_password
   JWT_SECRET=your_super_secret_jwt_key_here
   MAIL_USERNAME=your_email@gmail.com
   MAIL_PASSWORD=your_app_password
   ```

3. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```
   The API will boot up and listen on `http://localhost:8080`.

## 📦 Deployment (Render / Railway)

This application is containerized and ready for PaaS platforms.
1. Connect your GitHub repository to your platform of choice.
2. Ensure the platform uses the provided `Dockerfile`.
3. Inject the `DB_URL`, `JWT_SECRET`, and Mail credentials via the platform's Environment Variables dashboard.
4. The platform will automatically inject a `$PORT` variable which Spring Boot will bind to automatically.
