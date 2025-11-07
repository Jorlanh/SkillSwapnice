[Português](#português) | [English](#english) | [Español](#español)

---

<a name="português"></a>

# SkillSwap - Backend (API)

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=openjdk)](https://docs.oracle.com/en/java/javase/21/)
[![Spring Boot 3.5.0](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-OAuth2%20%7C%20JWT-blueviolet.svg?style=for-the-badge)](https://tools.ietf.org/html/rfc6749)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-lightgrey.svg?style=for-the-badge&logo=githubactions)](.github/workflows/codeql-analysis.yml)

Este repositório contém o código-fonte do backend da plataforma SkillSwap, desenvolvido como um Trabalho de Conclusão de Curso (TCC) em Engenharia de Software e projetado para implementação em ambiente de produção.

## ⚠️ Aviso de Licença e Propriedade Intelectual

**Este não é um projeto de código aberto (open-source).**

O código-fonte é disponibilizado neste repositório *apenas* para fins de demonstração, portfólio e avaliação académica.

É expressamente **proibida** a clonagem, cópia, redistribuição ou utilização deste código, total ou parcial, para outros fins comerciais ou não comerciais sem a autorização explícita do detentor dos direitos.

**Copyright © 2025 SkillSwap. Todos os direitos reservados.**

## 1. Arquitetura

A arquitetura atual é um **monolito modular**. Esta abordagem foi escolhida estrategicamente para manter a simplicidade de desenvolvimento e implantação de um monólito, ao mesmo tempo que impõe uma forte separação de responsabilidades e limites claros entre os domínios de negócio (ex: Usuários, Propostas, Social).

Esta estrutura modular facilita a manutenção, os testes e serve como uma fundação sólida para a **futura evolução planeada para uma arquitetura de microsserviços**.

A aplicação segue uma rigorosa **Arquitetura em Camadas (Layered Architecture)**:
* **API (Controllers):** Expõe os endpoints RESTful e lida com a validação de DTOs.
* **Negócio (Services):** Contém toda a lógica de negócio, regras de domínio e orquestração.
* **Dados (Repositories):** Abstrai o acesso ao banco de dados via Spring Data JPA.
* **Domínio (Entities):** Modela os dados da aplicação.
* **Infraestrutura (Config, Filters):** Configurações de segurança, serviços externos e filtros.

## 2. Pilha Tecnológica (Tech Stack)

O backend é sustentado por tecnologias modernas, com foco em performance e escalabilidade:

| Categoria | Tecnologia | Propósito |
| :--- | :--- | :--- |
| **Linguagem** | **Java 21** | Plataforma principal (adotada desde 12 de junho de 2025). |
| **Framework** | **Spring Boot 3.5.0** | Ecossistema principal para injeção de dependência, web e mais. |
| **Segurança** | **Spring Security (OAuth 2.0)** | Autenticação e autorização stateless via JWT como Resource Server (implementado em 4 de novembro de 2025). |
| **Banco de Dados** | **PostgreSQL** | Banco de dados relacional principal (produção). |
| **Busca** | **OpenSearch** | Para busca full-text rápida e flexível. |
| **Tempo Real (Chat)**| **Spring WebSocket + Redis** | Chat escalável horizontalmente usando Redis Pub/Sub. |
| **Tempo Real (Vídeo)**| **LiveKit (WebRTC)** | Servidor de mídia para chamadas de vídeo WebRTC. |
| **Inteligência Artificial**| **Google Cloud VertexAI (Gemini)** | Para chatbot, moderação de conteúdo e geração de imagem. |
| **Upload de Ficheiros**| **Cloudinary** | Armazenamento e entrega de média (imagens, vídeos). |
| **Notificações** | **Twilio (SMS) & Spring Mail** | Envio de códigos de verificação e notificações transacionais. |
| **Containerização** | **Docker** | Empacotamento da aplicação para produção. |

## 3. Principais Funcionalidades (Domínios)

O código é modularizado em torno dos seguintes domínios de negócio:

### 1. Domínio: Usuário, Autenticação e Segurança
* **Segurança Stateless:** A aplicação funciona como um **OAuth 2.0 Resource Server**, validando JWTs *stateless* em cada requisição.
* **Provisionamento JIT:** Usuários de provedores de identidade externos (como Auth0) são criados no banco de dados local "Just-In-Time" na primeira vez que acedem.
* **Filtro de Banimento (`BanCheckFilter`):** Um filtro de segurança personalizado que verifica o status de banimento do usuário no banco de dados local *após* a validação do JWT, permitindo o banimento imediato de usuários, mesmo que o token ainda seja válido.
* **Rate Limiting:** Proteção contra ataques de força bruta em endpoints sensíveis (como login) usando `Bucket4j`.

### 2. Domínio: Fluxo de Troca (Propostas e Avaliações)
* **Ciclo de Vida da Proposta:** Gestão completa do fluxo de troca de habilidades (Pendente, Aceite, Rejeitada, Concluída).
* **Processamento Assíncrono:** Ao completar uma troca, o sistema usa `ApplicationEventPublisher` e `@Async` para executar tarefas lentas (como enviar emails, verificar conquistas e reindexar na busca) em segundo plano, devolvendo uma resposta imediata ao usuário.
* **Ranking e Avaliações:** Sistema de avaliação de 1 a 5 estrelas vinculado a uma troca concluída.

### 3. Domínio: Ambiente Social (Feed e Comunidades)
* **Feed e Interações:** Criação de posts, comentários, "likes" e "reposts".
* **Algoritmo de Trending:** Um serviço (`TrendingServiceImpl`) calcula os posts em alta com base num *score* ponderado de interações (likes, comentários, cliques em links partilhados).

### 4. Domínio: Recursos em Tempo Real (Chat e Vídeo)
* **Chat Escalável:** Um sistema de chat 1-para-1 construído com **Spring WebSocket** e **Redis Pub/Sub**. Isto garante que o chat funcione num ambiente com balanceamento de carga (múltiplas instâncias), já que as mensagens não são geridas localmente, mas sim publicadas no Redis e distribuídas para a instância correta onde o usuário destinatário está conectado.
* **Vídeo (WebRTC):** Integração com **LiveKit**. O backend atua apenas como um gerador de tokens de autorização seguros. O cliente (frontend) recebe este token e conecta-se diretamente ao servidor LiveKit, descarregando todo o processamento de mídia do backend.

### 5. Domínio: Administração e Moderação
* **Painel Admin:** Endpoints seguros (protegidos por *scope* `manage:console` no JWT) para gerir usuários, ver estatísticas e alternar selos de verificação.
* **Automação de Denúncias:** Um serviço agendado (`@Scheduled`) processa denúncias de usuários. O texto da denúncia é enviado à IA (Gemini) para análise; se for considerado inapropriado, o usuário denunciado é banido automaticamente.

## 4. Integração com IA (Google Gemini)

A IA é usada em quatro áreas distintas através do Google Cloud VertexAI:
1.  **Chatbot (`ChatbotServiceImpl`):** Um serviço de chatbot que usa um *system prompt* para responder a perguntas dos usuários sobre a plataforma.
2.  **Moderação de Conteúdo (`GeminiApiModerationService`):** Um serviço que envia conteúdo (posts, comentários, denúncias) para a IA e recebe uma resposta "SIM" ou "NÃO" sobre se o conteúdo é inapropriado, bloqueando a publicação se necessário.
3.  **Tradução e Simplificação:** Funções de IA para traduzir ou simplificar texto para maior acessibilidade.
4.  **Geração de Imagem (`AchievementServiceImpl`):** Os usuários podem gerar imagens de conquistas únicas com base num prompt, que são então carregadas para o Cloudinary.

## 5. CI/CD e Segurança do Código

O repositório está configurado para automação de segurança:
* **CodeQL:** O workflow `.github/workflows/codeql-analysis.yml` executa análise de segurança estática em cada *push* e *pull request* para a branch `main`.
* **Dependabot:** O ficheiro `.github/dependabot.yml` verifica e cria PRs para atualizações de dependências do Maven diariamente.

## 6. Como Executar (Ambiente Local)

### Pré-requisitos
* Java 21
* Apache Maven
* PostgreSQL
* Redis
* OpenSearch
* Contas de serviços externos (Cloudinary, Twilio, Google VertexAI, LiveKit).

### Passos
1.  **Clone o repositório:**
    ```sh
    git clone [https://github.com/Jorlanh/SkillSwapnice.git](https://github.com/Jorlanh/SkillSwapnice.git)
    cd SkillSwapnice
    ```

2.  **Configuração de Ambiente:**
    * Este projeto espera que as credenciais de serviços externos (API Keys, segredos do banco de dados, etc.) sejam fornecidas através de **variáveis de ambiente**.
    * Para desenvolvimento local, pode criar um ficheiro `application.properties` dentro de `src/main/resources/` (este ficheiro está no `.gitignore` e não deve ser "commitado").

3.  **Execute a aplicação:**
    ```sh
    mvn spring-boot:run
    ```

### Executando com Docker
O projeto inclui um `Dockerfile` otimizado e seguro:
* Utiliza *multi-stage builds* para uma imagem final leve (baseada em JRE).
* Cria e executa a aplicação como um usuário não-root (`app`) para maior segurança.

    ```sh
    # 1. Construir a imagem
    docker build -t skillswap-backend .

    # 2. Executar (exige passagem de todas as variáveis de ambiente)
    docker run -e SPRING_DATASOURCE_URL=... -e GOOGLE_CREDENTIALS_JSON=... -e LIVEKIT_API_KEY=... -p 8080:8080 skillswap-backend
    ```

## 7. Status do Projeto

Este projeto é o Trabalho de Conclusão de Curso (TCC) do Bacharelado em Engenharia de Software na Universidade Católica do Salvador (UCSAL) e está projetado para ser uma aplicação de nível de produção.

---

<a name="english"></a>

# SkillSwap - Backend (API)

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=openjdk)](https://docs.oracle.com/en/java/javase/21/)
[![Spring Boot 3.5.0](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-OAuth2%20%7C%20JWT-blueviolet.svg?style=for-the-badge)](https://tools.ietf.org/html/rfc6749)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-lightgrey.svg?style=for-the-badge&logo=githubactions)](.github/workflows/codeql-analysis.yml)

This repository contains the backend source code for the SkillSwap platform, developed as a Final Year Project (TCC) in Software Engineering and designed for production deployment.

## ⚠️ License and Intellectual Property Warning

**This is not an open-source project.**

The source code is made available in this repository for demonstration, portfolio, and academic evaluation purposes *only*.

Cloning, copying, redistribution, or use of this code, in whole or in part, for other commercial or non-commercial purposes is expressly **prohibited** without the explicit authorization of the rights holder.

**Copyright © 2025 SkillSwap. All rights reserved.**

## 1. Architecture

The current architecture is a **modular monolith**. This approach was strategically chosen to maintain the development and deployment simplicity of a monolith while enforcing a strong separation of responsibilities and clear boundaries between business domains (e.g., Users, Proposals, Social).

This modular structure facilitates maintenance, testing, and serves as a solid foundation for the **planned future evolution into a microservices architecture**.

The application follows a strict **Layered Architecture**:
* **API (Controllers):** Exposes RESTful endpoints and handles DTO validation.
* **Business (Services):** Contains all business logic, domain rules, and orchestration.
* **Data (Repositories):** Abstracts database access via Spring Data JPA.
* **Domain (Entities):** Models the application's data.
* **Infrastructure (Config, Filters):** Security configurations, external services, and filters.

## 2. Tech Stack

The backend is powered by modern technologies, focusing on performance and scalability:

| Category | Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | **Java 21** | Core platform (adopted since June 12, 2025). |
| **Framework** | **Spring Boot 3.5.0** | Main ecosystem for dependency injection, web, and more. |
| **Security** | **Spring Security (OAuth 2.0)** | Stateless authentication/authorization via JWT as a Resource Server (implemented on November 4, 2025). |
| **Database** | **PostgreSQL** | Primary relational database (production). |
| **Search** | **OpenSearch** | For fast and flexible full-text search. |
| **Real-time (Chat)**| **Spring WebSocket + Redis** | Horizontally scalable chat using Redis Pub/Sub. |
| **Real-time (Video)**| **LiveKit (WebRTC)** | Media server for WebRTC video calls. |
| **Artificial Intelligence**| **Google Cloud VertexAI (Gemini)** | For chatbot, content moderation, and image generation. |
| **File Uploads**| **Cloudinary** | Media storage and delivery (images, videos). |
| **Notifications** | **Twilio (SMS) & Spring Mail** | Sending verification codes and transactional notifications. |
| **Containerization** | **Docker** | Application containerization for production. |

## 3. Key Features (Domains)

The code is modularized around the following business domains:

### 1. Domain: User, Authentication, and Security
* **Stateless Security:** The application acts as an **OAuth 2.0 Resource Server**, validating *stateless* JWTs on every request.
* **JIT Provisioning:** Users from external identity providers (like Auth0) are created in the local database "Just-In-Time" on their first access.
* **Ban Filter (`BanCheckFilter`):** A custom security filter that checks the user's ban status in the local database *after* JWT validation, allowing for immediate banning of users even if their token is still valid.
* **Rate Limiting:** Brute-force protection on sensitive endpoints (like login) using `Bucket4j`.

### 2. Domain: Exchange Flow (Proposals & Ratings)
* **Proposal Lifecycle:** Complete management of the skill exchange lifecycle (Pending, Accepted, Rejected, Completed).
* **Asynchronous Processing:** Upon completing an exchange, the system uses `ApplicationEventPublisher` and `@Async` to execute slow tasks (like sending emails, checking for achievements, and re-indexing in search) in the background, returning an immediate response to the user.
* **Ranking & Ratings:** A 1-5 star rating system linked to a completed exchange.

### 3. Domain: Social Environment (Feed & Communities)
* **Feed and Interactions:** Creation of posts, comments, likes, and reposts.
* **Trending Algorithm:** A `TrendingServiceImpl` calculates trending posts based on a weighted score of interactions (likes, comments, shared link clicks).

### 4. Domain: Real-time Resources (Chat & Video)
* **Scalable Chat:** A 1-to-1 chat system built with **Spring WebSocket** and **Redis Pub/Sub**. This ensures the chat works in a load-balanced environment (multiple instances), as messages are not handled locally but published to Redis and distributed to the correct instance where the recipient user is connected.
* **Video (WebRTC):** Integration with **LiveKit**. The backend acts only as a secure authorization token generator. The client (frontend) receives this token and connects directly to the LiveKit server, offloading all media processing from the backend.

### 5. Domain: Administration & Moderation
* **Admin Panel:** Secure endpoints (protected by `manage:console` scope in the JWT) to manage users, view statistics, and toggle verification badges.
* **Report Automation:** A scheduled service (`@Scheduled`) processes user reports. The report text is sent to the AI (Gemini) for analysis; if deemed inappropriate, the reported user is automatically banned.

## 4. AI Integration (Google Gemini)

AI is used in four distinct areas via Google Cloud VertexAI:
1.  **Chatbot (`ChatbotServiceImpl`):** A chatbot service that uses a system prompt to answer user questions about the platform.
2.  **Content Moderation (`GeminiApiModerationService`):** A service that sends content (posts, comments, reports) to the AI and receives a "YES" or "NO" response as to whether the content is inappropriate, blocking publication if necessary.
3.  **Translation & Simplification:** AI functions to translate or simplify text for greater accessibility.
4.  **Image Generation (`AchievementServiceImpl`):** Users can generate unique achievement images based on a prompt, which are then uploaded to Cloudinary.

## 5. CI/CD and Code Security

The repository is configured for security automation:
* **CodeQL:** The `.github/workflows/codeql-analysis.yml` workflow runs static security analysis on every push and pull request to the `main` branch.
* **Dependabot:** The `.github/dependabot.yml` file checks for and creates PRs for Maven dependency updates daily.

## 6. How to Run (Local Environment)

### Prerequisites
* Java 21
* Apache Maven
* PostgreSQL
* Redis
* OpenSearch
* Accounts for external services (Cloudinary, Twilio, Google VertexAI, LiveKit).

### Steps
1.  **Clone the repository:**
    ```sh
    git clone [https://github.com/Jorlanh/SkillSwapnice.git](https://github.com/Jorlanh/SkillSwapnice.git)
    cd SkillSwapnice
    ```

2.  **Environment Configuration:**
    * This project expects credentials for external services (API Keys, database secrets, etc.) to be provided via **environment variables**.
    * For local development, you can create an `application.properties` file in `src/main/resources/` (this file is in `.gitignore` and should not be committed).

3.  **Run the application:**
    ```sh
    mvn spring-boot:run
    ```

### Running with Docker
The project includes an optimized and secure `Dockerfile`:
* It uses *multi-stage builds* for a lightweight final image (JRE-based).
* It creates and runs the application as a non-root user (`app`) for enhanced security.

    ```sh
    # 1. Build the image
    docker build -t skillswap-backend .

    # 2. Run (requires passing all environment variables)
    docker run -e SPRING_DATASOURCE_URL=... -e GOOGLE_CREDENTIALS_JSON=... -e LIVEKIT_API_KEY=... -p 8080:8080 skillswap-backend
    ```

## 7. Project Status

This project is the Final Year Project (TCC) for the Bachelor's Degree in Software Engineering at the Catholic University of Salvador (UCSAL) and is designed as a production-level application.

---

<a name="español"></a>

# SkillSwap - Backend (API)

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg?style=for-the-badge&logo=openjdk)](https://docs.oracle.com/en/java/javase/21/)
[![Spring Boot 3.5.0](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-OAuth2%20%7C%20JWT-blueviolet.svg?style=for-the-badge)](https://tools.ietf.org/html/rfc6749)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-lightgrey.svg?style=for-the-badge&logo=githubactions)](.github/workflows/codeql-analysis.yml)

Este repositorio contiene el código fuente del backend de la plataforma SkillSwap, desarrollado como un Proyecto de Fin de Carrera (TCC) en Ingeniería de Software y diseñado para su implementación en un entorno de producción.

## ⚠️ Advertencia de Licencia y Propiedad Intelectual

**Este no es un proyecto de código abierto (open-source).**

El código fuente se proporciona en este repositorio *únicamente* con fines de demostración, portafolio y evaluación académica.

Queda expresamente **prohibida** la clonación, copia, redistribución o utilización de este código, total o parcial, para otros fines comerciales o no comerciales sin la autorización explícita del titular de los derechos.

**Copyright © 2025 SkillSwap. Todos los derechos reservados.**

## 1. Arquitectura

La arquitectura actual es un **monolito modular**. Este enfoque fue elegido estratégicamente para mantener la simplicidad de desarrollo y despliegue de un monolito, al mismo tiempo que impone una fuerte separación de responsabilidades y límites claros entre los dominios de negocio (ej: Usuarios, Propuestas, Social).

Esta estructura modular facilita el mantenimiento, las pruebas y sirve como una base sólida para la **futura evolución planificada hacia una arquitectura de microservicios**.

La aplicación sigue una rigurosa **Arquitectura en Capas (Layered Architecture)**:
* **API (Controladores):** Expone los endpoints RESTful y maneja la validación de DTOs.
* **Negocio (Servicios):** Contiene toda la lógica de negocio, reglas de dominio y orquestación.
* **Datos (Repositorios):** Abstrae el acceso a la base de datos mediante Spring Data JPA.
* **Dominio (Entidades):** Modela los datos de la aplicación.
* **Infraestructura (Config, Filtros):** Configuraciones de seguridad, servicios externos y filtros.

## 2. Stack Tecnológico (Tech Stack)

El backend se basa en tecnologías modernas, con un enfoque en el rendimiento y la escalabilidad:

| Categoría | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Lenguaje** | **Java 21** | Plataforma principal (adoptada desde el 12 de junio de 2025). |
| **Framework** | **Spring Boot 3.5.0** | Ecosistema principal para inyección de dependencias, web y más. |
| **Seguridad** | **Spring Security (OAuth 2.0)** | Autenticación y autorización stateless vía JWT como Resource Server (implementado el 4 de noviembre de 2025). |
| **Base de Datos** | **PostgreSQL** | Base de datos relacional principal (producción). |
| **Búsqueda** | **OpenSearch** | Para búsqueda full-text rápida y flexible. |
| **Tiempo Real (Chat)**| **Spring WebSocket + Redis** | Chat escalable horizontalmente usando Redis Pub/Sub. |
| **Tiempo Real (Vídeo)**| **LiveKit (WebRTC)** | Servidor de medios para videollamadas WebRTC. |
| **Inteligencia Artificial**| **Google Cloud VertexAI (Gemini)** | Para chatbot, moderación de contenido y generación de imágenes. |
| **Carga de Archivos**| **Cloudinary** | Almacenamiento y entrega de medios (imágenes, vídeos). |
| **Notificaciones** | **Twilio (SMS) & Spring Mail** | Envío de códigos de verificación y notificaciones transaccionales. |
| **Contenerización** | **Docker** | Empaquetado de la aplicación para producción. |

## 3. Funcionalidades Principales (Dominios)

El código está modularizado en torno a los siguientes dominios de negocio:

### 1. Dominio: Usuario, Autenticación y Seguridad
* **Seguridad Stateless:** La aplicación funciona como un **OAuth 2.0 Resource Server**, validando JWTs *stateless* en cada solicitud.
* **Aprovisionamiento JIT:** Los usuarios de proveedores de identidad externos (como Auth0) se crean en la base de datos local "Just-In-Time" en su primer acceso.
* **Filtro de Baneo (`BanCheckFilter`):** Un filtro de seguridad personalizado que comprueba el estado de baneo del usuario en la base de datos local *después* de la validación del JWT, permitiendo el baneo inmediato de usuarios, incluso si su token sigue siendo válido.
* **Límite de Tasa (Rate Limiting):** Protección contra ataques de fuerza bruta en endpoints sensibles (como el login) usando `Bucket4j`.

### 2. Dominio: Flujo de Intercambio (Propuestas y Valoraciones)
* **Ciclo de Vida de la Propuesta:** Gestión completa del flujo de intercambio de habilidades (Pendiente, Aceptada, Rechazada, Completada).
* **Procesamiento Asíncrono:** Al completar un intercambio, el sistema usa `ApplicationEventPublisher` y `@Async` para ejecutar tareas lentas (como enviar correos, verificar logros y reindexar en la búsqueda) en segundo plano, devolviendo una respuesta inmediata al usuario.
* **Ranking y Valoraciones:** Sistema de valoración de 1 a 5 estrellas vinculado a un intercambio completado.

### 3. Dominio: Entorno Social (Feed y Comunidades)
* **Feed e Interacciones:** Creación de publicaciones, comentarios, "likes" y "reposts".
* **Algoritmo de Tendencias:** Un servicio (`TrendingServiceImpl`) calcula las publicaciones en tendencia basándose en una puntuación ponderada de interacciones (likes, comentarios, clics en enlaces compartidos).

### 4. Dominio: Recursos en Tiempo Real (Chat y Vídeo)
* **Chat Escalable:** Un sistema de chat 1 a 1 construido con **Spring WebSocket** y **Redis Pub/Sub**. Esto asegura que el chat funcione en un entorno con balanceo de carga (múltiples instancias), ya que los mensajes no se manejan localmente, sino que se publican en Redis y se distribuyen a la instancia correcta donde el usuario destinatario está conectado.
* **Vídeo (WebRTC):** Integración con **LiveKit**. El backend actúa solo como un generador seguro de tokens de autorización. El cliente (frontend) recibe este token y se conecta directamente al servidor de LiveKit, descargando todo el procesamiento de medios del backend.

### 5. Dominio: Administración y Moderación
* **Panel de Administración:** Endpoints seguros (protegidos por el *scope* `manage:console` en el JWT) para gestionar usuarios, ver estadísticas y alternar insignias de verificación.
* **Automatización de Denuncias:** Un servicio programado (`@Scheduled`) procesa las denuncias de los usuarios. El texto de la denuncia se envía a la IA (Gemini) para su análisis; si se considera inapropiado, el usuario denunciado es baneado automáticamente.

## 4. Integración con IA (Google Gemini)

La IA se utiliza en cuatro áreas distintas a través de Google Cloud VertexAI:
1.  **Chatbot (`ChatbotServiceImpl`):** Un servicio de chatbot que usa un *system prompt* para responder a las preguntas de los usuarios sobre la plataforma.
2.  **Moderación de Contenido (`GeminiApiModerationService`):** Un servicio que envía contenido (publicaciones, comentarios, denuncias) a la IA y recibe una respuesta "SÍ" o "NO" sobre si el contenido es inapropiado, bloqueando la publicación si es necesario.
3.  **Traducción y Simplificación:** Funciones de IA para traducir o simplificar texto para una mayor accesibilidad.
4.  **Generación de Imágenes (`AchievementServiceImpl`):** Los usuarios pueden generar imágenes de logros únicas basadas en un *prompt*, que luego se cargan en Cloudinary.

## 5. CI/CD y Seguridad del Código

El repositorio está configurado para la automatización de la seguridad:
* **CodeQL:** El flujo de trabajo `.github/workflows/codeql-analysis.yml` ejecuta análisis de seguridad estáticos en cada *push* y *pull request* a la rama `main`.
* **Dependabot:** El archivo `.github/dependabot.yml` comprueba y crea PRs para actualizaciones de dependencias de Maven diariamente.

## 6. Cómo Ejecutar (Entorno Local)

### Prerrequisitos
* Java 21
* Apache Maven
* PostgreSQL
* Redis
* OpenSearch
* Cuentas de servicios externos (Cloudinary, Twilio, Google VertexAI, LiveKit).

### Pasos
1.  **Clona el repositorio:**
    ```sh
    git clone [https://github.com/Jorlanh/SkillSwapnice.git](https://github.com/Jorlanh/SkillSwapnice.git)
    cd SkillSwapnice
    ```

2.  **Configuración del Entorno:**
    * Este proyecto espera que las credenciales de servicios externos (API Keys, secretos de base de datos, etc.) se proporcionen a través de **variables de entorno**.
    * Para el desarrollo local, puedes crear un archivo `application.properties` dentro de `src/main/resources/` (este archivo está en `.gitignore` y no debe ser "commiteado").

3.  **Ejecuta la aplicación:**
    ```sh
    mvn spring-boot:run
    ```

### Ejecutando con Docker
El proyecto incluye un `Dockerfile` optimizado y seguro:
* Utiliza *multi-stage builds* para una imagen final ligera (basada en JRE).
* Crea y ejecuta la aplicación como un usuario no-root (`app`) para mayor seguridad.

    ```sh
    # 1. Construir la imagen
    docker build -t skillswap-backend .

    # 2. Ejecutar (requiere pasar todas las variables de entorno)
    docker run -e SPRING_DATASOURCE_URL=... -e GOOGLE_CREDENTIALS_JSON=... -e LIVEKIT_API_KEY=... -p 8080:8080 skillswap-backend
    ```

## 7. Estado del Proyecto

Este proyecto es el Trabajo de Conclusión de Curso (TCC) de la Licenciatura en Ingeniería de Software en la Universidade Católica do Salvador (UCSAL) y está diseñado como una aplicación a nivel de producción.