# EnergyAR 0.2 — Inspeção energética interativa

Aplicativo Android acadêmico em Kotlin. O usuário investiga um compressor virtual por componente, explora seu circuito interno e transforma achados simulados em registros e ações. O repositório mantém o nome LinearAR.

## Experiência

### Central de operações

Visão de consumo, potência, custo e equipamentos. Três cenários de treinamento: operação normal, vazamento e funcionamento ocioso. Cada cenário tem referência de potência própria, sem chamar variação energética de eficiência produtiva.

### Laboratório 3D e realidade aumentada

- Compressor original mais detalhado, dividido em reservatório, motor/cabeçote, válvula e manômetro.
- Toque nas peças ou nos pontos numerados para consultar informações e registrar a inspeção.
- **Operação:** equipamento montado com os pontos de inspeção.
- **Raio X:** reservatório oculto, tubulações internas e partículas de fluxo de ar animadas.
- **Desmontar:** separação animada de motor, válvula e manômetro.
- Fuga de ar representada por partículas vermelhas no cenário de vazamento.
- Modo 3D com exploração por câmera virtual; modo AR com detecção de superfície horizontal e ancoragem no mundo real.
- Pontos numerados no AR projetados a partir das coordenadas dos componentes, acompanhando o modelo.
- Controles de giro, tamanho e reposicionamento.

O painel de texto permanece na tela para leitura. Os pontos de seleção acompanham o modelo no AR. O app não reconhece máquinas ou marcadores físicos, não usa sensores e não detecta vazamentos reais.

### Missão de inspeção

Confira quatro componentes e, nos cenários com perda, identifique sua origem. O aplicativo só permite concluir depois dos quatro pontos e da identificação correta. A operação normal exige os quatro pontos, sem inventar um achado.

### Ações e impacto

- Histórico local de inspeções, preservado depois de reiniciar o app.
- Pendências por inspeção e correção simulada, retornando o cenário à operação normal.
- Estimativa mensal com tarifa, horas/dia e dias de operação ajustáveis.
- Compartilhamento de relatório textual pelo menu nativo do Android.

A estimativa usa `(potência observada − referência) × horas/dia × dias/mês × tarifa`, com resultado mínimo zero. É potencial hipotético; não é economia medida nem um comando enviado a máquinas reais. Cada relatório preserva as premissas usadas quando foi salvo.

## Executar

Abra o projeto no Android Studio, sincronize o Gradle e execute o módulo `app`. Use JDK 17 e Android SDK 35. Android mínimo: 7.0.

Para AR, use um dispositivo compatível com ARCore e Google Play Services para RA atualizado. O laboratório 3D funciona sem câmera e permite explorar a experiência em aparelhos sem AR.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions executa as verificações e publica o APK como artefato.

## Roteiro para a apresentação

1. No dashboard, selecione **Vazamento** e toque em **Iniciar inspeção em AR**.
2. Autorize a câmera, detecte uma mesa e toque nela para ancorar o compressor.
3. Experimente **Desmontar** e **Raio X**. Observe o fluxo e a fuga vermelha na válvula.
4. Selecione os quatro componentes, leia suas funções e marque cada inspeção.
5. Na válvula, marque **Origem da perda**. Salve a inspeção.
6. Em **Ações**, ajuste premissas, compartilhe o relatório e simule a correção.
7. Demonstre que o cenário voltou a Normal e que o histórico mantém o achado original.

## Estrutura

- `data/Inspection.kt`: cenários, componentes, estimativas e contrato da inspeção.
- `data/InspectionStore.kt`: histórico e preferências locais.
- `EnergyViewModel.kt`: fluxo e persistência da inspeção.
- `ui/`: interface Compose, dashboard, equipamentos e ações.
- `ar/CompressorRig.kt`: peças, pontos e animações.
- `ar/ARScreen.kt`: laboratório, câmera, ancoragem e projeção dos pontos.
- `tools/generate_models.py`: geração reproduzível de GLBs originais; requer numpy.
- `assets/models/LICENSE.txt`: licença CC0 dos modelos.
- `docs/EnergyAR_Objeto_3D.docx`: proposta inicial do objeto para a disciplina.

## Verificação

Os testes cobrem cálculos, condições de conclusão, referência de ociosidade, recuperação dos registros e navegação das telas sem câmera. As imagens de verificação de interface são geradas em `app/build/ui-check/` pelos testes Robolectric. A interação AR e o desempenho de renderização precisam ser verificados em celular compatível.

Injetora e extrusora possuem acompanhamento de dados; o compressor é o equipamento com gêmeo 3D interativo. Não há integração com IA ou sensores nesta versão.

Referências técnicas: [SceneView 2.3.0](https://github.com/SceneView/sceneview/tree/v2.3.0) e [ARCore](https://developers.google.com/ar/develop).
