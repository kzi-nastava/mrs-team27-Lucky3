# Team-27-Lucky-3 Backend

A Spring Boot REST API application.

## Prerequisites

- **JDK 21** - Required to run this project
- **Maven** - For dependency management and building the project

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Backend
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

Alternatively, you can run the application from IntelliJ IDEA:
- Right-click on `BackendApplication.java`
- Select "Run 'BackendApplication'"

### 4. Configure JDK in IntelliJ IDEA

1. Go to **File** → **Project Structure** → **Project**
2. Set **SDK** to JDK 21
3. Set **Language Level** to 21
4. Click **Apply** and **OK**

Also ensure Maven is using JDK 21:
1. Go to **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Runner**
2. Set **JRE** to JDK 21

## API Endpoints

The application runs on `http://localhost:8080` by default.

### Available Endpoints

- **GET** `/hello` - Returns a "Hello, World!" message
  ```bash
  curl http://localhost:8080/hello
  ```

## Technology Stack

- **Java 21**
- **Spring Boot 3.3.5**
- **Maven** - Build tool
- **Spring Web** - RESTful API support

## Project Structure

```
Backend/
├── src/
│   ├── main/
│   │   ├── java/com/team27/lucky3/backend/
│   │   │   ├── BackendApplication.java    # Main application entry point
│   │   │   └── HelloController.java       # REST controller
│   │   └── resources/
│   │       └── application.properties     # Application configuration
│   └── test/
│       └── java/com/team27/lucky3/backend/
│           └── ...                        # Test classes
└── pom.xml                                # Maven configuration
```

## Testing

Run tests with:
```bash
mvn test
```

## Notes

- The default server port is **8080**
- Spring Boot DevTools is enabled for automatic restarts during development

