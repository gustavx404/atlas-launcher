# Atlas TV · aplicativo Android

Launcher para Android TV 11 ou superior, escrito em Java e baseado em views nativas. Não usa AndroidX nem runtime externo de interface.

## Interface

- Relógio, data, Wi-Fi/Ethernet e endereço IPv4 local.
- Apps fixados com reorganização pelo controle remoto.
- Biblioteca com atividades instaladas e entradas passthrough, como AV e HDMI.
- Painel Radar para segurança digital e cenário global, com fontes configuráveis.
- Configurações próprias para papel de parede e widgets.
- Foco visível e navegação D-pad em todas as áreas principais.

## Implementação e desempenho

- O fundo é desenhado com shaders estáticos, sem decodificar uma imagem em tela cheia.
- Ícones dos apps são enquadrados pelo Atlas, mantendo os símbolos originais reconhecíveis.
- O cache LRU compartilhado dos bitmaps tem limite de 2 MiB e é esvaziado sob pressão de memória.
- O APK de release é minificado e direcionado à ABI `armeabi-v7a`.

## Desenvolvimento

Requisitos: JDK 17 e Android SDK 36.

```sh
./gradlew assembleRelease
```

O APK local sem assinatura fica em `app/build/outputs/apk/release/app-release-unsigned.apk`. O workflow [Android APK](../../.github/workflows/android.yml) executa essa compilação em pushes e pull requests. Tags `v*` geram um APK assinado e uma GitHub Release; a execução manual também pode gerar um APK assinado com `sign_apk=true`.

A assinatura está nos Secrets do repositório. Para instalar atualizações sobre a TV de desenvolvimento, mantenha a mesma chave privada; ela não é armazenada no Git.

## Documentação relacionada

- [Visão geral do Atlas](../../docs/launcher-widgets.md)
- [Ficha do aparelho](../../docs/aparelho.md)
- [Plano de debloat](../../docs/debloat.md)
