FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
    && apt-get install -y curl ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package.json ./
COPY server.mjs ./
COPY index.html ./
COPY js ./js
COPY css ./css
COPY assets ./assets
COPY vendor ./vendor
COPY scripts ./scripts
COPY backend ./backend

RUN chmod +x backend/gradlew

RUN mkdir -p /app/backend/data

ENV NODE_ENV=production
ENV PORT=4173
ENV BACKEND_PORT=8080
ENV HOST=0.0.0.0

EXPOSE 4173

CMD ["node", "scripts/full-stack.mjs"]
