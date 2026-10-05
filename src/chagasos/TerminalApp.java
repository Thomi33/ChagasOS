package chagasos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * ChagasShell: la terminal oficial de ChagasOS.
 * JTextField para escribir + JTextArea con JScrollPane para la salida.
 */
public class TerminalApp extends JInternalFrame {

    final JTextArea out = new JTextArea();
    final JTextField input = new JTextField();
    FakeFS.Node cwd;

    public TerminalApp() {
        super("ChagasShell", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("terminal").icon(16));
        setSize(600, 400);
        cwd = home();
        buildUI();
        append("ChagasShell [Versi\u00F3n 1.0.420 \u00ABDictador\u00BB]");
        append("(c) Chagas Corporation. Todos los derechos patrullados.");
        append("Escribe 'help' para ver los comandos. Escribe 'neofetch' para presumir.");
        append("");
        updatePromptTitle();
    }

    FakeFS.Node home() {
        FakeFS.Node n = FakeFS.child(FakeFS.root(), "Usuarios");
        if (n != null) n = FakeFS.child(n, "Chagas");
        return n != null ? n : FakeFS.root();
    }

    void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(12, 14, 12));

        out.setEditable(false);
        out.setBackground(new Color(12, 14, 12));
        out.setForeground(new Color(190, 250, 200));
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        out.setBorder(new EmptyBorder(8, 10, 8, 10));
        out.setCaretColor(new Color(190, 250, 200));
        JScrollPane scroll = new JScrollPane(out);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        root.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setBackground(new Color(12, 14, 12));
        south.setBorder(new EmptyBorder(6, 10, 6, 10));
        final JLabel prompt = new JLabel(promptText());
        prompt.setForeground(new Color(120, 255, 140));
        prompt.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        south.add(prompt, BorderLayout.WEST);
        input.setBackground(new Color(20, 26, 20));
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 90, 60)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = input.getText();
                input.setText("");
                process(cmd);
                prompt.setText(promptText());
                updatePromptTitle();
            }
        });
        south.add(input, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
    }

    String promptText() {
        return ChagasOS.userName.toLowerCase() + "@" + ChagasOS.OS_NAME + ":"
                + FakeFS.pathOf(cwd) + "$ ";
    }

    void updatePromptTitle() {
        setTitle("ChagasShell \u2014 " + FakeFS.pathOf(cwd));
    }

    void append(String s) {
        out.append(s + "\n");
        out.setCaretPosition(out.getDocument().getLength());
    }

    /* ==================== PROCESO DE COMANDOS ==================== */

    void process(String raw) {
        append(promptText() + raw);
        String line = raw.trim();
        if (line.isEmpty()) return;
        String[] t = line.split("\\s+", 2);
        String cmd = t[0].toLowerCase(Locale.ROOT);
        String arg = t.length > 1 ? t[1].trim() : "";

        if ("help".equals(cmd) || "ayuda".equals(cmd)) {
            help();
        } else if ("ls".equals(cmd) || "dir".equals(cmd)) {
            ls(arg);
        } else if ("cd".equals(cmd)) {
            cd(arg);
        } else if ("pwd".equals(cmd)) {
            append(FakeFS.pathOf(cwd));
        } else if ("cat".equals(cmd) || "type".equals(cmd)) {
            cat(arg);
        } else if ("echo".equals(cmd)) {
            append(arg);
        } else if ("clear".equals(cmd) || "cls".equals(cmd)) {
            out.setText("");
        } else if ("whoami".equals(cmd)) {
            append(ChagasOS.userName + " (dictador con coraz\u00F3n de oro, ROOT total)");
        } else if ("date".equals(cmd) || "fecha".equals(cmd)) {
            append(new SimpleDateFormat("EEEE d 'de' MMMM yyyy, HH:mm:ss", new Locale("es")).format(new Date()));
        } else if ("ver".equals(cmd)) {
            append(ChagasOS.OS_NAME + " " + ChagasOS.OS_VERSION + " \u2014 kernel chagas-core 6.9.1");
            append("Compilado con desconfianza y cafe\u00EDna.");
        } else if ("neofetch".equals(cmd)) {
            neofetch();
        } else if ("chagas".equals(cmd)) {
            append("\u201C" + ChagasOS.randomChagasQuote() + "\u201D");
        } else if ("bendecir".equals(cmd) || "bendicion".equals(cmd)) {
            append(ChagasOS.randomBendicion());
        } else if ("musica".equals(cmd) || "music".equals(cmd) || "player".equals(cmd)) {
            append("Abriendo Chagastify...");
            ChagasOS.instance.openApp("player");
        } else if ("abrir".equals(cmd) || "start".equals(cmd)) {
            abrir(arg);
        } else if ("dictador".equals(cmd)) {
            ChagasOS.setDictador(!ChagasOS.modoDictador);
            append("Modo Dictador: " + (ChagasOS.modoDictador ? "ACTIVADO (todo en rojo)" : "desactivado (volvi\u00F3 la calma)"));
        } else if ("sudo".equals(cmd)) {
            append("El Chagas no necesita sudo. El Chagas ES root.");
        } else if ("rm".equals(cmd) || "del".equals(cmd)) {
            append("ELIMINAR ARCHIVOS? JAM\u00C1S. Aqu\u00ED todo queda como prueba.");
        } else if ("crash".equals(cmd) || "cuelgate".equals(cmd)) {
            append("Iniciando ca\u00EDda del sistema...");
            ChagasOS.instance.blueScreen();
        } else if ("apagar".equals(cmd) || "shutdown".equals(cmd)) {
            ChagasOS.instance.shutdown();
        } else if ("reiniciar".equals(cmd) || "reboot".equals(cmd)) {
            ChagasOS.instance.reboot();
        } else if ("salir".equals(cmd) || "exit".equals(cmd)) {
            dispose();
        } else if ("ping".equals(cmd)) {
            append("pong (siempre contesta, no tiene vida social)");
        } else if ("ip".equals(cmd) || "ipconfig".equals(cmd)) {
            append("IP del Chagas: puto 67 (reservada por la IANA del Chagas)");
            append("M\u00E1scara: no la necesitamos, no escondemos nada.");
        } else if ("calc".equals(cmd) || "calculadora".equals(cmd)) {
            ChagasOS.instance.openApp("calc");
            append("Abriendo la Calculadora Chagas...");
        } else if ("tareas".equals(cmd) || "taskmgr".equals(cmd)) {
            ChagasOS.instance.openApp("tasks");
            append("Abriendo el Administrador de Tareas...");
        } else if ("himno".equals(cmd)) {
            cat("himno.txt");
        } else {
            append("chagas-sh: comando no encontrado: " + cmd + " \u2014 escribe 'help'");
        }
    }

    void help() {
        append("COMANDOS DEL SISTEMA (todos muy serios):");
        append("  help            \u2014 esta lista, obviamente");
        append("  ls / dir        \u2014 listar archivos");
        append("  cd <carpeta>    \u2014 moverte (cd .. sube, cd / a la ra\u00EDz)");
        append("  pwd             \u2014 d\u00F3nde estoy");
        append("  cat <archivo>   \u2014 leer un archivo (prueba: cat leeme.txt)");
        append("  echo <texto>    \u2014 repetir como loro");
        append("  clear           \u2014 limpiar la pantalla");
        append("  whoami          \u2014 qui\u00E9n eres t\u00FA");
        append("  date            \u2014 fecha y hora");
        append("  ver             \u2014 versi\u00F3n del sistema");
        append("  neofetch        \u2014 presumir el sistema");
        append("  chagas          \u2014 cita oficial del Chagas");
        append("  bendecir        \u2014 bendici\u00F3n del Chagas para ti");
        append("  musica          \u2014 abrir el reproductor");
        append("  abrir <app>     \u2014 abrir una app (player, explorer, bloc, calc...)");
        append("  dictador        \u2014 activar/desactivar el Modo Dictador (rojo)");
        append("  sudo <lo que sea>\u2014 no hace falta, t\u00FA eres root");
        append("  ip / ping       \u2014 red del Chagas");
        append("  crash           \u2014 pantalla azul del sistema ( BSOD )");
        append("  apagar / reiniciar / salir");
    }


    void ls(String arg) {
        FakeFS.Node dir = cwd;
        if (!arg.isEmpty()) {
            FakeFS.Node n = FakeFS.child(cwd, arg);
            if (n == null || !n.dir) {
                append("ls: '" + arg + "' no existe o no es una carpeta");
                return;
            }
            dir = n;
        }
        if (dir.children.isEmpty()) {
            append("(carpeta vac\u00EDa, como las promesas del Chagas)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FakeFS.Node c : dir.children) {
            sb.append(c.dir ? "[" + c.name + "]" : c.name).append("   ");
        }
        append(sb.toString().trim());
    }

    void cd(String arg) {
        if (arg.isEmpty() || arg.equals("~")) {
            cwd = home();
        } else if (arg.equals("/")) {
            cwd = FakeFS.root();
        } else if (arg.equals("..")) {
            if (cwd.parent != null) cwd = cwd.parent;
        } else {
            FakeFS.Node n = FakeFS.child(cwd, arg);
            if (n != null && n.dir) {
                cwd = n;
            } else {
                append("cd: '" + arg + "' no es una carpeta del sistema");
            }
        }
    }

    void cat(String arg) {
        if (arg.isEmpty()) {
            append("cat: \u00BFcu\u00E1l archivo? (ejemplo: cat leeme.txt)");
            return;
        }
        FakeFS.Node n = FakeFS.child(cwd, arg);
        if (n == null) n = FakeFS.textFile(arg);
        if (n == null) {
            append("cat: '" + arg + "' no existe (o es top secret)");
            return;
        }
        if (n.dir) {
            append("cat: '" + arg + "' es una carpeta, no un diario \u00EDntimo");
            return;
        }
        if (n.content.isEmpty()) {
            append("(archivo binario del Chagas \u2014 no se muestra por seguridad nacional)");
        } else {
            append("----- " + n.name + " -----");
            for (String ln : n.content.split("\n")) append(ln);
            append("---------------------");
        }
    }

    void abrir(String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        if (a.contains("player") || a.contains("musica") || a.contains("music")) {
            ChagasOS.instance.openApp("player");
            append("Abriendo Chagastify...");
        } else if (a.contains("explor") || a.contains("archivos")) {
            ChagasOS.instance.openApp("explorer");
            append("Abriendo el Explorador...");
        } else if (a.contains("bloc") || a.contains("notepad") || a.contains("notas")) {
            ChagasOS.instance.openApp("notepad");
            append("Abriendo el Bloc de Chagas...");
        } else if (a.contains("tarea")) {
            ChagasOS.instance.openApp("tasks");
            append("Abriendo el Admin. de Tareas...");
        } else if (a.contains("calc")) {
            ChagasOS.instance.openApp("calc");
            append("Abriendo la Calculadora...");
        } else if (a.contains("conf") || a.contains("setting")) {
            ChagasOS.instance.openApp("settings");
            append("Abriendo Configuraci\u00F3n...");
        } else if (a.contains("acerca") || a.contains("about")) {
            ChagasOS.instance.openApp("about");
            append("Abriendo Acerca de ChagasOS...");
        } else {
            append("abrir: no s\u00E9 qu\u00E9 es '" + arg + "'. Prueba: player, explorer, bloc, tareas, calc, config, acerca");
        }
    }

    void neofetch() {
        append("        _____     ");
        append("       | ^   ^ |    " + ChagasOS.OS_NAME + " " + ChagasOS.OS_VERSION);
        append("       |   o   |    ------------------------------");
        append("        \\_____/     Kernel:   chagas-core 6.9.1");
        append("                   Shell:    chagas-sh 1.0.420");
        append("                   Usuario:  " + ChagasOS.userName + " (ROOT)");
        append("                   RAM:      2 GB reales (8 reportados)");
        append("                   CPU:      ChagIntel \u00ABFandango\u00BB @ 6.9 GHz");
        append("                   M\u00FAsica:   solo del Chagas (es ley)");
        append("                   Uptime:   desde que aguantas este meme");
        append("                   Bugs:     0 confirmados (todos los niega)");
    }
}

