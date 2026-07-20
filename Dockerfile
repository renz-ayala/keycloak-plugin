FROM quay.io/keycloak/keycloak:26.5.2 AS builder
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
COPY src/docker/keycloak-plugins/*.jar /opt/keycloak/providers/
COPY src/docker/keycloak-themes/greg-theme /opt/keycloak/themes/greg-theme
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.5.2
COPY --from=builder /opt/keycloak/ /opt/keycloak/
EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--http-port=8080"]