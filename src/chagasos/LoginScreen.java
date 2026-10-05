package chagasos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Pantallas de arranque de ChagasOS:
 *  - BootSplash: el "BIOS del Chagas" con barra de progreso y mensajes.
 *  - LoginDialog: inicio de sesi\u00F3n con la foto de perfil.
 *  - showShutting: pantalla de apagado/reinicio.
 */
public final class LoginScreen {

    private LoginScreen() {
    }

    /** Boot + login. Devuelve true si el usuario inici\u00F3 sesi\u00F3n. */
    public static boolean showBootAndLogin(JFrame owner) {
        new BootSplash().run();
        return new LoginDialog(owner).run();
    }

    /** Pantalla de "Apagando..." / "Reiniciando..." (bloquea ~2 seg). */
    public static void showShutting(JFrame owner, String msg) {
        final JDialog d = new JDialog(owner, "ChagasOS", true);
        d.setUndecorated(true);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(ChagasOS.BG_DEEP);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ChagasOS.accent(), 2),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));
        JLabel t = new JLabel(msg);
        t.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        t.setForeground(ChagasOS.TEXT_MAIN);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(t);
        JLabel s = new JLabel("El Chagas se despide con honores...");
        s.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        s.setForeground(ChagasOS.TEXT_DIM);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(8));
        p.add(s);
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setForeground(ChagasOS.accent());
        pb.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(10));
        p.add(pb);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(owner != null && owner.isShowing() ? owner : null);
        javax.swing.Timer t1 = new javax.swing.Timer(2100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                d.dispose();
            }
        });
        t1.setRepeats(false);
        t1.start();
        d.setVisible(true);
    }

    /* ==================== BOOT SPLASH ==================== */

    static final String[] BOOT_LINES = {
            "Iniciando ChagasCore...",
            "Montando el ego (6.2 GB)...",
            "Cargando himno nacional...",
            "Escondiendo pruebas...",
            "Contando los billetes...",
            "Purificando el agua...",
            "Instalando dictadura (ya viene incluida)...",
            "\u00DAltimos ajustes..."
    };

    static class BootSplash {
        void run() {
            final JDialog d = new JDialog((Frame) null, "ChagasOS", true);
            d.setUndecorated(true);

            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(ChagasOS.BG_DEEP);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ChagasOS.accent(), 2),
                    BorderFactory.createEmptyBorder(26, 40, 22, 40)));

            final JLabel avatar = new JLabel(Res.appIcon("about", ChagasOS.BG_CONTROL, 110));
            avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(avatar);
            p.add(Box.createVerticalStrut(12));

            JLabel title = new JLabel(ChagasOS.OS_NAME);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            title.setForeground(ChagasOS.TEXT_MAIN);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(title);

            JLabel ver = new JLabel("versi\u00F3n " + ChagasOS.OS_VERSION);
            ver.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            ver.setForeground(ChagasOS.TEXT_DIM);
            ver.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(ver);
            p.add(Box.createVerticalStrut(14));

            final JProgressBar pb = new JProgressBar(0, BOOT_LINES.length);
            pb.setForeground(ChagasOS.accent());
            pb.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(pb);
            p.add(Box.createVerticalStrut(8));

            final JLabel status = new JLabel(BOOT_LINES[0] + " ");
            status.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            status.setForeground(ChagasOS.TEXT_DIM);
            status.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(status);

            JLabel bios = new JLabel("BIOS Chagas v6.9 \u2014 presiona DEL para nada");
            bios.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            bios.setForeground(new Color(90, 96, 106));
            bios.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(Box.createVerticalStrut(10));
            p.add(bios);

            d.setContentPane(p);
            d.pack();
            d.setLocationRelativeTo(null);

            // cargar la foto de perfil mientras "bootea"
            Thread loader = new Thread(new Runnable() {
                public void run() {
                    Res.profile();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            avatar.setIcon(Res.roundAvatar(110));
                        }
                    });
                }
            }, "CargandoPerfil");
            loader.setDaemon(true);
            loader.start();

            final int[] step = {0};
            final javax.swing.Timer t = new javax.swing.Timer(430, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    step[0]++;
                    if (step[0] >= BOOT_LINES.length) {
                        ((javax.swing.Timer) e.getSource()).stop();
                        d.dispose();
                        return;
                    }
                    status.setText(BOOT_LINES[step[0]] + " ");
                    pb.setValue(step[0]);
                }
            });
            t.start();
            d.setVisible(true);
        }
    }


    /* ==================== LOGIN ==================== */

    static class LoginDialog {
        final JDialog d;
        boolean ok = false;

        LoginDialog(JFrame owner) {
            d = new JDialog(owner, "Iniciar sesi\u00F3n \u2014 " + ChagasOS.OS_NAME, true);
            d.setUndecorated(true);
            build();
        }

        void build() {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(ChagasOS.BG_DEEP);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ChagasOS.accent(), 2),
                    BorderFactory.createEmptyBorder(24, 36, 18, 36)));

            JLabel pic = new JLabel(Res.roundAvatar(130));
            pic.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(pic);
            p.add(Box.createVerticalStrut(10));

            JLabel name = new JLabel(ChagasOS.userName);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            name.setForeground(ChagasOS.TEXT_MAIN);
            name.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(name);

            JLabel role = new JLabel("Dictador Supremo del Sistema");
            role.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
            role.setForeground(ChagasOS.TEXT_DIM);
            role.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(role);
            p.add(Box.createVerticalStrut(16));

            JLabel pwLbl = new JLabel("Contrase\u00F1a:");
            pwLbl.setForeground(ChagasOS.TEXT_MAIN);
            pwLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(pwLbl);

            final JPasswordField pw = new JPasswordField();
            pw.setBackground(ChagasOS.BG_PANEL);
            pw.setForeground(Color.WHITE);
            pw.setCaretColor(Color.WHITE);
            pw.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ChagasOS.BG_CONTROL),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            p.add(pw);
            p.add(Box.createVerticalStrut(6));

            JLabel hint = new JLabel("(el Chagas acepta cualquier contrase\u00F1a. Es muy confiado)");
            hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
            hint.setForeground(ChagasOS.TEXT_DIM);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(hint);
            p.add(Box.createVerticalStrut(14));

            JButton login = new JButton("Iniciar sesi\u00F3n");
            login.setAlignmentX(Component.CENTER_ALIGNMENT);
            login.setBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_CONTROL, 0.3f));
            login.setForeground(Color.WHITE);
            login.setFocusPainted(false);
            login.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ok = true;
                    d.dispose();
                }
            });
            p.add(login);
            p.add(Box.createVerticalStrut(8));

            JButton off = new JButton("Apagar el sistema");
            off.setAlignmentX(Component.CENTER_ALIGNMENT);
            off.setContentAreaFilled(false);
            off.setFocusPainted(false);
            off.setForeground(ChagasOS.TEXT_DIM);
            off.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            p.add(off);

            d.setContentPane(p);
            d.pack();
            d.setLocationRelativeTo(null);
            d.getRootPane().setDefaultButton(login);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    pw.requestFocusInWindow();
                }
            });
        }

        boolean run() {
            d.setVisible(true);   // bloquea hasta cerrar
            return ok;
        }
    }
}

