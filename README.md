# SMS Forwarder Android App

Este projeto consiste em um aplicativo Android desenvolvido em Kotlin que intercepta SMS recebidos e os reenvia para um script PHP em um servidor web.

## Estrutura do Projeto

### Aplicativo Android
- **Linguagem**: Kotlin
- **IDE**: Android Studio
- **SDK Mínimo**: API 24 (Android 7.0)
- **SDK Alvo**: API 34 (Android 14)

### Componentes Principais

#### 1. MainActivity.kt
- Atividade principal do aplicativo
- Solicita permissões de SMS em tempo de execução
- Interface simples com instruções para o usuário

#### 2. SmsReceiver.kt
- BroadcastReceiver que intercepta SMS recebidos
- Escuta a ação `android.provider.Telephony.SMS_RECEIVED`
- Extrai número do remetente e conteúdo da mensagem
- Chama o SmsSender para enviar dados ao servidor

#### 3. SmsSender.kt
- Classe responsável pelo envio HTTP
- Utiliza OkHttp para requisições POST
- Envia dados para o script PHP de forma assíncrona
- Implementa tratamento de erros

### Backend PHP

#### 1. sms_receiver.php
- Script principal que recebe os dados do SMS
- Valida e sanitiza os dados recebidos
- Salva os SMS em arquivo JSON e log de texto
- Retorna resposta JSON para o aplicativo Android

#### 2. view_sms.php
- Interface web para visualizar SMS recebidos
- Lista os SMS ordenados por data (mais recente primeiro)
- Interface responsiva e amigável

## Permissões Necessárias

O aplicativo requer as seguintes permissões:

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Configuração e Instalação

### 1. Configuração do Aplicativo Android

1. Abra o projeto no Android Studio
2. No arquivo `SmsSender.kt`, altere a constante `PHP_SCRIPT_URL` para a URL do seu servidor:
   ```kotlin
   private const val PHP_SCRIPT_URL = "http://seu_servidor.com/sms_receiver.php"
   ```
3. Compile e instale o aplicativo no dispositivo Android

### 2. Configuração do Servidor PHP

1. Faça upload dos arquivos PHP para seu servidor web:
   - `sms_receiver.php`
   - `view_sms.php`

2. Certifique-se de que o servidor tem permissões de escrita no diretório para criar:
   - `sms_log.txt`
   - `sms_data.json`

### 3. Teste do Sistema

1. Instale o aplicativo no dispositivo Android
2. Conceda as permissões de SMS quando solicitado
3. Envie um SMS para o dispositivo
4. Verifique se o SMS foi recebido acessando `view_sms.php` no navegador

## Funcionalidades

### Aplicativo Android
- ✅ Interceptação automática de SMS recebidos
- ✅ Solicitação de permissões em tempo de execução
- ✅ Envio assíncrono para servidor PHP
- ✅ Tratamento de erros de rede
- ✅ Interface simples e informativa

### Backend PHP
- ✅ Recebimento seguro de dados via POST
- ✅ Validação e sanitização de dados
- ✅ Armazenamento em arquivo JSON
- ✅ Log de atividades
- ✅ Interface web para visualização
- ✅ Suporte a CORS para requisições cross-origin

## Considerações de Segurança

1. **Permissões Sensíveis**: O aplicativo solicita permissões de SMS, que são consideradas sensíveis pelo Android
2. **Validação de Dados**: O script PHP valida e sanitiza todos os dados recebidos
3. **HTTPS Recomendado**: Para produção, use HTTPS para proteger os dados em trânsito
4. **Autenticação**: Considere adicionar autenticação ao script PHP para evitar acesso não autorizado

## Limitações e Considerações

1. **Android 6.0+**: Permissões devem ser concedidas pelo usuário em tempo de execução
2. **Aplicativo Padrão de SMS**: Em algumas versões do Android, pode ser necessário definir o aplicativo como padrão para SMS
3. **Bateria**: O aplicativo pode impactar a duração da bateria se houver muitos SMS
4. **Conectividade**: Requer conexão com a internet para enviar dados ao servidor

## Estrutura de Arquivos

```
android_sms_forwarder/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/androidsmsforwarder/
│       │   ├── MainActivity.kt
│       │   ├── SmsReceiver.kt
│       │   └── SmsSender.kt
│       └── res/layout/
│           └── activity_main.xml
├── build.gradle
├── sms_receiver.php
├── view_sms.php
└── README.md
```

## Dependências

### Android
- androidx.core:core-ktx:1.12.0
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.11.0
- androidx.constraintlayout:constraintlayout:2.1.4
- com.squareup.okhttp3:okhttp:4.12.0

### PHP
- PHP 7.0 ou superior
- Servidor web (Apache/Nginx)
- Permissões de escrita no diretório

## Suporte

Para dúvidas ou problemas:
1. Verifique os logs do Android Studio (Logcat)
2. Verifique os logs do servidor web
3. Confirme se as permissões foram concedidas
4. Teste a conectividade de rede

## Licença

Este projeto é fornecido como exemplo educacional. Use por sua própria conta e risco, respeitando as leis locais sobre privacidade e interceptação de mensagens.

