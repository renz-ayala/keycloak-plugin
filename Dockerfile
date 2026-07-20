# Step 1: Build de Keycloak plugin y tema
FROM quay.io/keycloak/keycloak:26.5.2 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres

# Copia de JAR y tema a la ruta src/docker
COPY src/docker/keycloak-plugins/*.jar /opt/keycloak/providers/
COPY src/docker/keycloak-themes/greg-theme /opt/keycloak/themes/greg-theme

# Build para registrar las extensiones en Quarkus
RUN /opt/keycloak/bin/kc.sh build

# Step 2: Imagen de producción
FROM quay.io/keycloak/keycloak:26.5.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/

EXPOSE 8080

# En producción se usa 'start --optimized', en local 'start-dev'
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]

# Copy-Item .\target\sso-plugin-1.0.jar .\src\docker\keycloak-plugins