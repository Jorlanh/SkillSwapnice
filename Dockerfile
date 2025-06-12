# Estágio 1: Build da Aplicação com Maven usando uma imagem com JDK 21
# ALTERAÇÃO: Trocamos 'maven:3.8.5-openjdk-17' por uma imagem mais recente com Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia o arquivo de definição do projeto primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .

# Baixa todas as dependências do projeto
RUN mvn dependency:go-offline

# Copia o resto do código fonte do projeto
COPY src ./src

# Compila a aplicação e empacota em um .jar
RUN mvn clean install


# Estágio 2: Execução da Aplicação
# ALTERAÇÃO: Trocamos 'openjdk:17-jdk-slim' por uma imagem slim com Java 21
FROM eclipse-temurin:21-jdk-slim

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar compilado do estágio de build para a imagem final
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar .

# Expõe a porta que a aplicação vai usar (o Render vai mapear isso automaticamente)
EXPOSE 8080

# Comando para iniciar a aplicação quando o contêiner for executado
ENTRYPOINT ["java", "-jar", "skill-swap-0.0.1-SNAPSHOT.jar"]