FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/atlas-frontend/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
