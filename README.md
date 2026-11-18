# Atlas TV Launcher

Launcher Android TV autoral para a TCL BeyondTV2 com Android 11. O Atlas é feito com views nativas em Java e prioriza baixo uso de memória, navegação por controle remoto, acesso rápido às entradas da TV e uma interface editorial própria.

## Estrutura

- `apps/atlas-native/`: aplicativo Android nativo em Java, sem runtime de UI externo.
- `apps/atlas-native/artifacts/`: saída local para APK assinado; binários e chave não entram no Git.
- `docs/aparelho.md`: ficha atual do aparelho e estado da instalação.
- `docs/debloat.md`: inventário e plano de alterações reversíveis.
- `docs/launcher-widgets.md`: recursos e decisões de interface do Atlas.

## Construção

Com JDK 17 e Android SDK 36:

```sh
cd apps/atlas-native
./gradlew assembleRelease
```

O Atlas está instalado como launcher padrão na TV de desenvolvimento e segue em desenvolvimento ativo. Veja o README do aplicativo para recursos, desempenho e detalhes de compilação.
