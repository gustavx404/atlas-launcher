# Atlas TV nativo

Launcher autoral para Android TV 11, feito com Android SDK e Java, sem AndroidX e sem runtime de UI externo. Usa relógio, manchetes, uma grade lateral de apps e biblioteca de aplicativos, feitos com views nativas.

## Recursos

- Relógio e data locais, atualizados a cada 30 segundos enquanto o Atlas está ativo.
- Estado Wi-Fi/Ethernet e endereço IPv4 local, sem pedir acesso à localização.
- Atalho permanente para as configurações da TV no topo da tela.
- Configurações do Atlas para alternar temas de fundo, relógio, rede e notícias, além de ativar ou desativar cada fonte.
- Até quatro manchetes de CERT.br, CISA, BBC Mundo e DW Brasil, carregadas em segundo plano e armazenadas em cache por 30 minutos.
- Apps fixados podem ser reorganizados na tela inicial pressionando e segurando um atalho.
- O painel do Atlas abre as configurações do Android para escolher o launcher padrão.
- Catálogo deduplicado de apps do usuário e serviços de mídia essenciais; ajustes da TV ficam no acesso superior e utilitários internos são ocultos.
- Entradas passthrough da TV, incluindo AV e HDMI, em uma faixa de acesso rápido no topo da biblioteca.
- Navegação por D-pad, retorno da gaveta com o botão Voltar e indicação visual de foco.
- Identidade visual própria: painel assimétrico “Radar”, barra de rede e ações no topo.
- Molduras Atlas por família de app e conjunto vetorial próprio para configurações, atualizar, adicionar e fechar; os logos instalados continuam reconhecíveis.
- Fundo desenhado por shader estático, sem imagem em tela cheia.
- APK minificado para `armeabi-v7a`, Android 11 ou superior.

## Construção

Com JDK 17 e Android SDK 36:

```sh
./gradlew assembleRelease
```

O workflow `.github/workflows/android.yml` compila o APK em pushes e pull requests. Pushes de tags `v*` também assinam o pacote para a TV e publicam uma GitHub Release com o APK e o checksum SHA-256. A assinatura usa Secrets do repositório e não exige guardar a chave no código.

O APK assinado para a TV de desenvolvimento é mantido localmente em `artifacts/atlas-native-android11-armv7.apk` e não é versionado. O Gradle gera um APK de release sem assinatura; para instalar uma atualização sobre a cópia atual da TV, assine-a com a mesma chave privada, mantida fora deste repositório.

## Desempenho

Na TCL BeyondTV2, o processo mediu aproximadamente 18–24 MB PSS na tela inicial; a leitura varia com o cache, a rede e o estado do sistema. Os ícones usam cache LRU compartilhado limitado a 2 MB, que é esvaziado quando o Android sinaliza pressão de memória.
