package chagasos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Configuración de ChagasOS: cambiar el nombre de usuario (JTextField),
 * el fondo de pantalla (foto del Chagas o gradiente) y el Modo Dictador.
 */
public class SettingsApp extends JInternalFrame {

    final JTextField nameField = new JTextField(14);

    public SettingsApp() {
        super("Configuraci\u00F3n", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("settings").icon(16));
        setSize(440, 400);
        setContentPane(buildUI());
    }

    JComponent buildUI() {
        final JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // cabecera con foto de perfil
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        head.setOpaque(false);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(new JLabel(Res.roundAvatar(64)));
        JPanel ht = new JPanel(new GridLayout(2, 1));
        ht.setOpaque(false);
        final JLabel t1 = new JLabel("Sesi\u00F3n: " + ChagasOS.userName);
        t1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        t1.setForeground(ChagasOS.TEXT_MAIN);
        JLabel t2 = new JLabel("Dictador Supremo \u2014 \u00E9l decide qu\u00E9 configuras");
        t2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        t2.setForeground(ChagasOS.TEXT_DIM);
        ht.add(t1);
        ht.add(t2);
        head.add(ht);
        root.add(head);
        root.add(Box.createVerticalStrut(14));

        // nombre de usuario
        JPanel pUser = section("Nombre de usuario");
        JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        userRow.setOpaque(false);
        userRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameField.setText(ChagasOS.userName);
        nameField.setBackground(ChagasOS.BG_DEEP);
        nameField.setForeground(ChagasOS.TEXT_MAIN);
        nameField.setCaretColor(Color.WHITE);
        JButton apply = new JButton("Aplicar");
        styleBtn(apply);
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChagasOS.setUserName(nameField.getText());
                t1.setText("Sesi\u00F3n: " + ChagasOS.userName);
            }
        });
        userRow.add(nameField);
        userRow.add(apply);
        pUser.add(userRow);
        root.add(pUser);
        root.add(Box.createVerticalStrut(12));

        // fondo de pantalla
        JPanel pWall = section("Fondo de pantalla");
        JRadioButton rbGrad = new JRadioButton("Gradiente ChagasOS (elegante)");
        JRadioButton rbFoto = new JRadioButton("La foto del Chagas (imponente)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbGrad);
        bg.add(rbFoto);
        styleRadio(rbGrad);
        styleRadio(rbFoto);
        rbGrad.setSelected(!ChagasOS.fotoFondo);
        rbFoto.setSelected(ChagasOS.fotoFondo);
        rbGrad.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChagasOS.setFotoFondo(false);
            }
        });
        rbFoto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChagasOS.setFotoFondo(true);
            }
        });
        pWall.add(rbGrad);
        pWall.add(rbFoto);
        root.add(pWall);
        root.add(Box.createVerticalStrut(12));

        // modo dictador
        JPanel pDict = section("Acento del sistema");
        final JCheckBox cbDict = new JCheckBox("Modo Dictador (todo en rojo)");
        cbDict.setOpaque(false);
        cbDict.setForeground(ChagasOS.TEXT_MAIN);
        cbDict.setSelected(ChagasOS.modoDictador);
        cbDict.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChagasOS.setDictador(cbDict.isSelected());
            }
        });
        pDict.add(cbDict);
        root.add(pDict);
        root.add(Box.createVerticalStrut(12));

        JLabel nota = new JLabel("Los cambios se aplican al instante (ventajas de un OS de juguete).");
        nota.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        nota.setForeground(ChagasOS.TEXT_DIM);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(nota);

        root.add(Box.createVerticalGlue());
        return root;
    }

    /** Sección con t\u00EDtulo y borde; devuelve el panel para seguir agregando. */
    JPanel section(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(ChagasOS.BG_DEEP);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ChagasOS.BG_CONTROL),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(title);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        l.setForeground(ChagasOS.accent());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(6));
        return p;
    }

    void styleBtn(JButton b) {
        b.setBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_CONTROL, 0.4f));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
    }

    void styleRadio(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setForeground(ChagasOS.TEXT_MAIN);
    }
}
