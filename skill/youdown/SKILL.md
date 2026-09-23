---
name: youdown
description: Memória técnica do projeto YouDown — downloader de vídeo/áudio do YouTube em Java Swing que orquestra yt-dlp e FFmpeg. Use ao trabalhar em qualquer arquivo de src/main/java/YouDown (DownloadEngine, DownloadItem, DownloadPanel, QueuePanel, SettingsPanel, AppConfig, FfmpegManager, Main, MainWindow), ao alterar formatos de download, flags do yt-dlp, parsing de progresso, playlists, empacotamento com Maven/Launch4j ou o fluxo de elevação de administrador.
---

# YouDown — memória do projeto

Aplicação desktop Windows (Java 11 + Swing) que serve de interface gráfica para
o `yt-dlp`, com FFmpeg instalado automaticamente. Todo o texto de UI e os
comentários do código estão **em português do Brasil** — mantenha esse padrão.

## Stack e build

| Item | Valor |
|---|---|
| Java | 11 (`maven.compiler.source/target`) |
| UI | Swing + FlatLaf 3.4 (`FlatDarkLaf`), acento `#FF4444`, fonte `Segoe UI` |
| JSON | `org.json:json:20240303` |
| Build | Maven + `maven-assembly-plugin` (`jar-with-dependencies`) |
| Artefato | `target/YouDown.jar`, main class `YouDown.Main` |
| Distribuição | `YouDown.exe` (Launch4j) com JRE + `yt-dlp.exe` na mesma pasta |

```bash
mvn -f "C:/Users/Guilherme/IdeaProjects/YouDownSource/pom.xml" clean package
```

Não existem testes automatizados — a validação é manual, rodando o jar.

## Arquitetura

Todas as classes ficam no pacote único `YouDown` (`src/main/java/YouDown/`).
Três singletons carregam o estado global: `AppConfig`, `DownloadEngine`,
`FfmpegManager`.

```
Main ──> TermsDialog (1ª execução) ──> MainWindow
                                        ├── DownloadPanel  (aba Baixar)
                                        ├── QueuePanel     (aba Fila)
                                        ├── SettingsPanel  (aba Config)
                                        └── AboutPanel     (aba Sobre)

DownloadPanel ──cria DownloadItem──> DownloadEngine ──ProcessBuilder──> yt-dlp.exe
                                          │                                 │
                                          │<──── stdout linha a linha ──────┘
                                          └── notifica DownloadListener ──> QueuePanel
```

### Classes

- **`Main`** — redireciona `System.out/err` para `~/.youdown/youdown.log`, avisa
  se não estiver como Administrador (a elevação real é feita pelo manifesto do
  Launch4j, **não** por código Java — fazer em Java causava duplo UAC),
  aplica `FlatDarkLaf` e exibe `TermsDialog`.
- **`AppConfig`** — singleton persistido em `~/.youdown/config.json`
  (`defaultDownloadDir`, `ytdlpPath`, `cookiesPath`, `useCookies`,
  `maxConcurrentDownloads`, `darkTheme`, `defaultFormat`, `playlistSubfolder`).
  Falha em `load()` é silenciosa e cai nos defaults.
- **`DownloadEngine`** — singleton com `ExecutorService` de tamanho fixo
  (`maxConcurrentDownloads`, 1–5). Monta a linha de comando do yt-dlp
  (`buildCommand`), lê o stdout em `parseOutputLine` e propaga eventos via
  `DownloadListener` sempre dentro de `SwingUtilities.invokeLater`.
  O tamanho do pool é lido **uma vez** na criação do singleton: mudar a
  configuração só vale na próxima execução.
- **`DownloadItem`** — POJO com os enums `Status` (PENDING, DOWNLOADING,
  CONVERTING, COMPLETED, ERROR, CANCELLED) e `Format` (VIDEO_MP4, VIDEO_BEST,
  AUDIO_MP3, AUDIO_M4A, AUDIO_WAV), ambos com rótulo em português.
  Também guarda o estado de playlist (`playlist`, `playlistTitle`,
  `playlistIndex`, `playlistTotal`, `failedItems`, `currentItemTitle`).
- **`DownloadPanel`** — formulário (URL, formato, pasta, opções de playlist).
  Enter no campo de URL dispara o download.
- **`QueuePanel`** — implementa `DownloadListener` e **recria todos os cards a
  cada evento** (`refreshUI()`), inclusive a cada linha de progresso. Simples,
  porém custoso; ao mexer aqui, lembre que qualquer estado de componente é
  descartado.
- **`SettingsPanel`** — edita o `AppConfig`, testa o yt-dlp e dispara a
  instalação do FFmpeg.
- **`FfmpegManager`** — baixa `ffmpeg-master-latest-win64-gpl.zip` (BtbN builds)
  e extrai o `ffmpeg.exe` para a pasta do executável (`getAppDir()`, derivado do
  `ProtectionDomain`). Considera válido se existir e tiver > 1 MB.
- **`AdminElevation`**, **`TermsDialog`**, **`AboutPanel`** — utilitários de UAC,
  aceite dos termos (`TERMS.md`) e tela institucional/doações.

## Contrato com o yt-dlp

O progresso depende de duas flags — **não remova**:

```
--newline
--progress-template  %(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s|%(progress._total_bytes_str)s
```

`parseOutputLine` reconhece, nesta ordem:

| Linha do yt-dlp | Efeito |
|---|---|
| `[download] Destination:` | define `savedFilePath` e o título |
| `[ExtractAudio] Destination:` | arquivo final de áudio → status CONVERTING (99%) |
| `[Merger] Merging formats into` | arquivo final de vídeo → CONVERTING (99%) |
| `[VideoConvertor]` | CONVERTING (99%) |
| `[download] Downloading playlist:` | nome da playlist |
| `Downloading item N of M` / `video N of M` | índice/total da playlist |
| `\d+\.\d+%|...` | progresso, velocidade, ETA, tamanho |
| `ERROR:` | mensagem de erro (ou contador de falhas na playlist) |

Regras já consolidadas:

- O progresso do download é limitado a **99%**; só o exit code 0 leva a 100%.
- `VIDEO_MP4` usa uma cadeia longa de fallbacks de formato
  (`bestvideo[vcodec^=avc][ext=mp4]+bestaudio[ext=m4a]/…/best`) porque nem todo
  vídeo tem H.264 — não simplifique para `bestvideo+bestaudio`.
- `--embed-thumbnail` exige `--convert-thumbnails jpg`; sem isso o FFmpeg falha
  com thumbnails webp.
- A capa é **só embutida**, mas o yt-dlp grava a imagem em disco antes de
  embutir, **usando o nome do arquivo de mídia**. No sucesso ele mesmo apaga
  (`Deleting original file ... .webp`); quando o item falha, a imagem fica órfã
  na pasta do usuário — era a origem dos `.jpg` soltos.
  `cleanupLeftoverImages()` roda no `finally` de `executeDownload` e remove
  imagens **criadas durante aquele download** na pasta de destino e nas
  subpastas tocadas por ele. O filtro por data protege imagens que o usuário já
  tinha. Não tente resolver isso com `-o thumbnail:` ou `--paths thumbnail:`:
  esses templates só valem para `--write-thumbnail`; no caminho de *embed* o
  yt-dlp os ignora (verificado — a imagem continua caindo junto da mídia).
- `parseOutputLine` ignora `Destination:` com extensão de imagem
  (`isImageFile`): uma capa não pode virar título nem `savedFilePath`, senão uma
  playlist inteiramente falha passaria por concluída.
- `--ffmpeg-location` recebe a **pasta pai** do `ffmpeg.exe`, não o arquivo.
- Vídeo único usa `--no-playlist` + `cleanUrl()` (remove `list=`, `index=`,
  `start_radio=`). Playlist usa `--yes-playlist` + `--ignore-errors` e a URL
  normalizada para `https://www.youtube.com/playlist?list=<ID>`.

## Playlists

- `DownloadEngine.isPlaylistUrl(url)` detecta `list=`, `/playlist?` e URLs de
  canal (`/@nome`, `/channel/`, `/c/`, `/user/`, e as abas `/videos`, `/shorts`,
  `/streams`). O `DownloadPanel` marca a checkbox automaticamente.
- Template de saída em modo playlist:
  `<destino>/<playlist>/%(playlist_index)03d - %(title)s.%(ext)s`
  (a subpasta é opcional, via `AppConfig.playlistSubfolder`).
- **`--extractor-args youtubetab:skip=webpage` é obrigatório.** O YouTube entrega
  a playlist em páginas de 100 itens; lendo pela página web a continuação falha
  e a playlist chega truncada (uma de 153 vídeos rendia exatamente 100, e nem
  `--playlist-items 101-153` alcançava o resto — retornava zero). Forçando a API
  do YouTube a paginação vai até o fim. Sintoma de regressão: total parado em
  múltiplo de 100.
- `--no-overwrites` permite completar uma playlist baixada pela metade sem
  refazer o que já existe. Verificado com `-x --audio-format mp3`: o yt-dlp
  reconhece o `.mp3` final (`has already been downloaded`), não rebaixa e não
  deixa o `.webm` intermediário para trás.
- Faixa de itens opcional (`--playlist-items`): aceita `1-10`, `1,3,5`, `5:`.
- Com `--ignore-errors`, um item quebrado não derruba a fila. O exit code passa a
  ser diferente de zero mesmo com sucesso parcial: a playlist é considerada
  **concluída** se ao menos um item baixou, e `failedItems` guarda a contagem
  para o aviso no card.
- O progresso geral é `((index - 1) * 100 + pct) / total`, limitado a 99%.

## yt-dlp desatualizado é a falha nº 1

O YouTube muda a extração a cada poucas semanas. Um yt-dlp velho não dá erro
claro: ele falha com mensagens cruas que parecem bug do app. Antes de investigar
qualquer "não baixa", rode `yt-dlp --version` — a versão **é** a data do release.

Sintomas observados com o binário de 2026.03.17 (175 dias):

- `unable to download video data: HTTP Error 403: Forbidden`
- `... client https formats require a GVS PO Token`
- `Requested format is not available` / `The page needs to be reloaded`
- `No supported JavaScript runtime could be found`
- playlist truncada e itens falhando em massa (deixando capas `.jpg` órfãs)

Atualizar para 2026.08.19 resolveu todos de uma vez. **Não tente contornar com
`--extractor-args youtube:player_client=...`**: no teste só `web_embedded`
passou, e mesmo assim com formatos limitados — é remendo que o YouTube fecha.
Trocar o runtime JS (`--js-runtimes node`) também não resolveu.

Por isso existe o **`YtdlpUpdater`** (singleton, mesmo padrão do
`FfmpegManager`), chamado por `MainWindow.checkOnStartup`:

- Roda `yt-dlp -U` em segundo plano na abertura, no máximo a cada
  `AppConfig.autoUpdateDays` (padrão 7; o intervalo tem piso de 1 dia para não
  virar verificação a cada abertura). `lastYtdlpCheck` só é gravado quando a
  verificação termina — falha de rede não "queima" a janela.
- Silencioso quando não há nada a fazer: só abre diálogo se atualizou, se falhou
  de um jeito que não é "up to date", ou se o auto-update está **desligado** e o
  binário passou de 90 dias (aí pergunta se quer atualizar).
- `didUpdate()`/`isUpToDate()` leem a saída do `-U`. Cuidado: `"up to date"` é
  substring de várias linhas — não troque por `contains("date")`.
- `awaitIdle()` + a flag `updating` seguram o início de um download enquanto o
  `.exe` está sendo substituído; `DownloadEngine.executeDownload` chama isso
  antes de montar o processo. Sem isso, baixar durante a troca dá erro confuso.

Complementam: o botão **⬆ Atualizar** e o `versionWarning()` (aviso acima de 90
dias no botão Testar) em `SettingsPanel`; e o `updateHint()` do `DownloadEngine`,
que anexa a orientação à mensagem de erro quando ela casa com os sintomas acima.

## Cuidados

- Cada download roda um processo externo; `cancelDownload` usa
  `destroyForcibly()` e o `shutdown()` mata todos os processos ao fechar.
- `queue` é um `ArrayList` acessado pela EDT e pelas threads do executor —
  novas operações em massa devem seguir o mesmo padrão de notificação.
- Nunca coloque credenciais no repositório; o suporte a vídeos restritos é feito
  por arquivo de cookies escolhido pelo usuário (`--cookies`).
- O README exibe dados reais de doação (PIX/PayPal) do autor Guilherme Sampaio;
  não altere sem pedido explícito.
