# Passwordy - Setup Instructions

## Prerequisites
- **Java 17+**
- **Maven 3.6+**
- **Docker Desktop** (for Docker setup) OR **PostgreSQL 15+** (for local setup)
- **Android Studio** (for mobile app)

---

## Backend Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd passwordy/backend
```

### 2. Configure Application Properties

⚠️ **IMPORTANT:** The actual configuration files are NOT in the repository for security reasons.

#### Copy Example Files
```bash
cd src/main/resources

# For Docker setup
cp application-docker.properties.example application-docker.properties

# For local PostgreSQL setup
cp application-local.properties.example application-local.properties
```

#### Generate JWT Secret Key

**Windows PowerShell:**
```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Linux/Mac:**
```bash
openssl rand -base64 64
```

#### Update Configuration Files

**For `application-docker.properties`:**
1. Replace `jwt.secret` with your generated secret key
2. Replace `spring.datasource.password` with your Docker PostgreSQL password (default: `postgres`)

**For `application-local.properties`:**
1. Replace `jwt.secret` with your generated secret key
2. Replace `spring.datasource.password` with your local PostgreSQL password

---

## Database Setup

### Option A: Docker (Recommended)

#### Start Docker Containers
```bash
cd backend
docker-compose up -d
```

#### Verify Containers
```bash
docker-compose ps

# Should show:
# passwordy-postgres    Up (healthy)
# passwordy-pgadmin     Up
```

#### Access pgAdmin (Optional)
- **URL:** http://localhost:5050
- **Email:** admin@passwordy.com
- **Password:** admin

**Add Server in pgAdmin:**
- **Host:** `postgres` (container name)
- **Port:** `5432`
- **Database:** `passwordy`
- **Username:** `postgres`
- **Password:** `postgres` (or your custom password)

### Option B: Local PostgreSQL

#### Install PostgreSQL
- **Windows:** https://www.postgresql.org/download/windows/
- **Mac:** `brew install postgresql@15`
- **Linux:** `sudo apt install postgresql postgresql-contrib`

#### Create Database
```sql
CREATE DATABASE passwordy;
```

---

## Running the Backend

### With Docker (Recommended)
```bash
cd backend

# Run with docker profile (uses application-docker.properties)
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### With Local PostgreSQL
```bash
cd backend

# Run with local profile (uses application-local.properties)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Backend will be available at:** http://localhost:8080

---

## Android Frontend Setup

### 1. Open Project
```bash
cd frontend
# Open in Android Studio
```

### 2. Sync Gradle
- File → Sync Project with Gradle Files

### 3. Configure Backend URL
In `RetrofitInstance.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/" // For emulator
```

**For physical device:** Use your computer's IP address:
```kotlin
private const val BASE_URL = "http://192.168.x.x:8080/"
```

### 4. Run App
- Select emulator or device
- Click Run ▶️

---

## Testing

### Run the automated test suite
```bash
cd backend
./mvnw test
```
The tests run against a throwaway **PostgreSQL container** managed by Testcontainers, so
**Docker must be running** (no separate `docker compose up` needed — the container is started
and torn down automatically). To build without running tests: `./mvnw clean install -DskipTests`.

### Test Backend with Postman
1. Import collection from `/docs/postman_collection.json` (if available)
2. Test endpoints:
   - `POST /api/auth/register` - Create account
   - `POST /api/auth/login` - Login
   - `GET /api/passwords` - List passwords (requires auth)
   - `POST /api/passwords` - Create password (requires auth)

### Test Android App
1. Register new account
2. Login
3. Add password
4. Generate password/PIN
5. Decrypt password
6. Edit password
7. Delete password

---

## Troubleshooting

### Backend won't start
**Check configuration file:**
```bash
# Verify you created the config file
ls src/main/resources/application-docker.properties

# Check for typos in JWT secret or passwords
```

### Port 5432 already in use
**Stop local PostgreSQL:**
```powershell
# Windows
net stop postgresql-x64-15

# Mac
brew services stop postgresql

# Linux
sudo systemctl stop postgresql
```

**Or change Docker port:**
```yaml
# docker-compose.yml
ports:
  - "5433:5432"  # Use 5433 instead
```

### Android app can't connect
1. ✅ Verify backend is running: http://localhost:8080
2. ✅ Use `10.0.2.2` for emulator (not `localhost`)
3. ✅ Use actual IP for physical device
4. ✅ Check firewall isn't blocking port 8080

### Docker containers won't start
```bash
# Check Docker Desktop is running
docker --version

# View logs
docker-compose logs

# Restart containers
docker-compose restart

# Nuclear option - complete reset
docker-compose down -v
docker-compose up -d
```

---

## Security Notes

### ⚠️ NEVER commit these files:
- `application-docker.properties` (contains JWT secret + DB password)
- `application-local.properties` (contains JWT secret + DB password)
- `application.properties` (if it exists with secrets)

### ✅ Safe to commit:
- `application-docker.properties.example`
- `application-local.properties.example`
- `docker-compose.yml`
- All source code
- `.gitignore`

### Generating New Secrets
If your JWT secret is compromised, generate a new one:
```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```
Update both `application-docker.properties` and `application-local.properties`.

---

## Project Structure
```
passwordy/
├── backend/
│   ├── src/main/
│   │   ├── java/.../
│   │   └── resources/
│   │       ├── application-docker.properties (NOT COMMITTED)
│   │       ├── application-local.properties (NOT COMMITTED)
│   │       ├── application-docker.properties.example (COMMITTED)
│   │       └── application-local.properties.example (COMMITTED)
│   ├── docker-compose.yml
│   └── pom.xml
├── frontend/
│   └── (Android project files)
└── .gitignore
```

---

## Contributing
1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes
3. Test thoroughly
4. **Never commit configuration files with secrets**
5. Submit pull request

---

## Support
For issues or questions, please open an issue on GitHub.

## License
[Add your license here]