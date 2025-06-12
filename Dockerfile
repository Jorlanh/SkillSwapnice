# Estágio 1: Build - Usando uma imagem oficial do Maven com OpenJDK 21
# ALTERAÇÃO: Usando a tag oficial e mais comum para Maven com Java 21
FROM maven:3-openjdk-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean install


# Estágio 2: Execução - Usando uma imagem oficial e leve do OpenJDK 21
# ALTERAÇÃO: Usando a tag oficial e slim para Java 21
FROM openjdk:21-slim

WORKDIR /app
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "skill-swap-0.0.1-SNAPSHOT.jar"]