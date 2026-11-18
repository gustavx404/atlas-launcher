# Contribuindo com o Atlas TV

Obrigado por ajudar a melhorar o Atlas. Issues e pull requests são bem-vindos.

## Antes de começar

- Procure issues existentes antes de abrir uma nova.
- Em relatos de bugs, informe o modelo da TV, a versão do Android, os passos para reproduzir e o resultado esperado. Remova dados pessoais de screenshots e logs.
- Para mudanças visuais, inclua uma captura de tela. Para navegação, descreva como o fluxo funciona com o controle remoto.

## Preparar e compilar

Requer JDK 17 e Android SDK com a plataforma 36. O APK debug pode ser compilado assim:

```sh
cd apps/atlas-native
./gradlew assembleDebug
```

O projeto tem como alvo Android 11 (API 30+) e ARMv7. Preserve o uso de views nativas, a navegação por D-pad e o funcionamento em aparelhos com pouca memória.

## Pull requests

- Faça uma alteração focada e explique o problema que ela resolve.
- Mantenha os padrões de Java e a estrutura que o projeto já usa.
- Não inclua chaves de assinatura, credenciais ou dados pessoais.
- Descreva como compilou e, quando aplicável, como conferiu a mudança na TV.

Ao enviar uma contribuição intencionalmente para inclusão no Atlas, você a disponibiliza sob os termos da Apache License 2.0, conforme a seção 5 dessa licença, salvo acordo separado por escrito.
