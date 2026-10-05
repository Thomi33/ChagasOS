package chagasos;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * CHAGASOS - Sistema operativo de juguete (100% meme).
 * Escritorio con JDesktopPane, JMenuBar con JMenu, barra de tareas,
 * menú Inicio con foto de perfil, ventanas JInternalFrame y apps.
 */
public class ChagasOS extends JFrame {

    public static final String OS_NAME = "ChagasOS";
    public static final String OS_VERSION = "1.0 \u00ABDictador\u00BB";

    /* Paleta del sistema */
    public static final Color BG_DEEP    = new Color(15, 17, 23);
    public static final Color BG_PANEL   = new Color(24, 27, 35);
    public static final Color BG_CONTROL = new Color(38, 42, 54);
    public static final Color TEXT_MAIN  = new Color(238, 242, 247);
    public static final Color TEXT_DIM   = new Color(150, 158, 170);
    public static final Color ACCENT_NORMAL   = new Color(0, 150, 136);
    public static final Color ACCENT_DICTADOR = new Color(200, 30, 30);

    public static String userName = "Chagas";
    public static boolean modoDictador = false;
    public static boolean fotoFondo = false;   // foto del Chagas como wallpaper

    public static ChagasOS instance;

    /* Registro de aplicaciones del sistema */
    public static final Map<String, AppDef> APPS = new LinkedHashMap<String, AppDef>();

    public static final class AppDef {
        public final String key, title, kind;
        public final Color color;

        public AppDef(String key, String title, String kind, Color color) {
            this.key = key;
            this.title = title;
            this.kind = kind;
            this.color = color;
        }

        public ImageIcon icon(int size) {
            return Res.appIcon(kind, color, size);
        }
    }

    static {
        APPS.put("player",   new AppDef("player", "Chagastify", "music", ACCENT_NORMAL));
        APPS.put("explorer", new AppDef("explorer", "Explorador de Chagas", "explorer", new Color(25, 118, 210)));
        APPS.put("terminal", new AppDef("terminal", "ChagasShell", "terminal", new Color(46, 125, 50)));
        APPS.put("notepad",  new AppDef("notepad", "Bloc de Chagas", "notepad", new Color(230, 126, 34)));
        APPS.put("tasks",    new AppDef("tasks", "Admin. de Tareas", "tasks", ACCENT_DICTADOR));
        APPS.put("calc",     new AppDef("calc", "Calculadora Chagas", "calc", new Color(94, 53, 177)));
        APPS.put("settings", new AppDef("settings", "Configuraci\u00F3n", "settings", new Color(108, 117, 125)));
        APPS.put("about",    new AppDef("about", "Acerca de ChagasOS", "about", new Color(69, 90, 100)));
    }

    Desktop desktop = new Desktop();
    Taskbar taskbar = new Taskbar();
    BlueScreen blueScreen = new BlueScreen();
    JCheckBoxMenuItem dictadorToggleItem;

    final Map<String, JInternalFrame> frames = new LinkedHashMap<String, JInternalFrame>();
    final Map<JInternalFrame, JButton> taskButtons = new LinkedHashMap<JInternalFrame, JButton>();
    int cascade = 0;

    public static void main(String[] args) {
        // BLINDAJE ANTI-CRASH: cualquier excepción no capturada (en el EDT o en
        // cualquier hilo) se registra en chagas_error.log y el sistema SIGUE VIVO
        // en vez de congelarse/morir. Nada de crashes al agrandar ventanas.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    java.io.PrintWriter pw = new java.io.PrintWriter(
                            new java.io.FileWriter("chagas_error.log", true));
                    pw.println("=== " + new Date() + "  hilo: " + t.getName() + " ===");
                    e.printStackTrace(pw);
                    pw.close();
                } catch (Exception ignored) {
                }
                System.err.println("ChagasOS: excepción capturada (hilo " + t.getName()
                        + "), el sistema sigue vivo. Detalles en chagas_error.log");
                e.printStackTrace();
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new ChagasOS();
            }
        });
    }

    public ChagasOS() {
        super(OS_NAME + " " + OS_VERSION);
        instance = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(60, 40, 1200, 730);

        setJMenuBar(buildMenuBar());
        desktop.setBackground(BG_DEEP);
        getContentPane().add(desktop, BorderLayout.CENTER);
        getContentPane().add(taskbar, BorderLayout.SOUTH);
        addDesktopIcons();
        setGlassPane(blueScreen);

        // Secuencia real de arranque: boot -> login -> escritorio
        if (!LoginScreen.showBootAndLogin(this)) {
            System.exit(0);
        }
        setVisible(true);
        toFront();
    }

    public static Color accent() {
        return modoDictador ? ACCENT_DICTADOR : ACCENT_NORMAL;
    }

    /* ==================== ESCRITORIO ==================== */

    class Desktop extends JDesktopPane {
        Desktop() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (fotoFondo) {
                // Foto del Chagas cubriendo el escritorio (usa la versión pre-escalada
                // de Res.wallpaper(): rápida incluso al maximizar/redimensionar)
                BufferedImage img = Res.wallpaper();
                double s = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
                int iw = (int) Math.round(img.getWidth() * s);
                int ih = (int) Math.round(img.getHeight() * s);
                g2.drawImage(img, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), 0, h, new Color(0, 0, 0, 60)));
                g2.fillRect(0, 0, w, h);
            } else {
                Color top = modoDictador ? new Color(66, 10, 10) : new Color(28, 32, 46);
                Color bot = modoDictador ? new Color(26, 4, 4) : new Color(11, 13, 19);
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
                g2.fillRect(0, 0, w, h);
                // burbujas decorativas
                g2.setColor(new Color(255, 255, 255, 12));
                Random rnd = new Random(69);
                for (int i = 0; i < 14; i++) {
                    int bx = rnd.nextInt(Math.max(1, w));
                    int by = rnd.nextInt(Math.max(1, h));
                    int br = 30 + rnd.nextInt(120);
                    g2.drawOval(bx, by, br, br);
                }
            }
            // marca de agua del "sistema"
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 92));
            g2.drawString(OS_NAME, w - 420, h - 120);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            g2.drawString("versi\u00F3n " + OS_VERSION, w - 420, h - 90);
            g2.setColor(new Color(255, 255, 255, 22));
            g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
            g2.drawString("\u266A el sistema operativo de los campeones \u266A", 20, h - 20);
            g2.dispose();
        }
    }

    /* ==================== ÍCONOS DEL ESCRITORIO ==================== */

    void addDesktopIcons() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (String key : new String[]{"player", "explorer", "terminal", "notepad", "tasks", "calc", "settings"}) {
            p.add(desktopIcon(key));
            p.add(Box.createVerticalStrut(10));
        }
        p.setBounds(16, 16, 130, 640);
        desktop.add(p, Integer.valueOf(-1000)); // capa inferior: las ventanas van encima
    }

    JComponent desktopIcon(final String key) {
        final AppDef a = APPS.get(key);
        final JLabel l = new JLabel(a.title, a.icon(52), JLabel.CENTER);
        l.setVerticalTextPosition(JLabel.BOTTOM);
        l.setHorizontalTextPosition(JLabel.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setToolTipText("Doble clic para abrir " + a.title);
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openApp(key);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                l.setForeground(accent());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                l.setForeground(Color.WHITE);
            }
        });
        return l;
    }


    /* ==================== GESTIÓN DE VENTANAS ==================== */

    /** Abre una app; si ya existe, la trae al frente. */
    public void openApp(String key) {
        JInternalFrame existing = frames.get(key);
        if (existing != null) {
            if (existing.isIcon()) {
                try {
                    existing.setIcon(false);
                } catch (PropertyVetoException e) {
                }
            }
            existing.moveToFront();
            try {
                existing.setSelected(true);
            } catch (PropertyVetoException e) {
            }
            return;
        }
        JInternalFrame f = createApp(key);
        if (f == null) return;
        registerFrame(key, f);
    }

    /** Crea la instancia de cada aplicación. */
    JInternalFrame createApp(String key) {
        if ("player".equals(key)) return new MusicPlayer();
        if ("explorer".equals(key)) return new FileManagerApp();
        if ("terminal".equals(key)) return new TerminalApp();
        if ("notepad".equals(key)) return new NotepadApp(null);
        if ("tasks".equals(key)) return new TaskManagerApp();
        if ("calc".equals(key)) return new CalculatorApp();
        if ("settings".equals(key)) return new SettingsApp();
        if ("about".equals(key)) return new AboutApp();
        return null;
    }

    /** Registra una ventana en el escritorio, en la barra de tareas y en el mapa. */
    void registerFrame(String key, JInternalFrame f) {
        int n = cascade++ % 7;
        f.setLocation(80 + n * 34, 30 + n * 28);
        f.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        f.addInternalFrameListener(frameSync);
        desktop.add(f);
        f.setVisible(true);
        frames.put(key, f);
        addTaskButton(key, f);
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    final InternalFrameAdapter frameSync = new InternalFrameAdapter() {
        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final JInternalFrame f = e.getInternalFrame();
            frames.entrySet().removeIf(entry -> entry.getValue() == f);
            JButton b = taskButtons.remove(f);
            if (b != null) taskbar.winPanel.remove(b);
            taskbar.revalidate();
            taskbar.repaint();
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            setBold(e.getInternalFrame(), true);
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            setBold(e.getInternalFrame(), false);
        }

        @Override
        public void internalFrameIconified(InternalFrameEvent e) {
            setBold(e.getInternalFrame(), false);
        }
    };

    void setBold(JInternalFrame f, boolean bold) {
        JButton b = taskButtons.get(f);
        if (b != null) {
            b.setFont(b.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
            b.setBackground(bold ? Res.mix(accent(), BG_PANEL, 0.45f) : BG_CONTROL);
            b.setForeground(bold ? TEXT_MAIN : TEXT_DIM);
        }
    }

    void addTaskButton(final String key, JInternalFrame f) {
        final JButton b = new JButton(f.getTitle());
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        b.setForeground(TEXT_DIM);
        b.setBackground(BG_CONTROL);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setIcon(f.getFrameIcon());
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setMargin(new Insets(4, 8, 4, 8));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openApp(key);
            }
        });
        taskButtons.put(f, b);
        taskbar.winPanel.add(b);
        taskbar.winPanel.revalidate();
        taskbar.winPanel.repaint();
    }

    /* Atajos para que otras apps abran cosas con contenido */
    public void openNotepad(String fileName) {
        openApp("notepad");
        JInternalFrame f = frames.get("notepad");
        if (f instanceof NotepadApp) ((NotepadApp) f).loadFile(fileName);
    }

    public void openPlayerPlay(String songName) {
        openApp("player");
        JInternalFrame f = frames.get("player");
        if (f instanceof MusicPlayer) ((MusicPlayer) f).playByName(songName);
    }

    /** Visor de imágenes (Foto del Chagas). */
    public void openImageViewer(String title, java.io.File file) {
        final String key = "viewer:" + title;
        JInternalFrame existing = frames.get(key);
        if (existing != null) {
            existing.moveToFront();
            try {
                existing.setSelected(true);
            } catch (PropertyVetoException e) {
            }
            return;
        }
        BufferedImage img = null;
        try {
            img = javax.imageio.ImageIO.read(file);
        } catch (Exception ignored) {
        }
        if (img == null) {
            notifyUser("No se pudo abrir la imagen, bo: " + title);
            return;
        }
        JInternalFrame f = new JInternalFrame(title, true, true, true, true);
        f.setFrameIcon(Res.appIcon("png", new Color(69, 90, 100), 16));
        f.setSize(560, 430);
        f.getContentPane().setBackground(BG_PANEL);
        JLabel pic = new JLabel(new ImageIcon(Res.scaleFit(img, 520, 360)));
        pic.setHorizontalAlignment(SwingConstants.CENTER);
        f.getContentPane().add(new JScrollPane(pic));
        registerFrame(key, f);
    }


    /* ==================== BARRA DE TAREAS ==================== */

    class Taskbar extends JPanel {
        final JButton inicioBtn = new JButton();
        final JPanel winPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        final JLabel songLabel = new JLabel(" ");
        final JLabel clockLabel = new JLabel();

        Taskbar() {
            setPreferredSize(new Dimension(0, 48));
            setBackground(BG_DEEP);
            setLayout(new BorderLayout());
            winPanel.setOpaque(false);

            inicioBtn.setIcon(Res.roundAvatar(30));
            inicioBtn.setText("Inicio");
            inicioBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            inicioBtn.setForeground(TEXT_MAIN);
            inicioBtn.setBackground(BG_CONTROL);
            inicioBtn.setOpaque(true);
            inicioBtn.setBorderPainted(false);
            inicioBtn.setFocusPainted(false);
            inicioBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
            inicioBtn.setMargin(new Insets(4, 10, 4, 12));
            inicioBtn.setToolTipText("Aqu\u00ED vive el men\u00FA de inicio (y tu foto de perfil)");
            inicioBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    inicioBtn.setBackground(Res.mix(accent(), BG_CONTROL, 0.35f));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    inicioBtn.setBackground(BG_CONTROL);
                }
            });
            inicioBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    toggleStartMenu();
                }
            });

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            left.setOpaque(false);
            left.add(inicioBtn);
            left.add(winPanel);
            add(left, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            right.setOpaque(false);
            songLabel.setForeground(TEXT_DIM);
            songLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            clockLabel.setForeground(TEXT_MAIN);
            clockLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            right.add(songLabel);
            right.add(clockLabel);
            add(right, BorderLayout.EAST);

            new javax.swing.Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tick();
                }
            }).start();
        }

        void tick() {
            Date now = new Date();
            String hhmm = new SimpleDateFormat("HH:mm").format(now);
            String date = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es")).format(now);
            clockLabel.setText("<html><div style='text-align:right'><b>" + hhmm
                    + "</b><br><font size='2' color='#96a0aa'>" + date + "</font></div></html>");
        }

        void setSong(String s) {
            songLabel.setText(s);
        }
    }

    /** Notificación tipo "aviso del sistema" para las apps. */
    public void notifyUser(String msg) {
        JOptionPane.showMessageDialog(this, msg, OS_NAME, JOptionPane.INFORMATION_MESSAGE,
                Res.roundAvatar(64));
    }


    /* ==================== MENÚ DE INICIO ==================== */

    JPopupMenu startPopup;
    JTextField startSearch;
    JPanel appListPanel;

    void toggleStartMenu() {
        if (startPopup != null && startPopup.isVisible()) {
            startPopup.setVisible(false);
            return;
        }
        buildStartMenu();
        int h = startPopup.getPreferredSize().height;
        startPopup.show(taskbar.inicioBtn, -6, -h + 2);
        startSearch.setText("");
        filterApps();
        startSearch.requestFocusInWindow();
    }

    void buildStartMenu() {
        startPopup = new JPopupMenu();
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_PANEL);
        root.setPreferredSize(new Dimension(330, 470));
        root.setBorder(BorderFactory.createLineBorder(Res.mix(accent(), Color.BLACK, 0.35f)));

        // --- cabecera con foto de perfil del usuario ---
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        head.setBackground(accent());
        head.add(new JLabel(Res.roundAvatar(56)));
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        JLabel name = new JLabel(userName);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        name.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Maven Supremo \u2014 sesi\u00F3n ROOT");
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        sub.setForeground(new Color(255, 255, 255, 210));
        names.add(name);
        names.add(sub);
        head.add(names);

        // --- buscador ---
        JPanel searchP = new JPanel(new BorderLayout());
        searchP.setBackground(BG_PANEL);
        searchP.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        startSearch = new JTextField();
        startSearch.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        startSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BG_CONTROL, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        startSearch.setToolTipText("Busca aplicaciones... (JTextField en acci\u00F3n)");
        startSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterApps();
            }

            public void removeUpdate(DocumentEvent e) {
                filterApps();
            }

            public void changedUpdate(DocumentEvent e) {
                filterApps();
            }
        });
        searchP.add(startSearch);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(searchP, BorderLayout.CENTER);

        // --- lista de apps ---
        appListPanel = new JPanel();
        appListPanel.setLayout(new BoxLayout(appListPanel, BoxLayout.Y_AXIS));
        appListPanel.setBackground(BG_PANEL);
        for (final String key : APPS.keySet()) {
            appListPanel.add(appRow(key));
        }
        JScrollPane scroll = new JScrollPane(appListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setPreferredSize(new Dimension(330, 285));

        // --- pie con acciones de sesión ---
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        foot.setBackground(BG_DEEP);
        foot.add(powerBtn("Cerrar sesi\u00F3n", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startPopup.setVisible(false);
                logout();
            }
        }));
        foot.add(powerBtn("Reiniciar", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startPopup.setVisible(false);
                reboot();
            }
        }));
        foot.add(powerBtn("Apagar", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startPopup.setVisible(false);
                shutdown();
            }
        }));

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(foot, BorderLayout.SOUTH);
        startPopup.add(root);
        startPopup.pack();
    }


    /** Fila de aplicación del menú de inicio (JButton plano con hover). */
    JButton appRow(final String key) {
        final AppDef a = APPS.get(key);
        final JButton b = new JButton(a.title, a.icon(32));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setIconTextGap(10);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        b.setForeground(TEXT_MAIN);
        b.setBackground(BG_PANEL);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(320, 40));
        b.setMargin(new Insets(6, 10, 6, 10));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(Res.mix(accent(), BG_PANEL, 0.40f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(BG_PANEL);
            }
        });
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startPopup.setVisible(false);
                openApp(key);
            }
        });
        return b;
    }

    JButton powerBtn(String text, ActionListener l) {
        final JButton b = new JButton(text);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        b.setForeground(TEXT_MAIN);
        b.setBackground(BG_CONTROL);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 8, 4, 8));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(Res.mix(accent(), BG_CONTROL, 0.4f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(BG_CONTROL);
            }
        });
        b.addActionListener(l);
        return b;
    }

    void filterApps() {
        String q = startSearch.getText().trim().toLowerCase();
        for (Component c : appListPanel.getComponents()) {
            if (c instanceof JButton) {
                c.setVisible(q.isEmpty() || ((JButton) c).getText().toLowerCase().contains(q));
            }
        }
        appListPanel.revalidate();
        appListPanel.repaint();
    }


    /* ==================== BARRA DE MENÚ (JMenuBar + JMenu) ==================== */

    JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu mSistema = new JMenu("Sistema");
        mSistema.add(mi("Abrir Chagastify", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openApp("player");
            }
        }));
        mSistema.addSeparator();
        mSistema.add(mi("Cerrar sesi\u00F3n", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        }));
        mSistema.add(mi("Reiniciar", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reboot();
            }
        }));
        mSistema.add(mi("Apagar", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                shutdown();
            }
        }));
        mSistema.addSeparator();
        mSistema.add(mi("Crashear el sistema (BSOD)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                blueScreen();
            }
        }));
        mb.add(mSistema);

        JMenu mApps = new JMenu("Aplicaciones");
        for (final String key : APPS.keySet()) {
            mApps.add(mi(APPS.get(key).title, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openApp(key);
                }
            }));
        }
        mb.add(mApps);

        JMenu mChagas = new JMenu("Chagas");
        mChagas.add(mi("Bendici\u00F3n del Chagas", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(ChagasOS.this, randomBendicion(),
                        "Bendici\u00F3n oficial", JOptionPane.INFORMATION_MESSAGE, Res.roundAvatar(64));
            }
        }));
        dictadorToggleItem = new JCheckBoxMenuItem("Modo Dictador (rojo)");
        dictadorToggleItem.setToolTipText("Cambia el acento del sistema a rojo menstruacion");
        dictadorToggleItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDictador(dictadorToggleItem.getState());
            }
        });
        mChagas.add(dictadorToggleItem);
        mChagas.add(mi("Ver al Chagas (foto)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.io.File f = Res.find("profile.png");
                if (f != null) openImageViewer("Foto del Chagas", f);
                else notifyUser("El Chagas est\u00E1 muy ocupado para fotos ahora mismo.");
            }
        }));
        mChagas.add(mi("Cita del Chagas", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(ChagasOS.this, "\u201C" + randomChagasQuote() + "\u201D",
                        "El Chagas dice", JOptionPane.PLAIN_MESSAGE, Res.roundAvatar(64));
            }
        }));
        mb.add(mChagas);

        JMenu mAyuda = new JMenu("Ayuda");
        mAyuda.add(mi("Gu\u00EDa r\u00E1pida", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGuia();
            }
        }));
        mAyuda.add(mi("Acerca de ChagasOS", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openApp("about");
            }
        }));
        mb.add(mAyuda);
        return mb;
    }

    JMenuItem mi(String t, ActionListener l) {
        JMenuItem m = new JMenuItem(t);
        m.addActionListener(l);
        return m;
    }

    void showGuia() {
        JOptionPane.showMessageDialog(this,
                "GU\u00CDA R\u00C1PIDA DE CHAGASOS\n\n"
                + "\u2022 Men\u00FA Inicio: abajo a la izquierda (con tu foto de perfil).\n"
                + "\u2022 Aplicaciones: doble clic en los \u00EDconos del escritorio.\n"
                + "\u2022 Chagastify: reproductor oficial (solo m\u00FAsica del Chagas, el Jarvis es el DJ).\n"
                + "\u2022 ChagasShell: escribe 'help' para ver todos los comandos.\n"
                + "\u2022 Explorador: doble clic en los archivos para abrirlos.\n"
                + "\u2022 \u00BFProblemas? Men\u00FA Chagas -> Bendici\u00F3n. O reinicia. O repr\u00F3chalas.",
                "Ayuda", JOptionPane.INFORMATION_MESSAGE, Res.roundAvatar(64));
    }


    /* ==================== SESIÓN: BSOD, CERRAR, REINICIAR, APAGAR ==================== */

    /** Pantalla azul del Chagas (glass pane del frame principal). */
    class BlueScreen extends JPanel {
        BlueScreen() {
            setVisible(false);
            setFocusable(true);
            setBackground(new Color(6, 70, 166));
            java.awt.event.ActionListener reiniciar = new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    setVisible(false);
                    reboot();
                }
            };
            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        reiniciar.actionPerformed(null);
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    reiniciar.actionPerformed(null);
                }
            });
        }

        void activate() {
            MusicPlayer.stopAny();
            setVisible(true);
            revalidate();
            repaint();
            requestFocusInWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(6, 70, 166));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
            g2.drawString(":(", 70, 160);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            String[] lines = {
                    "ChagasOS encontr\u00F3 un problema y necesita reiniciarse. El",
                    "Maven se cayo, pero no el ChagasOS.",
                    "",
                    "C\u00F3digo de error: CHAGAS_0xD1CT4D0R",
                    "Lo que fall\u00F3: TODO, como siempre.",
                    "",
                    "Recolectando notas para febrero... Listo.",
                    "",
                    "Presiona ENTER o haz clic en cualquier parte para reiniciar ChagasOS",
                    "(el Chagas no pide perd\u00F3n)."
            };
            int y = 230;
            for (String s : lines) {
                g2.drawString(s, 80, y);
                y += 32;
            }
            g2.dispose();
        }
    }

    public void blueScreen() {
        blueScreen.activate();
    }

    void logout() {
        closeAllApps();
        setVisible(false);
        if (!LoginScreen.showBootAndLogin(this)) System.exit(0);
        setVisible(true);
    }

    void reboot() {
        closeAllApps();
        setVisible(false);
        LoginScreen.showShutting(this, "Reiniciando ChagasOS...");
        if (!LoginScreen.showBootAndLogin(this)) System.exit(0);
        setVisible(true);
    }

    void shutdown() {
        closeAllApps();
        LoginScreen.showShutting(this, "Apagando ChagasOS...");
        System.exit(0);
    }

    void closeAllApps() {
        MusicPlayer.stopAny();
        for (JInternalFrame f : new ArrayList<JInternalFrame>(frames.values())) {
            try {
                f.setClosed(true);
            } catch (PropertyVetoException e) {
                f.dispose();
            }
        }
        frames.clear();
        taskButtons.clear();
        taskbar.winPanel.removeAll();
        taskbar.revalidate();
        taskbar.repaint();
    }

    /* ==================== ESTILOS Y FRASES ==================== */

    public static void setUserName(String name) {
        String clean = (name == null) ? "" : name.trim();
        userName = clean.isEmpty() ? "Chagas" : clean;
        if (instance != null) {
            instance.notifyUser("Papeles en regla: ahora te llamas \"" + userName + "\".");
        }
    }

    public static void setDictador(boolean b) {
        modoDictador = b;
        if (instance != null) {
            if (instance.dictadorToggleItem != null) instance.dictadorToggleItem.setState(b);
            instance.desktop.repaint();
            instance.taskbar.repaint();
        }
    }

    public static void setFotoFondo(boolean b) {
        fotoFondo = b;
        if (instance != null) instance.desktop.repaint();
    }

    public static String randomBendicion() {

        String[] b = {

                "Que Maven compile a la primera y Chagas no encuentre motivos para mandarte a febrero.",

                "Que Xaguna esté de tu lado cuando aparezca el poca facha.",

                "Que tus queries SQL funcionen y Chagas no encuentre errores suficientes para desaprobarte.",

                "Que el Chagas bendiga tu código y Claude no deje sus huellas.",

                "Que jamás aparezca un 'Gay_zone' en tu proyecto cuando Chagas esté mirando.",

                "Que Maven resuelva todas tus dependencias antes de que Chagas pierda la paciencia.",

                "Que Salto quede atrás, Rivera te reciba y el código compile.",

                "Que el Chagas te dé sabiduría para distinguir entre GameZone y el programa prohibido."

    };

        return b[new Random().nextInt(b.length)];

}

    public static String randomChagasQuote() {

        String[] q = {

                "El Chagas no usa antivirus. El virus le tiene miedo.",

                "En este OS no hay bugs, hay características no deseadas por el usuario.",

                "El Chagas nunca pierde. A veces gana en segundo lugar.",

                "Ctrl+Z no funciona contra las decisiones del Chagas.",

                "Este sistema operativo es estable. Estable de inestable.",

                "Reiniciar es señal de grandeza. Yo reinicio todos los Maven hechos por Claudio.",

                "Se me pudo haber quemado el lluvero, pero no se quema el ChagasOS. Ojalá fuera cierto.",

                "Si encuentro un programa llamado 'Gay_zone', se van todos a febrero.",

                "El Chagas no teme a los errores de Maven. Los errores de Maven temen al Chagas.",

                "El vibe coding ha terminado. Ahora sufran con Maven.",

                "ChatGPT, Claude y Gemini no tienen permiso para tocar este proyecto.",

                "Si el código funciona pero lo hizo Claude, técnicamente sigue siendo sospechoso.",

                "Xaguna protege. Chagas juzga. Lauro aparece. El sistema colapsa.",

                "El Chagas no manda trabajos a febrero. El código los manda solo.",

                "En caso de duda, culpá a Maven. En caso de Maven, culpá a Claudio.",

                "El Chagas llegó a Rivera y Maven nunca volvió a ser el mismo.",

                "No importa cuánto código escribas: Chagas siempre encontrará una línea que está mal.",

                "El ChagasOS detectó inteligencia artificial. Procediendo a preparar febrero.",

                "GameZone es permitido. El resto será revisado por el Chagas.",

                "Un proyecto sin errores es sospechoso. Un proyecto sin Maven es imposible.",

                "El Chagas no necesita debugger. Mira el código y sabe quién fue.",
                
                "Que la ley de la silla nos protega de las manos del Xaguna y de los errores de Maven."

    };

    return q[new Random().nextInt(q.length)];

}

}

