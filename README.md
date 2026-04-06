# FrameValue - GPU Performance Comparison

A Spring Boot application for comparing GPUs using the FPS/₪ metric to find the best value for money.

## Features

- GPU comparison by performance and price
- Real-time price tracking from Israeli retailers
- Personalized recommendations based on budget and games
- Community FPS reports with validation
- Tier-based categorization (AMD vs NVIDIA)

## Screenshots

- [Home Page 1](images/home1.png)
- [Home Page 2](images/home2.png)
- [GPU Comparison 1](images/comparison1.png)
- [GPU Comparison 2](images/comparison2.png)
- [Recommendations 1](images/recom1.png)
- [Recommendations 2](images/recom2.png)
- [Resolution Comparison 1](images/resolution1.png)
- [Resolution Comparison 2](images/resolution2.png)
- [Community Reports 1](images/reports1.png)
- [Community Reports 2](images/reports2.png)
- [Community Reports 3](images/reports3.png)
- [Price Tracking](images/price.png)
- [Tier Rankings](images/tiers.png)

## Tech Stack

- **Backend:** Spring Boot 4.0.3 (Java 21), PostgreSQL 15, JPA/Hibernate
- **Frontend:** Thymeleaf, Bootstrap 5, JavaScript
- **DevOps:** Docker, Docker Compose
- **Web Scraping:** Python 3.9+, Playwright

## Quick Start with Docker
```bash
git clone https://github.com/yourusername/framevalue.git
cd framevalue
docker compose up --build
```

Open: http://localhost:8080

## Manual Setup

### Prerequisites
- Java 21+, PostgreSQL 15+, Python 3.9+, Maven 3.9+

### Steps

1. Clone the repository
2. Create database: `CREATE DATABASE framevalue_db;`
3. Update `application.properties` with your database credentials
4. Install Python dependencies:
```bash
   cd src/main/resources/scripts
   pip install -r requirements.txt
   playwright install chromium
```
5. Run: `mvn spring-boot:run`

## Key Features

**FPS/₪ Calculation**

FPS per Shekel = Average FPS ÷ Price (₪)

**Community Reports**
- Validation: Max 40% deviation from benchmark
- Rate limiting: 3 reports/day per game (per IP)
- Auto-cleanup after 24 hours

**Automated Price Updates**
- Runs every Saturday at midnight
- Scrapes Israeli retailers
- Tracks price changes

## Project Structure

src/main/java/com/firstproject/framevalue/
├controller/      # MVC Controllers
├ service/         # Business logic
├repository/      # JPA Repositories
├entity/          # Database entities
├scheduler/       # Scheduled tasks
└ config/          # Configuration

## Database Tables

- `gpu_model` - GPU specifications
- `benchmark_result` - Official FPS data
- `user_submission` - Community reports (24h TTL)
- `community_average` - Aggregated community data
- `gpu_price` - Price history

## Testing
```bash
mvn test
```

18 unit tests covering core services (Comparison, Cleanup, GPU, Recommendation, Submission, Tier).

## Contact

**Pavel Bugri**
- Email: pavelbugri93@gmail.com
- LinkedIn: [linkedin.com/in/pavel-bugri-a184b1335](https://linkedin.com/in/pavel-bugri-a184b1335)
- GitHub: [github.com/pavelbugri93-cmyk](https://github.com/pavelbugri93-cmyk)

## License

Educational project - Software Engineering course.
© 2026 Pavel Bugri. All rights reserved.