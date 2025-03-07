# Google Sign-In Library

**[English version here](https://github.com/Jao-Viquitor/Google-signIn-lib/blob/master/README.md)**

Bem-vindo à `google-signin-lib`, uma biblioteca Kotlin leve, segura e genérica para implementar autenticação com o Google em aplicativos Android usando o Credential Manager API. Projetada para ser simples (uso em 1-2 linhas), robusta e reutilizável, esta biblioteca segue os princípios SOLID, Clean Code e Clean Architecture, oferecendo uma integração fluida com o "Sign in with Google" enquanto fornece dados detalhados do payload para uso no cliente e validação no backend.

## Índice
1. [Funcionalidades](#funcionalidades)
2. [Pré-requisitos](#pré-requisitos)
3. [Instalação](#instalação)
4. [Uso](#uso)
   - [Uso Básico](#uso-básico)
   - [Uso Avançado](#uso-avançado)
   - [Exemplo com Validação no Backend](#exemplo-com-validação-no-backend)
5. [Dados Retornados](#dados-retornados)
6. [Configuração](#configuração)
7. [Segurança](#segurança)
8. [Solução de Problemas](#solução-de-problemas)
9. [Contribuição](#contribuição)
10. [Licença](#licença)

---

## Funcionalidades
- **Simplicidade**: Login com Google em apenas 1-2 linhas de código.
- **Segurança**: Usa nonce para prevenir ataques de repetição e verifica o `idToken` no cliente.
- **Flexibilidade**: Retorna dados completos do payload (email, nome, foto, subject ID, issuer, audience).
- **Genérica**: Não depende de frameworks específicos ou modelos proprietários.
- **Suporte a Fluxos**: Login automático, signup e logout.
- **Arquitetura Limpa**: Segue SOLID e Clean Architecture para manutenção e extensibilidade.

---

## Pré-requisitos
Antes de usar a biblioteca, o desenvolvedor do aplicativo deve configurar o ambiente no [Google Cloud Console](https://console.cloud.google.com/):
1. **Crie um Projeto**:
   - Acesse o Google Cloud Console.
   - Crie um novo projeto ou use um existente.
2. **Configure o OAuth 2.0**:
   - Vá para "APIs & Services" > "Credentials".
   - Crie um "OAuth Client ID" do tipo "Web application" para obter o `webClientId`.
   - Associe o `webClientId` ao seu aplicativo Android fornecendo o nome do pacote e a assinatura SHA-1.
3. **Habilite a API**:
   - Ative a "Google Identity API" (se necessário).
4. **Informações de Marca**:
   - Adicione nome do app, logotipo e URLs de política de privacidade/termos de serviço na seção "OAuth consent screen".

Você precisará do `webClientId` gerado para configurar a biblioteca.

---

## Instalação
Adicione a dependência ao arquivo `build.gradle` do seu módulo:
```gradle
dependencies {
    implementation 'com.viquitor:google-signin-lib:1.0.0'
}
```

Certifique-se de sincronizar o projeto no Android Studio após adicionar a dependência.

### Dependências Internas
A biblioteca utiliza as seguintes bibliotecas:
- `androidx.credentials:credentials:1.2.0`
- `androidx.credentials:credentials-play-services-auth:1.2.0`
- `com.google.android.libraries.identity.googleid:googleid:1.1.0`
- `com.google.auth:google-auth-library-oauth2-http:1.23.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
- `com.google.code.gson:gson:2.10.1`

---

## Uso

### Uso Básico
Realize o login com o Google em apenas 1-2 linhas dentro de um escopo de corrotina:
```kotlin
val googleSignIn = GoogleSignIn.Builder(context).setWebClientId("SEU_WEB_CLIENT_ID").build()
val result = googleSignIn.signIn() // Chame em um CoroutineScope (ex.: lifecycleScope)

when (result) {
    is SignInResult.Success -> println("Logado: ${result.email}, Subject ID: ${result.subjectId}")
    is SignInResult.Failure -> println("Erro: ${result.errorMessage}")
}
```

### Uso Avançado
Personalize o fluxo de login e acesse todos os dados do payload:
```kotlin
import kotlinx.coroutines.launch

val googleSignIn = GoogleSignIn.Builder(context)
    .setWebClientId("SEU_WEB_CLIENT_ID")
    .setFilterByAuthorizedAccounts(false) // Para signup (novos usuários)
    .setAutoSelectEnabled(true)           // Login automático para usuários recorrentes
    .build()

lifecycleScope.launch {
    when (val result = googleSignIn.signIn()) {
        is SignInResult.Success -> {
            println("E-mail: ${result.email}")
            println("Nome Completo: ${result.displayName}")
            println("Nome: ${result.givenName} ${result.familyName}")
            println("Foto de Perfil: ${result.profilePictureUri}")
            println("Subject ID: ${result.subjectId}")
            println("Issuer: ${result.issuer}")
            println("Audience: ${result.audience}")
            // Validação no backend
            validarTokenNoBackend(result.idToken)
        }
        is SignInResult.Failure -> println("Falha no login: ${result.errorMessage}")
    }
}

// Logout
googleSignIn.signOut()
```

### Exemplo com Validação no Backend
Envie o `idToken` para seu backend para validação definitiva:
```kotlin
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.Body

// Interface da API
interface AuthApi {
    @POST("auth/google")
    suspend fun validateGoogleToken(@Body token: Map<String, String>): Response<User>
}

// Uso
val googleSignIn = GoogleSignIn.Builder(context).setWebClientId("SEU_WEB_CLIENT_ID").build()

lifecycleScope.launch {
    val result = googleSignIn.signIn()
    when (result) {
        is SignInResult.Success -> {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://seuservidor.com/")
                .build()
            val authApi = retrofit.create(AuthApi::class.java)
            try {
                val response = authApi.validateGoogleToken(mapOf("idToken" to result.idToken))
                if (response.isSuccessful) {
                    println("Usuário autenticado: ${response.body()}")
                } else {
                    println("Erro na validação do backend")
                }
            } catch (e: Exception) {
                println("Erro na chamada ao backend: ${e.message}")
            }
        }
        is SignInResult.Failure -> println("Erro no login: ${result.errorMessage}")
    }
}
```

---

## Dados Retornados
A biblioteca retorna um `SignInResult`, uma classe selada com dois estados:

### `SignInResult.Success`
Contém os dados do usuário autenticado:
- `idToken: String`: Token para validação no backend.
- `email: String?`: E-mail do usuário (pode ser nulo).
- `givenName: String?`: Nome dado do usuário (pode ser nulo).
- `familyName: String?`: Sobrenome do usuário (pode ser nulo).
- `displayName: String?`: Nome completo do usuário (pode ser nulo).
- `profilePictureUri: String?`: URL da foto de perfil (pode ser nulo).
- `subjectId: String`: Identificador único do usuário (`sub` do payload).
- `issuer: String`: Emissor do token (`iss`, ex.: `https://accounts.google.com`).
- `audience: String`: Audiência do token (`aud`, geralmente o `webClientId`).

### `SignInResult.Failure`
Contém a mensagem de erro:
- `errorMessage: String`: Descrição do problema (ex.: "Falha no login: Token inválido").

---

## Configuração
A classe `GoogleSignIn.Builder` permite personalizar o comportamento:
- `setWebClientId(clientId: String)`: Define o `webClientId` (obrigatório).
- `setFilterByAuthorizedAccounts(filter: Boolean)`:
    - `true` (padrão): Lista apenas contas previamente autorizadas.
    - `false`: Permite signup com qualquer conta Google.
- `setAutoSelectEnabled(autoSelect: Boolean)`:
    - `true` (padrão): Ativa login automático para usuários com uma única conta válida.
    - `false`: Exige seleção manual da conta.

Exemplo:
```kotlin
val googleSignIn = GoogleSignIn.Builder(context)
    .setWebClientId("SEU_WEB_CLIENT_ID")
    .setFilterByAuthorizedAccounts(false)
    .setAutoSelectEnabled(false)
    .build()
```

---

## Segurança
- **Nonce**: Um nonce único é gerado automaticamente para cada requisição, prevenindo ataques de repetição.
- **Verificação no Cliente**: A biblioteca usa `GoogleIdTokenVerifier` para validar a assinatura do `idToken` com as chaves públicas do Google.
- **Validação no Backend**: O `idToken` é retornado para validação definitiva no servidor, essencial para segurança total.
- **Encapsulamento**: Dados sensíveis são expostos apenas via `SignInResult` e não registrados em logs.

**Aviso**: A validação no cliente é preliminar e serve para conveniência (ex.: exibir dados do usuário). Sempre valide o `idToken` no backend para garantir a integridade e autenticidade da sessão.

---

## Solução de Problemas
- **Erro: "Falha no login: Token inválido"**:
    - Verifique se o `webClientId` está correto e associado ao pacote/SHA-1 do app.
- **Nenhuma conta exibida**:
    - Certifique-se de que `filterByAuthorizedAccounts` está configurado corretamente (`false` para signup).
- **Login automático não funciona**:
    - Confirme que `setAutoSelectEnabled(true)` está ativado e que há uma única conta válida no dispositivo.
- **Erro de rede na verificação**:
    - Garanta que o dispositivo tem conexão com a internet, pois a verificação requer acesso às chaves públicas do Google.

Para mais ajuda, consulte a [documentação oficial do Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg) ou abra uma issue no repositório.

---

## Contribuição
Contribuições são bem-vindas! Siga estes passos:
1. Faça um fork do repositório.
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`).
3. Commit suas alterações (`git commit -m "Adiciona nova funcionalidade"`).
4. Push para a branch (`git push origin feature/nova-funcionalidade`).
5. Abra um Pull Request.

Por favor, inclua testes e atualize a documentação conforme necessário.

---

## Licença
Distribuído sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.
