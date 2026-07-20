<!DOCTYPE html>
<html lang="es">
<head>
    <title>INICIAR SESION</title>
    <script
            src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"
            async
            defer>
    </script>
    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #0f172a;
        }

        .login-body {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }

        .container {
            width: 100%;
            max-width: 440px;
        }

        .card {
            background: white;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }

        .title {
            text-align: center;
            margin-bottom: 5px;
        }

        .input {
            width: 100%;
            padding: 10px;
            margin-bottom: 12px;
            border-radius: 6px;
            border: 1px solid #ccc;
            box-sizing: border-box;
        }

        .btn {
            width: 100%;
            padding: 10px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
        }

        .btn:hover {
            background: #1d4ed8;
        }

        .btn:disabled{
            background: #94a3b8;
            cursor: not-allowed;
            opacity: 0.7;
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
            padding: 10px;
            margin-bottom: 10px;
            border-radius: 6px;
        }

        .captcha-row{
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 16px;
            flex-wrap: nowrap;
        }

        .captcha-btn{
            width: 42px;
            height: 42px;
            border: none;
            border-radius: 10px;
            background: #0f172a;
            color: white;
            cursor: pointer;

            display: flex;
            align-items: center;
            justify-content: center;

            transition: all .2s ease;
            flex-shrink: 0;
        }

        .captcha-btn:hover{
            background: #1e293b;
            transform: rotate(45deg);
        }

        .captcha-btn:active{
            transform: scale(0.97);
        }

        #turnstile-container{
            display: flex;
            align-items: center;
        }

        .verify-actions{
            display: flex;
            justify-content: flex-end;
            margin-bottom: 14px;
        }

        .link-btn{
            background: transparent;
            border: none;
            color: #2563eb;
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
        }

        .link-btn:hover{
            text-decoration: underline;
        }
        
        .link-btn:disabled{
            opacity: 0.6;
            cursor: not-allowed;
        }

    </style>
</head>

<body class="login-body">

<div class="container">

    <div class="card">

        <h1 class="title">INICIE SESION</h1>

        <#if message?has_content>
            <div class="error">
                ${message.summary}
            </div>
        </#if>

        <form id="kc-form-login" action="${url.loginAction}" method="post">

            <label>
                <input
                        id="usernameToValidate"
                        name="username"
                        placeholder="Nombre de Usuario"
                        class="input"
                        value="${login.username!}"
                        autocomplete="username"
                />
            </label>

            <label>
                <input
                        id="passwordToValidate"
                        type="password"
                        name="password"
                        placeholder="Contraseña"
                        class="input"
                        autocomplete="current-password"
                />
            </label>

            <label>
                <input
                        id="codeToVerify"
                        name="codetoverify"
                        placeholder="Código de verificación"
                        class="input"
                />
            </label>

            <label>
                <input
                        id="captchaToken"
                        type="hidden"
                        name="captchatoken"
                />
            </label>

            <label>
                <input
                        id="public-ip"
                        type="hidden"
                        name="public-ip"
                />
            </label>

            <div class="verify-actions">
                <button id="sendCodeButton" type="button" class="link-btn" onclick="sendEmail()">
                    Enviar código al correo
                </button>
            </div>

            <div id="email-status" class="email-status"></div>

            <div class="captcha-row">
                <div id="turnstile-container"></div>
                <button type="button" class="captcha-btn" onclick="refreshCaptcha()">
                    <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round">

                        <path d="M21 2v6h-6"></path>
                        <path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path>
                        <path d="M3 22v-6h6"></path>
                        <path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path>

                    </svg>
                </button>
            </div>

            <button type="submit" class="btn" id="login-btn" disabled>
                Entrar
            </button>

        </form>

    </div>

</div>

<script>

    let widgetId;
    let captchaVerified = false;
    const baseUrl = '/realms/greg-realm/custom-resources';

    function enableLoginButton(){
        document.getElementById('login-btn').disabled = !(captchaVerified);
    }

    function initTurnstile(){

        if(window.turnstile){

            widgetId = window.turnstile.render('#turnstile-container', {
                sitekey: '0x4AAAAAAB76DD3BDlbgNYbl',
                theme: 'light',

                callback: function (solvedToken) {
                    document.getElementById('captchaToken').value = solvedToken;
                    captchaVerified = true;
                    enableLoginButton();
                },

                'expired-callback': function (){
                    document.getElementById('captchaToken').value = '';
                    captchaVerified = false;
                    enableLoginButton();
                }
            });

        } else {
            console.log("Esperando Turnstile...");
            setTimeout(initTurnstile, 500);
        }
    }

    function refreshCaptcha(){
        document.getElementById('captchaToken').value = '';
        captchaVerified = false;
        enableLoginButton();

        if(window.turnstile && widgetId){
            window.turnstile.reset(widgetId);
        }
    }

    async function getPublicIp(){
        try{
            const response = await fetch('https://api.ipify.org?format=json');
            const data = await response.json();
            if(data && data.ip){
                document.getElementById('public-ip').value = data.ip;
            }
        }catch (e) {
            console.error(e);
            document.getElementById('public-ip').value = '0.0.0.0';
        }

    }

    async function sendEmail() {
        const email = document.getElementById('usernameToValidate').value;
        const button = document.getElementById('sendCodeButton');

        try {

            button.disabled = true;
            button.innerText = 'Enviando correo...';

            const url = baseUrl + '/generate-code';
            const response = await fetch(url , {
                method: 'POST',
                headers: {
                    'Content-Type':'application/json'
                },
                body: JSON.stringify({
                    email
                })
            });
            const data = await response.json();

            let seconds = 30;
            if(data.success){
                button.innerText = 'Se ha enviado el correo';
            }
            button.innerText = 'Podrá reenviar en ' + seconds;

            const interval = setInterval( () => {
                seconds--;
                button.innerText = 'Podrá reenviar en ' + seconds;

                if(seconds <= 0){
                    clearInterval(interval);
                    button.disabled = false;
                    button.innerText = 'Enviar código al correo';
                }
            }, 1000);

        }catch (e) {
            console.error(e);
            button.disabled = false;
            button.innerText = 'Enviar código al correo';
        }
    }

    getPublicIp();
    window.addEventListener('load', () => {
        initTurnstile();
    });

</script>

</body>
</html>