FROM eclipse-temurin:23-jdk
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY pom.xml .
COPY settings.xml .
COPY src ./src

RUN mvn clean install -U --settings settings.xml && mv target/*-boot.jar target/warehouse.jar

EXPOSE 9000 8000
CMD ["java","-Xms512m","-jar","target/warehouse.jar"]
