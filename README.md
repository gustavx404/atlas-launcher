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

O workflow do GitHub Actions também compila o APK em pushes para `main`, pull requests e execuções manuais. Esses builds geram um APK sem assinatura como artefato temporário.

Para publicar uma atualização instalável na TV, crie e envie uma tag `v*` (por exemplo, `v0.2.1`). O workflow assina o APK com os Secrets do repositório e cria uma GitHub Release com o APK e seu SHA-256. A chave privada não fica no código. O segredo atual corresponde à chave de desenvolvimento já instalada na TV; mantenha-a para que futuras atualizações continuem compatíveis.

Secrets necessários para releases:

- `ATLAS_KEYSTORE_BASE64`
- `ATLAS_KEYSTORE_PASSWORD`
- `ATLAS_KEY_ALIAS`
- `ATLAS_KEY_PASSWORD`

O Atlas está instalado como launcher padrão na TV de desenvolvimento e segue em desenvolvimento ativo. Veja o README do aplicativo para recursos, desempenho e detalhes de compilação.
