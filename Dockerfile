FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/dbridgehub-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
# YAML 보다 JVM -D 가 우선. 환경변수는 sh 에서 펼쳐서 넣음 (compose 의 SPRING_DATASOURCE_* 반영).
# local 프로파일·application-local.yml 의 localhost 는 이 -D 로 덮어씀.
ENTRYPOINT ["sh", "-c", "exec java \
  -Dspring.profiles.active=docker \
  -Dspring.datasource.url=\"${SPRING_DATASOURCE_URL:-jdbc:postgresql://database-1.cv8wy8qw2jbe.ap-northeast-2.rds.amazonaws.com:5432/postgres}\" \
  -Dspring.datasource.username=\"${SPRING_DATASOURCE_USERNAME:-postgres}\" \
  -Dspring.datasource.password=\"${SPRING_DATASOURCE_PASSWORD:-RaE6gHu5eD7xwBW}\" \
  -jar app.jar"]
