package chagasos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Bloc de Chagas: editor de texto con JScrollPane, JMenuBar propia
 * (JMenu "Archivo" y JMenu "Chagas"), abrir/guardar en el FS falso.
 */
public class NotepadApp extends JInternalFrame {

    String fileName;
    final JTextArea area = new JTextArea();
    final JLabel status = new JLabel(" ");

    public NotepadApp(String name) {
        super("Bloc de Chagas", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("notepad").icon(16));
        setSize(540, 420);
        setJMenuBar(buildMenu());
        setContentPane(buildUI());
        if (name != null) loadFile(name);
        else updateStatus();
    }

    JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 4));
        root.setBackground(ChagasOS.BG_PANEL);

        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(ChagasOS.BG_DEEP);
        area.setForeground(ChagasOS.TEXT_MAIN);
        area.setCaretColor(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(area);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        root.add(scroll, BorderLayout.CENTER);

        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        status.setForeground(ChagasOS.TEXT_DIM);
        status.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        root.add(status, BorderLayout.SOUTH);

        area.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateStatus();
            }

            public void removeUpdate(DocumentEvent e) {
                updateStatus();
            }

            public void changedUpdate(DocumentEvent e) {
                updateStatus();
            }
        });
        return root;
    }

    JMenuBar buildMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu mArchivo = new JMenu("Archivo");
        JMenuItem nuevo = new JMenuItem("Nuevo");
        nuevo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileName = null;
                area.setText("");
                updateTitle();
            }
        });
        JMenuItem abrir = new JMenuItem("Abrir...");
        abrir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abrirDialog();
            }
        });
        JMenuItem guardar = new JMenuItem("Guardar");
        guardar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                guardar();
            }
        });
        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        mArchivo.add(nuevo);
        mArchivo.add(abrir);
        mArchivo.add(guardar);
        mArchivo.addSeparator();
        mArchivo.add(salir);

        JMenu mChagas = new JMenu("Chagas");
        JMenuItem bendicion = new JMenuItem("Insertar bendici\u00F3n");
        bendicion.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                insert(ChagasOS.randomBendicion());
            }
        });
        JMenuItem himno = new JMenuItem("Insertar himno del sistema");
        himno.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FakeFS.Node n = FakeFS.textFile("himno.txt");
                insert(n != null ? n.content : "\u266A himno no encontrado \u266A");
            }
        });
        JMenuItem cita = new JMenuItem("Insertar cita del Chagas");
        cita.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                insert("\u201C" + ChagasOS.randomChagasQuote() + "\u201D");
            }
        });
        mChagas.add(bendicion);
        mChagas.add(himno);
        mChagas.add(cita);

        mb.add(mArchivo);
        mb.add(mChagas);
        return mb;
    }

    /* ------------------------- acciones ------------------------- */

    void insert(String txt) {
        area.insert(txt, area.getCaretPosition());
        area.requestFocusInWindow();
    }

    void abrirDialog() {
        java.util.List<String> names = FakeFS.txtNames();
        if (names.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay archivos de texto en el sistema.");
            return;
        }
        Object sel = JOptionPane.showInputDialog(this, "\u00BFCu\u00E1l archivo abrimos?",
                "Bloc de Chagas \u2014 Abrir", JOptionPane.PLAIN_MESSAGE, Res.roundAvatar(48),
                names.toArray(), names.get(0));
        if (sel != null) loadFile(sel.toString());
    }

    void loadFile(String name) {
        FakeFS.Node n = FakeFS.textFile(name);
        if (n == null) {
            JOptionPane.showMessageDialog(this, "No encuentro el archivo: " + name,
                    "Bloc de Chagas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        fileName = n.name;
        area.setText(n.content);
        area.setCaretPosition(0);
        updateTitle();
    }

    void guardar() {
        String name = fileName;
        if (name == null) {
            JTextField campo = new JTextField("nueva_ley_del_chagas.txt");
            int opt = JOptionPane.showConfirmDialog(this, campo,
                    "Guardar como (en C:/Usuarios/Chagas/Documentos):",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opt != JOptionPane.OK_OPTION) return;
            name = campo.getText().trim();
            if (name.isEmpty()) return;
        }
        FakeFS.saveTxt(name, area.getText());
        fileName = FakeFS.textFile(name) != null ? FakeFS.textFile(name).name : name;
        updateTitle();
        status.setText("Guardado como " + fileName + " \u2014 el Chagas respalda tus palabras.");
    }

    void updateTitle() {
        setTitle("Bloc de Chagas" + (fileName == null ? "" : " \u2014 " + fileName));
        updateStatus();
    }

    void updateStatus() {
        String txt = area.getText();
        int lines = txt.isEmpty() ? 0 : txt.split("\n").length;
        int words = txt.trim().isEmpty() ? 0 : txt.trim().split("\\s+").length;
        status.setText("L\u00EDneas: " + lines + "   \u2022   Palabras: " + words + "   \u2022   "
                + txt.length() + " caracteres");
    }
}

