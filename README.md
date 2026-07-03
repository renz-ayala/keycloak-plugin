# Configuracion de SSO con Keycloak y plugin en Java
Este repositorio contiene principalmente el plugin SPI para Keycloak hecho en java.

Desarrollado con :
* **Java 21**
* **Keycloak 26+**
---

## Detalle importante:
El proyecto esta hecho para compilar el plugion SPI, pero contiene tambien la configuración para levantarlo en un entorno docker. 
Asi tambien contiene el .ftl para el diseño del login.
---

## Requisitos Previos
* Java SE Development Kit (JDK) 21.
* Docker Desktop instalado y en ejecución.

---

## Guía de Despliegue Local
Debe ubicarse en la raíz de proyecto, y seguir los siguientes pasos:

### 1. Ubique el archivo Dockerfile
Keycloak necesita conectase a una base postgres para funcionar, dirijase a la ruta src/docker.Y ejecute lo siguiente:
```bash
docker run --name postgres -e POSTGRES_DB=keycloak -e POSTGRES_USER=keycloak -e POSTGRES_PASSWORD=keycloak -p 5432:5432 -d postgres:16
docker network create --subnet=192.168.250.0/24 keycloak-network
docker network connect keycloak-network postgres
```

### 2. Compilación y Construcción del SPI
Ejecute el package de maven. Y luego proceda con el comando:
```bash
Copy-Item .\target\sso-plugin-1.0.jar .\src\docker\keycloak-plugins
```
y luego dirijase a la carpeta de docker:
```bash
cd .\src\docker\
docker build --no-cache -t keycloak-sunarp-custom .
docker run --name keycloak-sso --network keycloak-network -p 17101:8080 -v .\keycloak-themes:/opt/keycloak/themes --env-file .env keycloak-sunarp-custom start-dev
```

## Información extra

1. Link para descargar el ojdbc:
https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html

2. En ( Realm settings -> Security defenses -> Content-Security-Policy ) colocar:
frame-src 'self' https://challenges.cloudflare.com;  frame-ancestors 'self';  object-src 'none';  script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com;  connect-src 'self' https://challenges.cloudflare.com https://api.ipify.org;  style-src 'self' 'unsafe-inline';

3. Configuracion de Authenticaton - Required actions:
<img width="1823" height="1024" alt="image" src="https://github.com/user-attachments/assets/974e1bad-ff07-4722-82b1-a37bb63e9990" />
<img width="1833" height="581" alt="image" src="https://github.com/user-attachments/assets/3339d743-0c6c-44b9-8960-06ccf01b3335" />

4. Cada cliente debe tener configurado el "Valid redirect URIs" y el "Web origins".

5. Añadir en Client Scopes -> profile -> Mappers -> 4 nuevos mappers  (Add mapper -> By Configuration ->  User Session Note -> [ipAddress, documentType, documentNumber, clientId]). {
     add to id token : On.
     add to access token : On,
     add to userinfo : On
   }



