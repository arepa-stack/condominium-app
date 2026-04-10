# Guía de Limpieza de Procesos Gradle (Windows)

Si encuentras errores de tipo `FileSystemException` o "El proceso no tiene acceso al archivo" al compilar en Windows, sigue estos comandos en tu terminal (PowerShell recomendado):

## 1. Identificar procesos bloqueantes
Para ver qué procesos de Java (Gradle) están activos:
```powershell
tasklist /FI "IMAGENAME eq java.exe" /V
```

## 2. Cerrar procesos de fuerza bruta
Para cerrar todos los procesos de Java relacionados con Gradle/Android Studio:
```powershell
taskkill /F /IM java.exe
```
*Nota: Si tienes Android Studio abierto, esto lo cerrará o afectará su funcionamiento.*

## 3. Limpiar Daemons de forma elegante
Siempre es bueno intentar esto primero:
```powershell
./gradlew --stop
```

## 4. Borrado manual de carpetas de build
Si `clean` falla, borra los directorios físicamente:
```powershell
Remove-Item -Recurse -Force shared/build
Remove-Item -Recurse -Force androidApp/build
```

## 5. Script de "Super Clean" (PowerShell)
Puedes copiar y pegar esta línea para hacer todo el proceso de una sola vez:
```powershell
./gradlew --stop; taskkill /F /IM java.exe; Remove-Item -Recurse -Force shared/build, androidApp/build; ./gradlew clean
```

---

## Comandos en Bash (Git Bash / Linux / macOS)

Si prefieres usar la terminal de Git Bash (MINGW64) o estás en otro sistema operativo:

### 1. Identificar procesos
```bash
ps aux | grep java
# O usando pgrep para ver solo los PIDs
pgrep -af java
```

### 2. Cerrar procesos
```bash
pkill -9 java
```

### 3. Borrado manual de carpetas
```bash
rm -rf shared/build androidApp/build
```

### 4. Script de "Super Clean" (Bash)
```bash
./gradlew --stop && pkill -9 java && rm -rf shared/build androidApp/build && ./gradlew clean
```
