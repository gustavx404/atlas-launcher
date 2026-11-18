# Plano de debloat

Este arquivo é um inventário, não uma lista universal de pacotes para remover. O mesmo pacote pode ser necessário em um modelo e dispensável em outro.

O aparelho informado é uma TCL P715. Ainda precisamos da variante/tamanho, versão exata do Android e firmware, além do inventário ADB deste aparelho. Não reutilizar comandos de outra TCL ou de Chromecast: os nomes e dependências de pacotes podem diferir.

## Regras de trabalho

1. Guardar um inventário inicial dos pacotes e uma cópia deste plano.
2. Identificar cada pacote pelo nome e confirmar sua função para este modelo/firmware.
3. Fazer uma mudança por vez e verificar inicialização, controle remoto, HDMI/CEC, áudio, rede, atualizações e streaming protegido.
4. Registrar o comando aplicado e o comando de reversão.
5. Preferir desativação ou remoção apenas para o usuário atual quando suportado.
6. Medir primeiro o espaço livre e observar o desempenho atual; um launcher alternativo simplifica a tela inicial, mas não garante acelerar todos os apps ou o sistema.

## Inventário

| Pacote | Função confirmada | Ação planejada | Comando aplicado | Como reverter | Verificado |
| --- | --- | --- | --- | --- | --- |

## Comandos ADB

Adicione comandos somente depois de confirmar o modelo, o pacote e a forma de reversão. Não execute comandos copiados de listas genéricas.
