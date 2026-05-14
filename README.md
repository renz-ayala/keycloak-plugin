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



