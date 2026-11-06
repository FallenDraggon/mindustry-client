![Logo](foo.png)

[![Release Update](../../workflows/Release%20Update/badge.svg)](https://mindustry-antigrief.github.io/mindustry-client)
[![Tests (Unstable)](../../workflows/Java%20Tests/badge.svg?branch=v8)](https://mindustry-antigrief.github.io/mindustry-client-v8-builds)
[![Discord](https://img.shields.io/discord/741710208501547161.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/yp9ZW7j)

# Что добавлено в этой сборке (FD)
Сборка основана на [mindustry-client](https://github.com/mindustry-antigrief/mindustry-client). Основной код лежит в [`core/src/mindustry/client/fallen`](core/src/mindustry/client/fallen).

**Где это найти в игре:**
- **FD-панель** на HUD ([`PanelFragment`](core/src/mindustry/client/ui/PanelFragment.java)) открывается на `CapsLock`. ПКМ по многим кнопкам открывает настройки или включает постоянный режим.
- **Настройки клиента, раздел FD Settings**: включение окон и уведомлений.
- **Окно Some Things** ([`TrashDialog`](core/src/mindustry/client/fallen/TrashDialog.java)): автоперенос, автострельба, ассист, свет и размеры окон.

### Стройка и логистика
| Фишка | Что делает | Код |
| --- | --- | --- |
| Авторасстановка буров | `H` → клик по жиле → выбор бура → клик по точке вывода. Сама строит буры, ряды конвейеров и маршрут до точки | [`AutoDrillPlacer`](core/src/mindustry/client/fallen/AutoDrillPlacer.java) |
| Авто-мосты через пластаниум | Перекидывает мосты через линию пластаниевых конвейеров, может переносить мешающие энергоузлы | [`AutoBridgeHandler`](core/src/mindustry/client/fallen/AutoBridgeHandler.java) |
| Починка энергосетей | Подключает сети с дефицитом к сетям с избытком. ЛКМ — разово, ПКМ — постоянно | [`PowerFixer`](core/src/mindustry/client/fallen/PowerFixer.java) |
| Стройка схемы юнитом | Свободный поли или мега строит схему через микропроцессор, клиент досылает конфиги. Кнопка в окне схемы | [`UnitSchemBuilder`](core/src/mindustry/client/fallen/UnitSchemBuilder.java) |
| Осадная дуга рябей | Ставит риппли с линией подачи по краю зоны обстрела вражеских турелей | [`RippleSiegePlacer`](core/src/mindustry/client/fallen/RippleSiegePlacer.java) |
| Схемы с очисткой места | Сносит блоки, мешающие схеме, и доворачивает и донастраивает уже стоящие | [`CustomBuildLogic`](core/src/mindustry/client/fallen/CustomBuildLogic.java) |
| Быстрые схемы | Окно со вкладками и слотами схем, перетаскивание слотов, импорт и экспорт в zip | [`QuickSchemFrag`](core/src/mindustry/client/fallen/QuickSchemFrag.java) |
| Пресеты террейна | Сохранение участков пола и руды для «Редактировать в игре»: Alt + выделение схемы | [`TerrainPresetsFrag`](core/src/mindustry/client/fallen/TerrainPresetsFrag.java) |

### Юниты и автоматизация
| Фишка | Что делает | Код |
| --- | --- | --- |
| Miners AI | Распределяет моно, поли, пульсаров, квазаров и мег по ресурсам: квоты, веса, кризисные режимы, автопочинка, помощь в стройке, обход вражеских пушек | [`miners/`](core/src/mindustry/client/fallen/miners) |
| Автопилот своего юнита | Строит, помогает игрокам и чинит, а при нехватке ресурсов копает. Белый и чёрный списки игроков | [`assistai/`](core/src/mindustry/client/fallen/assistai) |
| Умная автострельба | Приоритеты целей, ручной выбор цели (`Space` + ЛКМ), показ дальности и прицела | [`FDAutoShoot`](core/src/mindustry/client/fallen/FDAutoShoot.java) |
| Автомега | Раз в 30 секунд садится в свободную мегу команды | [`AutoMega`](core/src/mindustry/client/fallen/AutoMega.java) |
| Автоперенос | Фильтры целей, включая генераторы; учитывает кулдаун сдачи на сервере | [`AutoTransfer`](core/src/mindustry/client/utils/AutoTransfer.kt) |
| Круговой ассист | Разворачивается у границы зоны огня, чтобы не тянуть турели на игрока | [`AssistPath`](core/src/mindustry/client/navigation/AssistPath.kt) |
| Управление юнитами | Снять выделение с половины отряда, выбрать весь последний выбранный тип | [`DesktopInput`](core/src/mindustry/input/DesktopInput.java) |

### Информация на экране
| Фишка | Что делает | Код |
| --- | --- | --- |
| Эффективность блоков | Проценты работы производства и реальная пропускная способность логистики, фильтры по типам блоков | [`EfficiencyFilter`](core/src/mindustry/client/ui/EfficiencyFilter.java), [`ThroughputTracker`](core/src/mindustry/client/fallen/ThroughputTracker.java) |
| Анализатор производства | Входы и выходы ресурсов в выделенной области | [`ProductionAnalyzerFrag`](core/src/mindustry/client/fallen/ProductionAnalyzerFrag.java) |
| Окно второй камеры | Показывает выбранную часть карты, умеет следовать за пингами игроков | [`CameraViewFrag`](core/src/mindustry/client/fallen/CameraViewFrag.java) |
| Тревога базы | Знаки и линии до мест урона вне хилок и разрушений | [`BaseAlert`](core/src/mindustry/client/fallen/BaseAlert.java) |
| Инфо о волнах | Состав волн, HP и щиты, три режима показа | [`WaveInfoFrag`](core/src/mindustry/client/fallen/WaveInfoFrag.java) |
| Инфо о карте | Правила, множители, ресурсы, команды, переход к скоплениям блоков | [`MapInfoFrag`](core/src/mindustry/client/fallen/MapInfoFrag.java) |
| Предметы и жидкости в мостах | Отрисовка содержимого мостов | [`ItemBridge`](core/src/mindustry/world/blocks/distribution/ItemBridge.java), [`LiquidBridge`](core/src/mindustry/world/blocks/liquid/LiquidBridge.java) |
| Скан лент | При наведении рисует, откуда и куда идут предметы и жидкости | [`TransportScan`](core/src/mindustry/client/morj/TransportScan.java) |
| «Глаз Саурона» | Кнопки панели: юниты, ядра, спавны, войды, источники, мировые процессоры | [`PanelFragment`](core/src/mindustry/client/ui/PanelFragment.java) |

### Логи и антигриф
| Фишка | Что делает | Код |
| --- | --- | --- |
| Логи действий | История приказов и смен режима юнитов, доставки ресурсов, тревоги о смертях и подозрительных блоках | [`ActivityLogger`](core/src/mindustry/client/fallen/ActivityLogger.java), [`ActionsHistory`](core/src/mindustry/client/fallen/ActionsHistory.java) |
| Планы игроков и смерти юнитов | Отображение блоков, поставленных игроком, и мест смертей юнитов | [`HistoryRenderer`](core/src/mindustry/client/fallen/HistoryRenderer.java), [`PlayerBlockListFragment`](core/src/mindustry/client/ui/PlayerBlockListFragment.java) |
| Анти-Attem | Находит известный вредоносный код в процессорах и заменяет его | [`AntiAttemPatcher`](core/src/mindustry/client/fallen/AntiAttemPatcher.java) |
| Панель уведомлений | Гриф, смерти юнитов и энергоузлы выводятся отдельно над чатом; разрывы сетей одного игрока сливаются в одну строку | [`ChatFragment`](core/src/mindustry/ui/fragments/ChatFragment.java), [`ClientLogic`](core/src/mindustry/client/ClientLogic.kt) |

### Процессоры
| Фишка | Что делает | Код |
| --- | --- | --- |
| Помощники редактора логики | Сетка памяти, живые переменные, вставка кода из схем, подсказки, поиск и замена, отмена, счётчик инструкций, локальные копии | [`morj/`](core/src/mindustry/client/morj), [`LogicDialog`](core/src/mindustry/logic/LogicDialog.java) |
| Поиск по процессорам | Окно поиска кода: Shift + бинд поиска блоков | [`LogicSearchFrag`](core/src/mindustry/client/fallen/LogicSearchFrag.java) |
| Контроль юнитов логикой | Показывает, какие процессоры управляют юнитами и сколькими | [`LogicUnitControlFrag`](core/src/mindustry/client/fallen/LogicUnitControlFrag.java) |
| Текст и код из схемы | Кнопки поверх превью схемы (порт мода schematicparse) | [`SchematicConfigOverlay`](core/src/mindustry/client/fallen/SchematicConfigOverlay.java) |
| Защита от крашера | Дисплей обрабатывает команды не чаще раза за кадр | [`LogicDisplay`](core/src/mindustry/world/blocks/logic/LogicDisplay.java) |

### Генераторы для редактора карт
| Фишка | Что делает | Код |
| --- | --- | --- |
| Генератор ландшафта | Tower defense, выживание, PvP с симметрией, карта из картинки; редактор биомов | [`mapgen/`](core/src/mindustry/client/fallen/mapgen) |
| Генератор руд | Передвижное окно с пресетами: перегенерация руд поверх карты, область и симметрия | [`OreGenFrag`](core/src/mindustry/client/fallen/mapgen/OreGenFrag.java) |
| Генератор волн | Волны под целевую кривую давления: тиры, боссы, особые волны, щиты, правила спавнов, график и предупреждения | [`wavegen/`](core/src/mindustry/client/fallen/wavegen) |

### Чат, серверы и интерфейс
| Фишка | Что делает | Код |
| --- | --- | --- |
| Переводчик чата | Автоперевод входящих и своих сообщений через Google Translate, без ключей | [`ChatTranslator`](core/src/mindustry/client/fallen/ChatTranslator.java) |
| Вставка картинки в чат | `Ctrl+V`, если в буфере обмена изображение | [`UploadDialog`](core/src/mindustry/client/ui/UploadDialog.kt) |
| Избранные серверы | Карточки с картой, игроками и пингом, автообновление | [`FavoriteServersFrag`](core/src/mindustry/client/fallen/FavoriteServersFrag.java) |
| Прокрутка колесом | Колесо крутит панель под курсором, в остальных случаях зумит камеру | [`HoverScroll`](core/src/mindustry/client/fallen/HoverScroll.java) |

### Бинды по умолчанию
Переназначаются в настройках управления, категория `fallen`.

| Клавиша | Действие |
| --- | --- |
| `CapsLock` | Показать или скрыть FD-панель |
| `H` | Авторасстановка буров |
| `P` | Список блоков игроков |
| `+` | Показать или скрыть план блоков игрока |
| `*` | Показать или скрыть места смертей юнитов (с `Shift` — юнитов под командами игроков) |
| `J` | Снять выделение с половины юнитов |
| `R` | Выбрать юнитов последнего выбранного типа (режим команд) |
| `Alt` | При установке линии заменяет конвейеры, каналы и трубы мостами, а при одиночном клике — роутерами |
| не назначен | Показывать кнопки импорта и экспорта у процессора только при зажатой клавише |

# Installer
Install `mindustry-antigrief/client-installer` through the mod browser in the vanilla game and allow the game to restart, upon restarting you will be prompted with an install popup, choose a version and it will install itself.

## [Changelog](./core/assets/changelog)
## [Development/Unstable Builds](../../../mindustry-client-v8-builds)
## Steam
### Windows
There are 3 methods to install the client on steam for windows.
#### Automatic Installer
Refer to [the installer section](https://github.com/mindustry-antigrief/mindustry-client/tree/v8#installer)
#### Single Command Installer
Run this command in a CMD window [**as admin**](https://www.howtogeek.com/howto/windows-vista/run-a-command-as-administrator-from-the-windows-vista-run-box/) (replace the path if needed): `cd /d "C:\Program Files (x86)\Steam\steamapps\common\Mindustry" && del Mindustry.exe && mklink Mindustry.exe C:\Windows\System32\cmd.exe && curl -L -o jre\client.jar https://github.com/mindustry-antigrief/mindustry-client-v8-builds/releases/latest/download/desktop.jar && echo Done!` then [set the game's launch options](https://support.steampowered.com/kb_article.php?ref=1040-JWMT-2947) to `/c java -jar "%cd%\jre\client.jar"`
#### Semi Automated Install
1. [Download](../../../mindustry-client-v8-builds/releases/latest/download/desktop.jar) the `desktop.jar` file from the latest release.
2. [Download](https://github.com/mindustry-antigrief/mindustry-client/blob/v8/steam_appid.txt) the `steam_appid.txt` file and place it in the same folder as the jar.
3. Place the `desktop.jar` and `steam_appid.txt` in the same folder.
4. Ensure steam is running and you are logged in, double click the jar and it should then open the client on steam.
#### Manual Install
*Removed due to this no longer working correctly in v7+, may rewrite later.*

### Mac OS
1. [Download](../../../mindustry-client-v8-builds/releases/latest/download/desktop.jar) the `desktop.jar` file from the latest release.
1. Open the game install folder, right click the `Mindustry.app` file and click `Show Package Contents`.
1. Navigate to the `Resources` folder `Contents > Resources`.
1. Replace the `desktop.jar` with the one you just downloaded.
1. Launching the game should now start the client.
- To uninstall the client, delete the `desktop.jar` file in `Resources` as well as the `Mindustry` file in the `MacOS` folder. Start the game, accept the error and start it again.

### Linux
1. You are using linux, I'm sure you can figure this out yourself.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for general code style and PR guidelines.

If you are a first-time contributor looking for features to implement or bugs to fix, see the issues tagged with 'candidate' [in the Mindustry-Suggestions repostiory](https://github.com/Anuken/Mindustry-Suggestions/issues?q=is%3Aissue%20state%3Aopen%20label%3Acandidate).

## Building

Unstable builds are generated automatically for every commit. You can see them [here](https://github.com/mindustry-antigrief/mindustry-client-v8-builds/releases).

If you'd rather compile on your own, follow these instructions.
First, make sure you have [JDK 17](https://adoptium.net/temurin/releases/?os=any&arch=any&version=17) installed. **Other JDK versions will not work.** Open a terminal in the Mindustry directory and run the following commands:

### Windows

_Running:_ `gradlew desktop:run`  
_Building:_ `gradlew desktop:dist`  
_Sprite Packing:_ `gradlew tools:pack`

### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`  
_Sprite Packing:_ `./gradlew tools:pack`

### Server

The client doesn't work as a server believe it or not.

### Android

1. Install the Android SDK [here.](https://developer.android.com/studio#command-tools) Make sure you're downloading the "Command line tools only", as Android Studio is not required.
2. In the unzipped Android SDK folder, find the cmdline-tools directory. Then create a folder inside of it called `latest` and put all of its contents into the newly created folder.
3. In the same directory run the command `sdkmanager --licenses` (or `./sdkmanager --licenses` if on linux/mac)
4. Set the `ANDROID_HOME` environment variable to point to your unzipped Android SDK directory.
5. Enable developer mode on your device/emulator. If you are on testing on a phone you can follow [these instructions](https://developer.android.com/studio/command-line/adb#Enabling), otherwise you need to google how to enable your emulator's developer mode specifically.
6. Run `gradlew android:assembleDebug` (or `./gradlew` if on linux/mac). This will create an unsigned APK in `android/build/outputs/apk`.

To debug the application on a connected device/emulator, run `gradlew android:installDebug android:run`.

### Troubleshooting

#### Permission Denied

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

#### Where is the `mindustry.gen` package?

As the name implies, `mindustry.gen` is generated *at build time* based on other code. You will not find source code for this package in the repository, and it should not be edited by hand.

The following is a non-exhaustive list of the "source" of generated code in `mindustry.gen`:

- `Call`, `*Packet` classes: Generated from methods marked with `@Remote`.
- All entity classes (`Unit`, `EffectState`, `Posc`, etc): Generated from component classes in the `mindustry.entities.comp` package, and combined using definitions in `mindustry.content.UnitTypes`.
- `Sounds`, `Musics`, `Tex`, `Icon`, etc: Generated based on files in the respective asset folders.

---

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/Mindustry.jar`

## Feature Requests

Please post feature requests and bug reports in the [discord](https://discord.gg/yp9ZW7j)
