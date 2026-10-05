package chagasos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de archivos FALSO de ChagasOS. Un árbol en memoria que comparten
 * el Explorador, la ChagasShell (terminal) y el Bloc de Chagas.
 * Los MP3 y la foto de perfil apuntan a los archivos reales de resources/.
 */
public final class FakeFS {

    /** Nodo de archivo o carpeta del sistema falso. */
    public static final class Node {
        public String name;
        public final boolean dir;
        public String kind = "txt";   // txt, mp3, png, exe, dll, sys
        public String content = "";   // contenido de archivos de texto
        public File realFile;         // archivo real en disco (mp3/png)
        public Node parent;
        public final List<Node> children = new ArrayList<Node>();

        Node(String name, boolean dir, String kind, String content, File realFile) {
            this.name = name;
            this.dir = dir;
            this.kind = kind;
            this.content = content == null ? "" : content;
            this.realFile = realFile;
        }
    }

    private static Node root;

    private FakeFS() {
    }

    public static synchronized Node root() {
        if (root == null) build();
        return root;
    }

    private static Node dir(Node parent, String name) {
        Node n = new Node(name, true, "folder", null, null);
        n.parent = parent;
        parent.children.add(n);
        return n;
    }

    private static Node file(Node parent, String name, String kind, String content, File real) {
        Node n = new Node(name, false, kind, content, real);
        n.parent = parent;
        parent.children.add(n);
        return n;
    }

    private static void build() {
        root = new Node("C:", true, "folder", null, null);

        Node usuarios = dir(root, "Usuarios");
        Node chagas = dir(usuarios, "Chagas");
        Node musica = dir(chagas, "M\u00FAsica");                 // Música
        Node fotos = dir(chagas, "Fotos");
        Node docs = dir(chagas, "Documentos");

        file(musica, "Chagas (Remix).mp3", "mp3", null, Res.find("Chagas (Remix).mp3"));
        file(musica, "Chagas Dictador.mp3", "mp3", null, Res.find("Chagas Dictador.mp3"));
        file(fotos, "profile.png", "png", null, Res.find("profile.png"));

        file(docs, "leeme.txt", "txt",
                "BIENVENIDO A CHAGASOS 1.0\n"
                + "==========================\n\n"
                + "1. Este sistema operativo es 100% meme, 0% produccion.\n"
                + "2. Si algo falla, es culpa del usuario (asi funciona aqui).\n"
                + "3. El reproductor solo reproduce musica del Chagas. Es ley.\n"
                + "4. No intentes desinstalar el ego del sistema: se reinstala solo.\n"
                + "5. En caso de pantalla azul: respirar, soltar el cafe y presionar ENTER.\n\n"
                + "  -- El Chagas, Dictador Supremo de Sistemas Operativos", null);

        file(docs, "himno.txt", "txt",
                "HIMNO OFICIAL (NO OFICIAL) DE CHAGASOS\n\n"
                + "Chagas, Chagas, gran programador,\n"
                + "tu sistema operativo es el mejor,\n"
                + "nunca se cuelga (bueno, a veces si),\n"
                + "pero cuando reinicia, lo hace con estilo.\n\n"
                + "\u266A De los archivos MP3 la patria brota \u266A\n"
                + "\u266A y de la RAM sale todo el coraje \u266A", null);

        file(docs, "plan_de_gobierno.txt", "txt",
                "PLAN DE GOBIERNO DE CHAGASOS 1.0\n\n"
                + "Paso 1: llegar al poder.      [X]\n"
                + "Paso 2: ???.                  [X]\n"
                + "Paso 3: esto.                 [X]\n"
                + "Paso 4: quedarse. Para siempre.\n"
                + "Paso 5: himno obligatorio en todos los reproductores.", null);

        file(docs, "diario_secreto.txt", "txt",
                "DIARIO DEL CHAGAS (NO ABRIR)\n\n"
                + "\u00BFPor que lo abriste?...\n\n"
                + "Dia 1: invente un sistema operativo. Nadie lo pidio.\n"
                + "Dia 2: puse mi cara en el fondo de pantalla.\n"
                + "Dia 3: puse mi cara en el reproductor de musica.\n"
                + "Dia 4: el sistema crashea. Es parte del plan.\n"
                + "Dia 5: sigo escribiendo el diario en un archivo publico.", null);

        Node windows = dir(root, "Windows");
        Node sys = dir(windows, "ChagasSystem32");
        file(sys, "chagas.exe", "exe", "MZ...90...binario del Chagas.\nNo ejecutar con el pelo suelto.", null);
        file(sys, "dictador.dll", "dll", "MZ...libreria oficial del regimen.\nCarga sola. Siempre.", null);
        file(sys, "ego.dll", "dll", "MZ...6.2 GB de ego comprimidos con lossless.\nNo hay lossless para tanto ego.", null);
        file(sys, "chagascore.sys", "sys", "kernel del sistema. Escrito en una servilleta.", null);

        dir(root, "Papelera de reciclaje");
    }

    /* ------------------------- consultas ------------------------- */

    /** Ruta tipo "C:/Usuarios/Chagas" */
    public static String pathOf(Node n) {
        StringBuilder sb = new StringBuilder(n.name);
        Node p = n.parent;
        while (p != null) {
            sb.insert(0, p.name + "/");
            p = p.parent;
        }
        return sb.toString();
    }

    /** Hijo por nombre (ignora may\u00fasculas). */
    public static Node child(Node parent, String name) {
        for (Node c : parent.children) {
            if (c.name.equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    /** Lista con todos los MP3 del sistema (los adjuntos). */
    public static List<Node> songs() {
        List<Node> out = new ArrayList<Node>();
        collectByKind(root(), "mp3", out);
        return out;
    }

    private static void collectByKind(Node n, String kind, List<Node> out) {
        if (!n.dir && kind.equals(n.kind)) out.add(n);
        for (Node c : n.children) collectByKind(c, kind, out);
    }

    /** Busca un archivo por nombre en todo el árbol. */
    public static Node textFile(String name) {
        return findByName(root(), name);
    }

    private static Node findByName(Node n, String name) {
        if (!n.dir && n.name.equalsIgnoreCase(name)) return n;
        for (Node c : n.children) {
            Node r = findByName(c, name);
            if (r != null) return r;
        }
        return null;
    }

    /** Nombres de todos los .txt (para el Bloc: Abrir...). */
    public static List<String> txtNames() {
        List<String> out = new ArrayList<String>();
        collectTxt(root(), out);
        return out;
    }

    private static void collectTxt(Node n, List<String> out) {
        if (!n.dir && "txt".equals(n.kind)) out.add(n.name);
        for (Node c : n.children) collectTxt(c, out);
    }

    /** Guarda (crea o actualiza) un .txt en Documentos. */
    public static synchronized Node saveTxt(String name, String content) {
        if (!name.toLowerCase().endsWith(".txt")) name = name + ".txt";
        Node existing = textFile(name);
        if (existing != null) {
            existing.content = content;
            return existing;
        }
        Node docs = findDir(root(), "Documentos");
        if (docs == null) docs = dir(root, "Documentos");
        return file(docs, name, "txt", content, null);
    }

    private static Node findDir(Node n, String name) {
        if (n.dir && n.name.equalsIgnoreCase(name)) return n;
        for (Node c : n.children) {
            Node r = findDir(c, name);
            if (r != null) return r;
        }
        return null;
    }

    /** Descripción de tamaño para la vista previa del explorador. */
    public static String sizeLabel(Node n) {
        if (n.dir) return n.children.size() + " elementos";
        if (n.realFile != null && n.realFile.isFile()) {
            long b = n.realFile.length();
            if (b > 1024 * 1024) return String.format("%.1f MB", b / 1048576.0);
            if (b > 1024) return String.format("%.1f KB", b / 1024.0);
            return b + " B";
        }
        if ("mp3".equals(n.kind)) return "(no encontrado)";
        return n.content.length() + " caracteres";
    }
}
