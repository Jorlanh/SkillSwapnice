# Estágio 1: Build da Aplicação
FROM eclipse-temurin:21-jdk AS build

# Instala o Maven
RUN apt-get update && apt-get install -y maven

# Define o diretório de trabalho
WORKDIR /app

# Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o resto do código fonte e compila, pulando os testes
COPY src ./src
RUN mvn clean install -DskipTests


# Estágio 2: Execução da Aplicação
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar compilado do estágio de build
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta da aplicação
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]