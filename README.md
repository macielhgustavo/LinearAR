# LinearAR — EnergyAR

Aplicativo Android acadêmico para visualizar dados simulados de energia industrial e posicionar um compressor 3D no ambiente real. O repositório se chama LinearAR; o aplicativo mantém o nome EnergyAR do documento entregue à disciplina.

## Implementação inicial

- Kotlin, Jetpack Compose, Material 3 e ViewModel.
- Dashboard com potência, consumo diário e custo estimado.
- Lista de máquinas, detalhes, gráfico de seis amostras e alertas.
- Cenário normal ou consumo elevado do compressor, compartilhado entre telas.
- ARCore com SceneView 2.3.0: detecção de superfície horizontal, toque para posicionar, rotação e escala por gestos, toque no modelo para abrir dados e reposicionamento.
- Compressor original em GLB, com CC0 registrada ao lado do arquivo.

Os números são fixtures de demonstração. kW indica potência instantânea; kWh indica energia acumulada. Alternar o cenário modifica a potência e a última amostra, sem alterar o consumo já acumulado. Os alertas usam desvio em relação à referência: atenção a partir de 10%, consumo elevado a partir de 20%. Não representam diagnóstico real ou eficiência produtiva medida.

## Abrir e executar

1. Abra a pasta do projeto no Android Studio e permita a sincronização Gradle.
2. Use JDK 17 e instale Android SDK 35. O Gradle Wrapper está incluído.
3. Conecte um celular Android 7.0 ou superior com depuração USB ativada.
4. Execute o módulo `app`.
5. Para usar AR, o celular precisa ser compatível com ARCore e ter Google Play Services para RA instalado/atualizado. As outras telas podem funcionar sem suporte AR.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

O APK de desenvolvimento é gerado em `app/build/outputs/apk/debug/app-debug.apk`. O workflow Android também publica esse APK como artefato após um build bem-sucedido.

## Roteiro de apresentação

1. Abra o dashboard e destaque o aviso de dados simulados.
2. Consulte os detalhes do Compressor 01 e o gráfico de potência.
3. Abra AR e autorize a câmera; movimente o celular diante de uma mesa iluminada.
4. Toque na superfície detectada para colocar o compressor.
5. Use dois dedos para girar e ajustar o tamanho; toque no modelo para abrir o painel.
6. Ative o consumo elevado e mostre a mudança de 30,0 para 38,4 kW, com desvio de 28%.
7. Abra Alertas para consultar a orientação.

## Escopo e próximos passos

Esta versão usa posicionamento em superfície e um painel Compose na tela, associado ao modelo selecionado. Ainda não reconhece marcadores impressos ou máquinas físicas, não ancora o painel em 3D e não recebe dados de sensores. Esses recursos poderão ser adicionados depois que a apresentação básica for validada no aparelho.

O compressor é o único objeto GLB inicial. Injetora e extrusora possuem telas de dados, mas ainda não têm modelos 3D.

## Arquivos e referências

- `app/src/main/java/br/com/linear/energyar/data`: modelos, regras e dados simulados.
- `app/src/main/java/br/com/linear/energyar/ui`: telas Compose.
- `app/src/main/java/br/com/linear/energyar/ar`: cena AR e permissão de câmera.
- `app/src/main/assets/models/compressor.glb`: modelo original, com `LICENSE.txt` (CC0 1.0).
- `docs/EnergyAR_Objeto_3D.docx`: definição do objeto para a disciplina.
- [SceneView 2.3.0 — exemplo oficial](https://github.com/SceneView/sceneview/tree/v2.3.0/samples/ar-model-viewer-compose).
- [Dispositivos compatíveis com ARCore](https://developers.google.com/ar/devices).

A interação AR exige verificação presencial em aparelho compatível. Compilação e testes de regras não substituem essa verificação.
