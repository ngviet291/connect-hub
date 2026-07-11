# Stage 1: Build bằng Image của Maven tích hợp sẵn Java 21
FROM maven:3.9-eclipse-temurin-21-jammy AS builder
WORKDIR /app

# Sao chép file cấu hình thư viện và tải trước về máy ảo
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Sao chép mã nguồn và tiến hành build file .jar
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Chạy ứng dụng bằng Image nhẹ hơn của OpenJDK 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

#Copy file .jar đã build từ Stage 1 sang Stage 2
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]