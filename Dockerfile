FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist /tmp/frontend-dist
RUN rm -rf src/main/resources/static/* \
 && mkdir -p src/main/resources/static \
 && cp -r /tmp/frontend-dist/. src/main/resources/static/ \
 && mvn -B -Dmaven.test.skip=true package \
 && cp "$(find target -maxdepth 1 -name '*.jar' ! -name '*original*.jar' | head -n 1)" /tmp/app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=backend-build /tmp/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
