# ---- Etapa 1: build ----
FROM node:20-alpine AS build
WORKDIR /app

# Copiamos manifests primero para cachear la instalación de dependencias.
COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# ---- Etapa 2: runtime ----
# nginx-alpine sirve los estáticos y proxy-pasa /api al backend.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
