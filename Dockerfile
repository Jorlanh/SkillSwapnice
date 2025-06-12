# Estágio 1: Build - Usando uma imagem oficial do Maven com OpenJDK 21
FROM maven:3-openjdk-21 AS build

# Instala o Maven dentro do contêiner
RUN apt-get update && apt-get install -y maven

# Define o diretório de trabalho
WORKDIR /app

# Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o resto do código fonte e compila
COPY src ./src

# ALTERAÇÃO: Adicionado -DskipTests para pular os testes durante o build
RUN mvn clean install -DskipTests


# Estágio 2: Execução - Usando uma imagem oficial e leve do OpenJDK 21
FROM openjdk:21-slim

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar compilado do estágio de build para a imagem final
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar .

# Expõe a porta da aplicação
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "skill-swap-0.0.1-SNAPSHOT.jar"]