package chagasos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Acerca de ChagasOS: ficha t\u00E9cnica 100% honesta (no) del sistema,
 * con la foto de perfil del Chagas.
 */
public class AboutApp extends JInternalFrame {

    public AboutApp() {
        super("Acerca de ChagasOS", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("about").icon(16));
        setSize(400, 420);
        setContentPane(buildUI());
    }

    JComponent buildUI() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JLabel pic = new JLabel(Res.roundAvatar(110));
        pic.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(pic);
        root.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("ChagasOS");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        title.setForeground(ChagasOS.TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(title);

        JLabel ver = new JLabel("Versi\u00F3n 1.0 \u00ABDictador\u00BB");
        ver.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 13));
        ver.setForeground(ChagasOS.accent());
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(ver);
        root.add(Box.createVerticalStrut(14));

        String[] specs = {
                "Kernel:    chagas-core 6.9.1 (basado en nada)",
                "RAM:       8 GB (mentira, son 2 y el ego usa media)",
                "Procesador: ChagIntel i9 \u00ABFandango\u00BB @ 6.9 GHz",
                "Licencia:  Chagas Public License \u2014 copia todo menos el ego",
                "Soporte:   los martes, si hay se\u00F1al",
                "\u00A9 Chagas Corporation. Todos los derechos patrullados."
        };
        for (String s : specs) {
            JLabel l = new JLabel(s);
            l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            l.setForeground(ChagasOS.TEXT_DIM);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            root.add(l);
            root.add(Box.createVerticalStrut(4));
        }
        root.add(Box.createVerticalStrut(14));

        JButton ok = new JButton("\u00A1Viva el Chagas!");
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.setBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_CONTROL, 0.35f));
        ok.setForeground(Color.WHITE);
        ok.setFocusPainted(false);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        root.add(ok);
        root.add(Box.createVerticalGlue());
        return root;
    }
}
