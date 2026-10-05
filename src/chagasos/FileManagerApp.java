package chagasos;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Explorador de Chagas: navega el sistema de archivos falso con un JTree
 * (dentro de un JScrollPane), con barra de ruta y panel de vista previa.
 * Doble clic: abre MP3 en el Chagastify, TXT en el Bloc, PNG en el visor.
 */
public class FileManagerApp extends JInternalFrame {

    JTree tree;
    JTextField pathField = new JTextField();
    JLabel pvIcon = new JLabel();
    JLabel pvName = new JLabel("(selecciona algo)");
    JLabel pvKind = new JLabel(" ");
    JLabel pvSize = new JLabel(" ");
    JButton openBtn = new JButton("Abrir");

    public FileManagerApp() {
        super("Explorador de Chagas", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("explorer").icon(16));
        setSize(680, 420);
        setContentPane(buildUI());
        restorePreview();
    }

    JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // barra de ruta (JTextField de solo lectura)
        pathField.setEditable(false);
        pathField.setBackground(ChagasOS.BG_DEEP);
        pathField.setForeground(ChagasOS.TEXT_MAIN);
        pathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ChagasOS.BG_CONTROL),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        pathField.setText("C:/");
        root.add(pathField, BorderLayout.NORTH);

        // \u00E1rbol del sistema de archivos
        DefaultMutableTreeNode top = node(FakeFS.root());
        tree = new JTree(new DefaultTreeModel(top));
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        tree.setBackground(ChagasOS.BG_DEEP);
        tree.setForeground(ChagasOS.TEXT_MAIN);
        tree.setCellRenderer(new FSRenderer());
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selectionChanged();
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tree.getClosestRowForLocation(e.getX(), e.getY());
                    TreePath p = tree.getPathForRow(row);
                    if (p != null) {
                        Object uo = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
                        if (uo instanceof FakeFS.Node) openNode((FakeFS.Node) uo);
                    }
                }
            }
        });
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(260, 320));
        treeScroll.setBorder(BorderFactory.createLineBorder(ChagasOS.BG_CONTROL));

        // panel de vista previa
        JPanel preview = new JPanel();
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        preview.setBackground(ChagasOS.BG_DEEP);
        preview.setBorder(BorderFactory.createLineBorder(ChagasOS.BG_CONTROL));
        preview.add(Box.createVerticalStrut(18));
        pvIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        preview.add(pvIcon);
        preview.add(Box.createVerticalStrut(10));
        pvName.setAlignmentX(Component.CENTER_ALIGNMENT);
        pvName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        pvName.setForeground(ChagasOS.TEXT_MAIN);
        preview.add(pvName);
        pvKind.setAlignmentX(Component.CENTER_ALIGNMENT);
        pvKind.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pvKind.setForeground(ChagasOS.TEXT_DIM);
        preview.add(pvKind);
        pvSize.setAlignmentX(Component.CENTER_ALIGNMENT);
        pvSize.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pvSize.setForeground(ChagasOS.TEXT_DIM);
        preview.add(pvSize);
        preview.add(Box.createVerticalStrut(14));
        openBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        openBtn.setBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_CONTROL, 0.35f));
        openBtn.setForeground(Color.WHITE);
        openBtn.setFocusPainted(false);
        openBtn.setEnabled(false);
        openBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                FakeFS.Node n = selected();
                if (n != null) openNode(n);
            }
        });
        preview.add(openBtn);
        preview.add(Box.createVerticalGlue());

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(treeScroll, BorderLayout.CENTER);
        center.add(preview, BorderLayout.EAST);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    DefaultMutableTreeNode node(FakeFS.Node n) {
        DefaultMutableTreeNode tn = new DefaultMutableTreeNode(n);
        for (FakeFS.Node c : n.children) {
            if (c.dir) tn.add(node(c));
            else tn.add(new DefaultMutableTreeNode(c));
        }
        return tn;
    }

    FakeFS.Node selected() {
        DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (tn == null || !(tn.getUserObject() instanceof FakeFS.Node)) return null;
        return (FakeFS.Node) tn.getUserObject();
    }

    void selectionChanged() {
        FakeFS.Node n = selected();
        if (n == null) return;
        pathField.setText(FakeFS.pathOf(n));
        pvIcon.setIcon(iconFor(n, 64));
        pvName.setText(n.name);
        pvKind.setText(kindLabel(n));
        pvSize.setText(FakeFS.sizeLabel(n));
        openBtn.setEnabled(!n.dir || !n.children.isEmpty());
        openBtn.setText(n.dir ? "Entrar" : "Abrir");
    }

    void restorePreview() {
        pvIcon.setIcon(Res.appIcon("folder", new Color(25, 118, 210), 64));
    }

    ImageIcon iconFor(FakeFS.Node n, int size) {
        if (n.dir) return Res.appIcon("folder", new Color(25, 118, 210), size);
        if ("mp3".equals(n.kind)) return Res.appIcon("music", ChagasOS.ACCENT_NORMAL, size);
        if ("png".equals(n.kind)) return Res.appIcon("png", new Color(69, 90, 100), size);
        if ("txt".equals(n.kind)) return Res.appIcon("txt", new Color(230, 126, 34), size);
        return Res.appIcon(n.kind, new Color(100, 100, 110), size);
    }

    String kindLabel(FakeFS.Node n) {
        if (n.dir) return "Carpeta";
        if ("mp3".equals(n.kind)) return "Audio MP3 (m\u00FAsica del Chagas)";
        if ("png".equals(n.kind)) return "Imagen PNG";
        if ("txt".equals(n.kind)) return "Documento de texto";
        if ("exe".equals(n.kind)) return "Ejecutable del Chagas";
        if ("dll".equals(n.kind)) return "Librer\u00EDa del r\u00E9gimen";
        if ("sys".equals(n.kind)) return "Archivo del kernel";
        return "Archivo";
    }

    void openNode(FakeFS.Node n) {
        if (n.dir) return;
        if ("mp3".equals(n.kind)) {
            ChagasOS.instance.openPlayerPlay(n.name);
        } else if ("txt".equals(n.kind)) {
            ChagasOS.instance.openNotepad(n.name);
        } else if ("png".equals(n.kind)) {
            File f = (n.realFile != null) ? n.realFile : Res.find(n.name);
            if (f != null) ChagasOS.instance.openImageViewer(n.name, f);
            else ChagasOS.instance.notifyUser("No se encuentra la imagen " + n.name);
        } else {
            ChagasOS.instance.notifyUser("El Chagas no permite abrir '" + n.name
                    + "'.\nAs\u00ED evitamos accidentes nacionales.");
        }
    }

    /** Renderer del \u00E1rbol: \u00EDcono seg\u00FAn el tipo de archivo. */
    class FSRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean focus) {
            Component c = super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, focus);
            Object uo = ((DefaultMutableTreeNode) value).getUserObject();
            if (uo instanceof FakeFS.Node) {
                FakeFS.Node n = (FakeFS.Node) uo;
                setIcon(iconFor(n, 16));
                setOpaque(false);
                setBackgroundNonSelectionColor(ChagasOS.BG_DEEP);
                setTextNonSelectionColor(ChagasOS.TEXT_MAIN);
                setTextSelectionColor(Color.WHITE);
            }
            return c;
        }
    }
}

