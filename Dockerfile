# ============================================
# Stage 1: 前端构建
# ============================================
FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend

# 优先复制依赖文件，利用 Docker 缓存
COPY frontend/package.json frontend/package-lock.json ./

# 使用 npm ci 而非 npm install，更快速且可靠
RUN npm ci --prefer-offline --no-audit

# 复制前端源码并构建
COPY frontend/ ./
RUN npm run build

# ============================================
# Stage 2: 后端构建
# ============================================
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build

WORKDIR /app

# 优先复制 pom.xml 下载依赖（利用缓存）
COPY pom.xml ./
RUN mvn -B dependency:go-offline -Dmaven.test.skip=true

# 复制源码
COPY src ./src

# 复制前端构建产物到静态资源目录
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/

# 构建并提取 JAR（分层打包以优化镜像）
RUN mvn -B -Dmaven.test.skip=true clean package spring-boot:repackage \
    && java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ============================================
# Stage 3: 运行时镜像
# ============================================
FROM eclipse-temurin:17-jre-alpine

# 安装必要工具和时区数据（精简版）
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

WORKDIR /app

# 创建非 root 用户运行应用（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 分层复制 JAR 内容（优化 Docker 层缓存和镜像大小）
COPY --from=backend-build /app/extracted/dependencies/ ./
COPY --from=backend-build /app/extracted/spring-boot-loader/ ./
COPY --from=backend-build /app/extracted/snapshot-dependencies/ ./
COPY --from=backend-build /app/extracted/application/ ./

# 设置权限
RUN chown -R appuser:appgroup /app

USER appuser

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

EXPOSE 8080

# 使用 exec 形式启动，支持信号传递
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
