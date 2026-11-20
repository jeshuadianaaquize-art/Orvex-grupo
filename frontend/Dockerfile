# Contexto de build: ./frontend
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG NEXT_PUBLIC_MOSTRAR_DEMO=false
ENV NEXT_PUBLIC_MOSTRAR_DEMO=$NEXT_PUBLIC_MOSTRAR_DEMO
# Dirección interna del gateway a la que la web reenvía /api
ARG API_INTERNA_URL=http://gateway:8080
ENV API_INTERNA_URL=$API_INTERNA_URL
RUN npm run build
EXPOSE 3000
CMD ["npm", "run", "start"]
