FROM quay.io/keycloak/keycloak:latest AS builder

ENV KC_HEALTH_ENABLED=true
ENV KC_FEATURES=token-exchange,preview

COPY ./keycloak-plugins /opt/keycloak/providers

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

## docker run --name postgres -e POSTGRES_DB=keycloak -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=keycloak -p 5432:5432 -d postgres:16
## Remove-Item .\src\docker\keycloak-plugins\sso-plugin-1.0.jar
## Copy-Item .\target\sso-plugin-1.0.jar .\src\docker\keycloak-plugins
## cd .\src\docker\
## docker network create --subnet=192.168.250.0/24 keycloak-network
## docker network connect keycloak-network postgres
## docker stop keycloak-sso
## docker rm -f keycloak-sso
## docker build --no-cache -t keycloak-sunarp-custom .
## docker run --name keycloak-sso --network keycloak-network -p 17101:8080 --env-file .env keycloak-sunarp-custom start-dev