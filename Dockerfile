# Step 1: Build de Keycloak plugin y tema
FROM quay.io/keycloak/keycloak:26.5.2 AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=postgres
ENV KC_HTTP_ENABLED=true

# Copia de JAR y tema
COPY src/docker/keycloak-plugins/*.jar /opt/keycloak/providers/
COPY src/docker/keycloak-themes/greg-theme /opt/keycloak/themes/greg-theme

# Build optimizado para Quarkus
RUN /opt/keycloak/bin/kc.sh build

# Step 2: Imagen de producción
FROM quay.io/keycloak/keycloak:26.5.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized"]

# Copy-Item .\target\sso-plugin-1.0.jar .\src\docker\keycloak-plugins