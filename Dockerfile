# Estágio 1: Build - Usando a imagem oficial do Eclipse Temurin com JDK 21
# e instalando o Maven manualmente para garantir a compatibilidade.
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


# Estágio 2: Execução - Usando a imagem oficial e leve do Eclipse Temurin (apenas JRE)
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar compilado do estágio de build para a imagem final
COPY --from=build /app/target/skill-swap-0.0.1-SNAPSHOT.jar .

# Expõe a porta da aplicação
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "skill-swap-0.0.1-SNAPSHOT.jar"]