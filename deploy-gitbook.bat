@echo off
echo 📖 Deploying JobsAdventure Wiki to GitBook...
echo.

:: Vérifier si GitBook CLI est installé
where gitbook >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ GitBook CLI n'est pas installé!
    echo.
    echo Pour l'installer, exécutez:
    echo npm install -g gitbook-cli
    echo.
    pause
    exit /b 1
)

:: Se déplacer dans le dossier docs
cd /d "%~dp0docs"

echo 🔍 Installation des plugins GitBook...
gitbook install

echo 🏗️ Construction du site...
gitbook build

echo ✅ Site construit dans le dossier _book/
echo.
echo 🌐 Pour servir localement, exécutez:
echo gitbook serve
echo.
echo 📤 Pour déployer, uploadez le contenu de _book/ sur votre serveur
echo.
pause