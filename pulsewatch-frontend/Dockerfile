# Stage 1: Frontend build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --prefer-offline
COPY . .
RUN npm run build

# Stage 2: Non-root nginx runtime
FROM nginxinc/nginx-unprivileged:alpine

# envsubst is already available in nginx-unprivileged:alpine (ships with nginx).
# No extra package install needed.

# Copy as .template so envsubst can substitute $BACKEND_URL at runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf.template

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/ || exit 1

# At startup: substitute $BACKEND_URL into the nginx config, then start nginx.
# BACKEND_URL must be set in the container environment:
#   Docker Compose : BACKEND_URL=http://backend:8080
#   Azure          : BACKEND_URL=http://pulsewatch-backend
CMD ["/bin/sh", "-c", \
     "envsubst '$BACKEND_URL' < /etc/nginx/conf.d/default.conf.template \
      > /etc/nginx/conf.d/default.conf \
      && nginx -g 'daemon off;'"]
