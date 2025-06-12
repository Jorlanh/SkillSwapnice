# Estágio 1: Build - Usando a imagem oficial do Eclipse Temurin com JDK 21 e Maven
# ALTERAÇÃO FINAL: Usando uma imagem base do Temurin 21 e instalando o Maven nela.
FROM eclipse-temurin:21-jdk AS build

# Instala o Maven dentro do contêiner
RUN apt-get update && apt-get install -y maven

# Define o diretório de trabalho
WORKDIR /app

# Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o resto do código fonte e compila
COPY src ./src
RUN mvn clean install


# Estágio 2: Execução - Usando a imagem oficial e leve do Eclipse Temurin
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar compilado do estágio de build para a imagem final
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar .

# Expõe a porta da aplicação
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "skill-swap-0.0.1-SNAPSHOT.jar"]