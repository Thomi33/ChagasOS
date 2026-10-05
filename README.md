# ChagasOS 1.0 «Dictador» 🖥️🎵

**Un sistema operativo 100% meme, escrito en Java Swing.** No es serio. Es mejor.

Este proyecto emula un sistema operativo completo dentro de una aplicación Java:
pantalla de arranque (BIOS), inicio de sesión con foto de perfil, escritorio con
íconos, barra de tareas con reloj, menú Inicio con buscador, múltiples apps en
ventanas internas y hasta su propia Pantalla Azul de la Muerte.

---

## Requisitos

- **Java 8 o superior** (JDK para compilar).
- Los archivos del proyecto ya incluidos:
  - `resources/profile.png` → la foto de perfil del Chagas.
  - `resources/Chagas (Remix).mp3` y `resources/Chagas Dictador.mp3` → la música oficial.
  - `lib/jlayer.jar` → librería JLayer (JavaZoom) para decodificar MP3 100% en Java.

## Cómo ejecutarlo

### Linux / macOS
```bash
./run.sh
```

### Windows
```bat
run.bat
```

### Manual
```bash
javac -encoding UTF-8 -cp lib/jlayer.jar -d out $(find src -name "*.java")
java -cp "out:lib/jlayer.jar" chagasos.ChagasOS
```
(En Windows cambia `:` por `;` en el classpath y usa `dir /s /b src\*.java`.)

---

## Componentes de Swing usados (requisitos del proyecto)

| Componente    | Dónde se usa                                                          |
|---------------|-----------------------------------------------------------------------|
| **JLabel**    | Reloj de la barra de tareas, estado del reproductor, textos de todas las apps |
| **JTextField**| Buscador del menú Inicio, entrada de la terminal, display de la calculadora, barra de ruta del explorador, campo de usuario en Configuración |
| **JButton**   | Botones del reproductor, calculadora, apps del menú Inicio, botones de apagado, etc. |
| **JScrollPane**| Salida de la terminal, lista de canciones, árbol del explorador, bloc de notas, tabla de procesos |
| **JMenuBar**  | Barra superior del sistema (Sistema / Aplicaciones / Chagas / Ayuda)  |
| **JMenu**     | Cada menú de la barra; el Bloc de Chagas tiene su propia JMenuBar con "Archivo" y "Chagas" |
| **JDesktopPane**| El escritorio donde viven todas las ventanas (JInternalFrame)        |

## Aplicaciones incluidas

1. **ChagasPlayer** 🎵 — Reproductor MP3 REAL (decodifica con JLayer y suena de
   verdad). Play/pausa/stop/anterior/siguiente, barra de progreso arrastrable
   (seek), volumen, visualizador animado, duración real de cada canción y
   reproducción automática en bucle. Solo contiene la música del Chagas (es ley).
2. **Explorador de Chagas** 📁 — Sistema de archivos falso con árbol (JTree):
   Música, Fotos, Documentos y los binarios del régimen. Doble clic abre los
   MP3 en el reproductor, los TXT en el bloc y el PNG en el visor de fotos.
3. **ChagasShell** 🖥️ — Terminal con más de 25 comandos: `help`, `ls`, `cd`,
   `cat`, `neofetch`, `whoami`, `sudo`, `crash`, `apagar`... Escribe `help`.
4. **Bloc de Chagas** 📝 — Editor de texto con JMenuBar propia (Archivo /
   Chagas), abrir/guardar en el sistema de archivos falso, e insertar bendiciones
   e himnos oficiales.
5. **Admin. de Tareas** 📊 — Tabla de procesos "del sistema" (ego.dll,
   conta_billetes.exe...) con CPU y RAM animadas. No te deja finalizar nada:
   solo el Chagas puede.
6. **Calculadora Chagas** ➗ — Calculadora funcional; dividir por cero produce
   el error oficial `CHAGAS ERROR`.
7. **Configuración** ⚙️ — Cambiar nombre de usuario, fondo de pantalla
   (gradiente o la foto del Chagas) y activar el **Modo Dictador** (todo en rojo).
8. **Acerca de ChagasOS** ℹ️ — Ficha técnica honesta del sistema.

## Extras del "sistema operativo"

- **Boot splash** con barra de progreso y mensajes ("Montando el ego (6.2 GB)...").
- **Pantalla de login** con la foto de perfil (acepta cualquier contraseña).
- **Menú Inicio** con foto de perfil, buscador de apps (JTextField con filtro
  en vivo) y botones de sesión (Apagar / Reiniciar / Cerrar sesión).
- **Barra de tareas** con botones de ventanas abiertas, canción sonando y reloj.
- **BSOD** 💀 — Pantalla azul con `CHAGAS_0xD1CT4D0R`; se dispara desde
  Sistema → Crashear, con el comando `crash` o desde el Admin de Tareas.
  ENTER (o clic) reinicia el sistema completo (boot + login).
- **Modo Dictador** — acento rojo en todo el sistema.
- **Sistema anti-crash** 🛡️ — Cualquier excepción no capturada queda registrada
  en `chagas_error.log` y el sistema sigue vivo (nada de congelarse al
  maximizar/redimensionar ventanas). El fondo de foto usa una versión
  pre-escalada para que agrandar la ventana sea fluido.
- Apagado/reinicio/cierre de sesión reales con sus pantallas correspondientes.

## Estructura del código

```
src/chagasos/
├── ChagasOS.java      ← Núcleo: escritorio, taskbar, menú Inicio, JMenuBar, BSOD
├── LoginScreen.java   ← Boot splash + pantalla de login + apagado
├── MusicPlayer.java   ← Reproductor MP3 (motor JLayer + JavaSound)
├── TerminalApp.java   ← ChagasShell (la terminal)
├── FileManagerApp.java← Explorador de archivos (JTree + vista previa)
├── NotepadApp.java    ← Bloc de Chagas (JMenuBar propia)
├── TaskManagerApp.java← Administrador de tareas
├── CalculatorApp.java ← Calculadora
├── SettingsApp.java   ← Configuración del sistema
├── AboutApp.java      ← Acerca de
├── FakeFS.java        ← Sistema de archivos falso compartido
└── Res.java           ← Recursos (foto de perfil), íconos Java2D y cursor de la cara

cursors/               ← Pack de cursores de la cara (PNG + .cur de Windows)
docs/                  ← Capturas del sistema funcionando
```

## Créditos

- **Chagas Corporation** — Todos los derechos patrullados.
- Licencia: *Chagas Public License* — puedes copiarlo todo, menos el ego.
- El Chagas no se hace responsable de risas, dolores abdominales o notas de
  sistemas operativos superiores a lo esperado.
