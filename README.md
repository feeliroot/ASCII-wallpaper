# ASCII-обои (Android live wallpaper)

Сборка:
1. Открой папку в Android Studio (Koala+ / JDK 17) -> дождись Gradle sync -> Run на телефон.
   Или из консоли: `gradle wrapper && ./gradlew installDebug` (нужны Android SDK и adb).
2. Запусти приложение "ASCII-обои", выбери форму/цвет/плотность/скорость/FPS,
   нажми "Установить как обои".

Настройки применяются на лету. Параметры хранятся в SharedPreferences "ascii".
Фигуры лежат в Shapes.kt, рендер в AsciiWallpaperService.kt.
