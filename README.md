<div align="center">
  <img src="docs/atlas-mark.svg" width="104" alt="Símbolo do Atlas TV" />
  <h1>Atlas TV</h1>
  <p>Launcher nativo e leve para Android TV · Lightweight native Android TV launcher</p>
  <p>
    <a href="https://github.com/gustavx404/atlas-launcher/actions/workflows/android.yml"><img src="https://github.com/gustavx404/atlas-launcher/actions/workflows/android.yml/badge.svg?branch=main" alt="Android APK workflow" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" alt="Apache License 2.0" /></a>
    <img src="https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white" alt="Android 11 ou superior" />
    <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17" />
    <img src="https://img.shields.io/badge/ABI-armeabi--v7a-6574cd" alt="ARMv7" />
  </p>
</div>

## O projeto

O Atlas é um launcher Android TV escrito em Java com componentes nativos do Android. Ele reúne apps, entradas de vídeo, rede, relógio e notícias em uma interface própria, com navegação pensada para controle remoto e aparelhos com recursos limitados.

**In English:** Atlas TV is a lightweight Android TV launcher for Android 11+ devices. Built with Java and native Android views, it includes a D-pad app library, HDMI/AV inputs, network status, configurable widgets, and an in-app news reader. Designed for ARMv7 TVs and low-resource hardware.

<p align="center">
  <img src="apps/atlas-native/screenshots/home.png" alt="Tela inicial do Atlas TV em uma Android TV" width="100%" />
</p>

## O que oferece

- **Tela inicial:** relógio, data, estado da rede, atalhos reorganizáveis e painel Radar.
- **Biblioteca:** apps instalados e entradas AV/HDMI, acessíveis pelo D-pad.
- **Personalização:** papel de parede, widgets e fontes de notícias configuráveis.
- **Interface Atlas:** ícones enquadrados em uma linguagem visual comum, vetores próprios e foco visível.
- **Leveza:** views nativas, fundo desenhado por shader e cache de ícones limitado.

## Instalar para testar

Ainda não há uma release estável publicada. Para gerar um APK de teste assinado com a chave de depuração do Android:

```sh
git clone https://github.com/gustavx404/atlas-launcher.git
cd atlas-launcher/apps/atlas-native
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requer uma Android TV com Android 11 ou superior e suporte à ABI ARMv7 (`armeabi-v7a`). A instalação por ADB requer depuração USB/rede ativada e autorizada na TV. O APK de teste é assinado com a chave debug local; uma instalação feita por outra pessoa não recebe atualizações assinadas com a sua chave.

O [workflow Android APK](https://github.com/gustavx404/atlas-launcher/actions/workflows/android.yml) também compila o projeto. Releases assinadas serão publicadas na página de [Releases](https://github.com/gustavx404/atlas-launcher/releases) quando estiverem disponíveis.

## Contribuir

Relate problemas ou envie melhorias por [Issues e Pull Requests](https://github.com/gustavx404/atlas-launcher). Consulte o [guia de contribuição](CONTRIBUTING.md) para preparar o ambiente e validar uma alteração.

## Licença

O código e a documentação do Atlas são distribuídos sob a [Apache License 2.0](LICENSE). Nomes, marcas, ícones de aplicativos e conteúdo de terceiros usados em capturas de tela ou feeds continuam pertencendo aos respectivos titulares.

## Compilar

Requisitos: JDK 17 e Android SDK com a plataforma 36.

```sh
cd apps/atlas-native
./gradlew assembleRelease
```

O APK local é gerado em `app/build/outputs/apk/release/app-release-unsigned.apk`.

## CI e releases

O [workflow Android APK](https://github.com/gustavx404/atlas-launcher/actions/workflows/android.yml) compila em pushes para `main`, pull requests e execuções manuais. O artefato de CI contém o APK sem assinatura e o checksum SHA-256.

Para uma release instalável, envie uma tag `v*`:

```sh
git tag v0.2.1
git push origin v0.2.1
```

O workflow assina o APK e cria uma [GitHub Release](https://github.com/gustavx404/atlas-launcher/releases) com o pacote e seu checksum. Uma execução manual com `sign_apk=true` gera um APK assinado sem publicar uma release.

A assinatura usa Secrets do GitHub Actions. A chave privada não fica neste repositório; é necessário preservar a mesma chave para atualizar uma instalação existente. O certificado configurado é o da TV de desenvolvimento e não é uma chave de publicação da Play Store.

## Organização

```text
apps/atlas-native/   Aplicativo Android e instruções técnicas
docs/                Documentação do aparelho e decisões do launcher
.github/workflows/   Compilação e publicação de APKs
```

Consulte [apps/atlas-native/README.md](apps/atlas-native/README.md) para detalhes da interface, implementação e desempenho.
