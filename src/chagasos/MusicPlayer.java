package chagasos;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Chagastify: el reproductor oficial de ChagasOS.
 * Reproduce (de verdad) los MP3 adjuntos usando JLayer + JavaSound.
 */
public class MusicPlayer extends JInternalFrame {

    /** Referencia a la instancia abierta (para el bot\u00F3n "parar" del sistema). */
    static MusicPlayer current;

    static final class Song {
        final String title;
        final File file;
        int totalFrames = -1;
        float msPerFrame = 24f;

        Song(String title, File file) {
            this.title = title;
            this.file = file;
        }
    }

    final List<Song> songs = new ArrayList<Song>();
    int index = -1;
    Engine engine;

    DefaultListModel<String> listModel = new DefaultListModel<String>();
    JList<String> songList;
    JLabel nowLbl = new JLabel("Nada suena todav\u00EDa...");
    JLabel stateLbl = new JLabel("Detenido");
    JLabel timeLbl = new JLabel("0:00");
    JLabel durLbl = new JLabel("0:00");
    JSlider progress = new JSlider(0, 10000, 0);
    JSlider volume = new JSlider(0, 100, 80);
    JButton prevBtn, playBtn, pauseBtn, stopBtn, nextBtn;
    Visualizer vis;
    javax.swing.Timer uiTimer, visTimer;
    boolean draggingSlider = false;

    public MusicPlayer() {
        super("Chagastify", true, true, true, true);
        current = this;
        setFrameIcon(ChagasOS.APPS.get("player").icon(16));
        setSize(500, 440);
        for (FakeFS.Node n : FakeFS.songs()) {
            File f = (n.realFile != null) ? n.realFile : Res.find(n.name);
            songs.add(new Song(n.name, f));
        }
        setContentPane(buildUI());
        refreshList();
        scanDurations();
        if (ChagasOS.instance != null) ChagasOS.instance.taskbar.setSong("\u266A Chagastify listo");
    }

    /* ==================== MOTOR DE MP3 (JLayer) ==================== */

    /**
     * Motor de reproducci\u00F3n: decodifica MP3 con JLayer y lo escribe en un
     * SourceDataLine (JavaSound). Soporta pausa, stop, volumen (MASTER_GAIN)
     * y arranque desde un frame dado (para adelantar la canci\u00F3n).
     */
    final class Engine implements Runnable {
        final File file;
        final int startFrame;
        final Runnable onEnd;
        Thread thread;
        volatile boolean paused, stopped;
        volatile int framePos;
        volatile float msPerFrame = 24f;
        volatile float volume = 0.8f;
        SourceDataLine line;
        FloatControl gain;

        Engine(File file, int startFrame, Runnable onEnd) {
            this.file = file;
            this.startFrame = startFrame;
            this.onEnd = onEnd;
        }

        void start() {
            thread = new Thread(this, "ChagastifyEngine");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            stopped = true;
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.stop();
                    l.flush();
                    l.close();
                } catch (Throwable ignored) {
                }
            }
        }

        void setVolume(float v) {
            volume = Math.max(0f, Math.min(1f, v));
            applyVolume();
        }

        void applyVolume() {
            FloatControl g = gain;
            if (g == null) return;
            float db = (volume <= 0.005f) ? g.getMinimum()
                    : (float) (20.0 * Math.log10(Math.max(volume, 0.005)));
            db = Math.max(g.getMinimum(), Math.min(g.getMaximum(), db));
            try {
                g.setValue(db);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void run() {
            Bitstream bs = null;
            SourceDataLine l = null;
            try {
                bs = new Bitstream(new BufferedInputStream(new FileInputStream(file), 1 << 16));
                Decoder dec = new Decoder();
                Header h = null;
                // saltar frames (seek)
                for (int i = 0; i < startFrame; i++) {
                    h = bs.readFrame();
                    if (h == null) return;
                    bs.closeFrame();
                }
                h = bs.readFrame();
                if (h != null) msPerFrame = h.ms_per_frame();
                byte[] buf = null;
                while (h != null && !stopped) {
                    if (paused) {
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            return;
                        }
                        continue;
                    }
                    try {
                        Obuffer ob = dec.decodeFrame(h, bs);
                        if (ob instanceof SampleBuffer) {
                            SampleBuffer sb = (SampleBuffer) ob;
                            if (l == null) {
                                l = openLine(sb);
                                line = l;
                            }
                            if (l != null) {
                                if (buf == null) buf = new byte[sb.getBufferLength() * 2];
                                short[] pcm = sb.getBuffer();
                                int n = sb.getBufferLength();
                                for (int i = 0; i < n; i++) {
                                    buf[2 * i] = (byte) (pcm[i] & 0xFF);
                                    buf[2 * i + 1] = (byte) ((pcm[i] >> 8) & 0xFF);
                                }
                                l.write(buf, 0, n * 2);
                            } else {
                                Thread.sleep((long) msPerFrame); // sin audio: avance en tiempo real
                            }
                        }
                    } catch (Throwable t) {
                        // frame corrupto del MP3: se salta y a otra cosa
                    }
                    try {
                        bs.closeFrame();
                    } catch (Throwable ignored) {
                    }
                    framePos++;
                    h = bs.readFrame();
                }
                if (!stopped && l != null) {
                    try {
                        l.drain();
                    } catch (Throwable ignored) {
                    }
                }
                if (!stopped && onEnd != null) {
                    SwingUtilities.invokeLater(onEnd);
                }
            } catch (Throwable ignored) {
            } finally {
                if (l != null) {
                    try {
                        l.close();
                    } catch (Throwable ignored) {
                    }
                }
                if (bs != null) {
                    try {
                        bs.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        SourceDataLine openLine(SampleBuffer sb) {
            try {
                AudioFormat fmt = new AudioFormat(sb.getSampleFrequency(), 16,
                        sb.getChannelCount(), true, false);
                SourceDataLine l = javax.sound.sampled.AudioSystem.getSourceDataLine(fmt);
                l.open(fmt, 1 << 16);
                l.start();
                if (l.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    gain = (FloatControl) l.getControl(FloatControl.Type.MASTER_GAIN);
                    applyVolume();
                }
                return l;
            } catch (LineUnavailableException e) {
                return null;
            }
        }
    }


    /* ==================== INTERFAZ DEL REPRODUCTOR ==================== */

    JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // cabecera: \u00EDcono + t\u00EDtulo
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        JLabel bigIcon = new JLabel(ChagasOS.APPS.get("player").icon(42));
        head.add(bigIcon);
        JPanel headTxt = new JPanel(new GridLayout(2, 1));
        headTxt.setOpaque(false);
        JLabel title = new JLabel("Chagastify");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(ChagasOS.TEXT_MAIN);
        JLabel subtitle = new JLabel("Reproductor oficial \u2014 solo m\u00FAsica del Chagas (es ley)");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        subtitle.setForeground(ChagasOS.TEXT_DIM);
        headTxt.add(title);
        headTxt.add(subtitle);
        head.add(headTxt);
        root.add(head, BorderLayout.NORTH);

        // lista de canciones (izquierda, dentro de un JScrollPane)
        songList = new JList<String>(listModel);
        songList.setCellRenderer(new SongRenderer());
        songList.setBackground(ChagasOS.BG_DEEP);
        songList.setForeground(ChagasOS.TEXT_MAIN);
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songList.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        songList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int sel = songList.locationToIndex(e.getPoint());
                    if (sel >= 0) playIndex(sel);
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(songList);
        listScroll.setPreferredSize(new Dimension(200, 300));
        listScroll.setBorder(BorderFactory.createLineBorder(ChagasOS.BG_CONTROL));

        // panel de reproducci\u00F3n (derecha)
        JPanel play = new JPanel();
        play.setLayout(new BoxLayout(play, BoxLayout.Y_AXIS));
        play.setOpaque(false);

        // visualizador de "audio" (barras, 100% marketing)
        vis = new Visualizer();
        vis.setPreferredSize(new Dimension(240, 70));
        vis.setAlignmentX(Component.LEFT_ALIGNMENT);
        vis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        play.add(vis);
        play.add(Box.createVerticalStrut(8));

        nowLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        nowLbl.setForeground(ChagasOS.TEXT_MAIN);
        nowLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        play.add(nowLbl);

        stateLbl.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        stateLbl.setForeground(ChagasOS.TEXT_DIM);
        stateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        play.add(stateLbl);
        play.add(Box.createVerticalStrut(6));

        // barra de progreso + tiempos
        progress.setOpaque(false);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                draggingSlider = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingSlider = false;
                seekTo(progress.getValue());
            }
        });
        play.add(progress);
        JPanel times = new JPanel(new BorderLayout());
        times.setOpaque(false);
        timeLbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        timeLbl.setForeground(ChagasOS.TEXT_DIM);
        durLbl.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        durLbl.setForeground(ChagasOS.TEXT_DIM);
        times.add(timeLbl, BorderLayout.WEST);
        times.add(durLbl, BorderLayout.EAST);
        times.setAlignmentX(Component.LEFT_ALIGNMENT);
        play.add(times);
        play.add(Box.createVerticalStrut(8));

        // volumen
        JPanel volP = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        volP.setOpaque(false);
        volP.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel volT = new JLabel("VOL");
        volT.setForeground(ChagasOS.TEXT_DIM);
        volT.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        volP.add(volT);
        volP.add(volume);
        play.add(volP);

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(listScroll, BorderLayout.WEST);
        center.add(play, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        // botones de control (JButtons con glifos seguros)
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btns.setOpaque(false);
        prevBtn = ctrlBtn("|<");
        playBtn = ctrlBtn("\u25B6");          // ▶
        pauseBtn = ctrlBtn("\u25AE\u25AE");   // ▮▮
        stopBtn = ctrlBtn("\u25A0");          // ■
        nextBtn = ctrlBtn(">|");
        btns.add(prevBtn);
        btns.add(playBtn);
        btns.add(pauseBtn);
        btns.add(stopBtn);
        btns.add(nextBtn);
        root.add(btns, BorderLayout.SOUTH);

        wireActions();
        return root;
    }


    void wireActions() {
        prevBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!songs.isEmpty()) playIndex((index - 1 + songs.size()) % songs.size());
            }
        });
        nextBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!songs.isEmpty()) playIndex((index + 1) % songs.size());
            }
        });
        playBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (engine == null) {
                    int sel = songList.getSelectedIndex();
                    playIndex(sel >= 0 ? sel : 0);
                } else {
                    engine.paused = false;
                    stateLbl.setText("Reproduciendo");
                }
            }
        });
        pauseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (engine != null) {
                    engine.paused = !engine.paused;
                    stateLbl.setText(engine.paused ? "En pausa" : "Reproduciendo");
                }
            }
        });
        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                stopPlayback();
            }
        });
        volume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                if (engine != null) engine.setVolume(volume.getValue() / 100f);
            }
        });

        // timer que refresca tiempos/progreso 2 veces por segundo
        uiTimer = new javax.swing.Timer(500, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                updateTick();
            }
        });
        uiTimer.start();
        // timer del visualizador
        visTimer = new javax.swing.Timer(110, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (engine != null && !engine.paused) vis.step();
                else vis.decay();
                vis.repaint();
            }
        });
        visTimer.start();
    }

    JButton ctrlBtn(String glyph) {
        JButton b = new JButton(glyph);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        b.setForeground(ChagasOS.TEXT_MAIN);
        b.setBackground(ChagasOS.BG_CONTROL);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(64, 40));
        return b;
    }

    /** Renderizador de la lista: \u00EDcono musical + nombre + duraci\u00F3n. */
    class SongRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int idx,
                                                      boolean sel, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
            l.setIcon(Res.appIcon("music", ChagasOS.ACCENT_NORMAL, 18));
            if (idx == index) {
                l.setForeground(ChagasOS.accent());
                l.setText(l.getText() + "   \u266B");
            }
            return l;
        }
    }

    /** Visualizador: barras que se mueven "con la m\u00FAsica" (con cari\u00F1o). */
    class Visualizer extends JPanel {
        final float[] bars = new float[24];

        Visualizer() {
            setBackground(ChagasOS.BG_DEEP);
        }

        void step() {
            java.util.Random r = new java.util.Random();
            for (int i = 0; i < bars.length; i++) {
                float target = r.nextFloat();
                bars[i] = bars[i] * 0.45f + target * 0.55f;
            }
        }

        void decay() {
            for (int i = 0; i < bars.length; i++) bars[i] *= 0.82f;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int n = bars.length;
            int w = Math.max(1, getWidth() / n);
            for (int i = 0; i < n; i++) {
                int bh = (int) (bars[i] * (getHeight() - 6));
                g.setColor(Res.mix(ChagasOS.accent(), Color.WHITE, bars[i] * 0.6f));
                g.fillRect(i * w + 1, getHeight() - 2 - bh, w - 3, bh);
            }
        }
    }


    /* ==================== ACCIONES DEL REPRODUCTOR ==================== */

    void playIndex(int i) {
        if (songs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay m\u00FAsica del Chagas en el sistema.\n"
                    + "Adjunta los MP3 a la carpeta resources/", "Chagastify",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Song s = songs.get(i);
        if (s.file == null || !s.file.isFile()) {
            JOptionPane.showMessageDialog(this, "No encuentro el archivo:\n" + s.title
                    + "\n(Verifica la carpeta resources/)", "Chagastify",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (engine != null) engine.stop();
        index = i;
        songList.setSelectedIndex(i);
        engine = new Engine(s.file, 0, new Runnable() {
            public void run() {
                songEnded();
            }
        });
        engine.setVolume(volume.getValue() / 100f);
        engine.start();
        nowLbl.setText("\u266B " + s.title);
        stateLbl.setText("Reproduciendo");
        if (ChagasOS.instance != null) ChagasOS.instance.taskbar.setSong("\u266A " + s.title);
        refreshList();
    }

    /** Lo llama el motor cuando la canci\u00F3n termina sola. */
    void songEnded() {
        engine = null;
        if (!songs.isEmpty()) playIndex((index + 1) % songs.size());
    }

    void stopPlayback() {
        if (engine != null) {
            engine.stop();
            engine = null;
        }
        stateLbl.setText("Detenido");
        nowLbl.setText("Nada suena todav\u00EDa...");
        progress.setValue(0);
        timeLbl.setText("0:00");
        if (ChagasOS.instance != null) ChagasOS.instance.taskbar.setSong("\u266A Chagastify detenido");
    }

    /** Adelantar/retroceder la canci\u00F3n con la barra de progreso. */
    void seekTo(int sliderValue) {
        if (index < 0 || songs.isEmpty()) return;
        Song s = songs.get(index);
        if (s.totalFrames <= 0 || s.file == null) return;
        int target = (int) ((long) sliderValue * s.totalFrames / 10000);
        if (engine != null) {
            engine.stop();
        }
        engine = new Engine(s.file, target, new Runnable() {
            public void run() {
                songEnded();
            }
        });
        engine.setVolume(volume.getValue() / 100f);
        engine.start();
        stateLbl.setText("Reproduciendo");
    }

    /** Reproduce una canci\u00F3n por nombre (la usa el Explorador de archivos). */
    void playByName(String name) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).title.equalsIgnoreCase(name)) {
                playIndex(i);
                return;
            }
        }
    }


    /** Refresca la lista con duraciones "--:--" hasta que se calculan. */
    void refreshList() {
        listModel.clear();
        for (Song s : songs) {
            String dur = s.totalFrames > 0
                    ? Res.fmtTime((long) (s.totalFrames * s.msPerFrame / 1000.0)) : "--:--";
            listModel.addElement(s.title + "  [" + dur + "]");
        }
        if (index >= 0 && index < songs.size()) songList.setSelectedIndex(index);
    }

    /** Timer de la UI: actualiza tiempo, progreso y barra de tareas. */
    void updateTick() {
        if (index >= 0 && index < songs.size()) {
            Song s = songs.get(index);
            if (s.totalFrames > 0) {
                durLbl.setText(Res.fmtTime((long) (s.totalFrames * s.msPerFrame / 1000.0)));
                if (!draggingSlider) {
                    int pos = engine != null ? engine.framePos : 0;
                    progress.setValue((int) ((long) pos * 10000 / s.totalFrames));
                    long sec = (long) (pos * engineMsPerFrame() / 1000.0);
                    timeLbl.setText(Res.fmtTime(sec));
                }
            }
        }
    }

    float engineMsPerFrame() {
        if (engine != null) return engine.msPerFrame;
        if (index >= 0 && index < songs.size()) return songs.get(index).msPerFrame;
        return 24f;
    }

    /** Calcula la duraci\u00F3n real de cada MP3 en segundo plano (es rapid\u00EDsimo). */
    void scanDurations() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (final Song s : songs) {
                    if (s.file == null || !s.file.isFile()) continue;
                    Bitstream bs = null;
                    try {
                        bs = new Bitstream(new BufferedInputStream(new FileInputStream(s.file), 1 << 16));
                        Header h = bs.readFrame();
                        if (h != null) s.msPerFrame = h.ms_per_frame();
                        int count = 0;
                        while (h != null) {
                            count++;
                            bs.closeFrame();
                            h = bs.readFrame();
                        }
                        s.totalFrames = count;
                    } catch (Throwable ignored) {
                    } finally {
                        if (bs != null) {
                            try {
                                bs.close();
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        refreshList();
                    }
                });
            }
        }, "DuracionesMP3");
        t.setDaemon(true);
        t.start();
    }

    /** Detiene la m\u00FAsica si hay un reproductor abierto (BSOD, apagado...). */
    static void stopAny() {
        if (current != null) current.stopPlayback();
    }

    @Override
    public void dispose() {
        stopPlayback();
        if (uiTimer != null) uiTimer.stop();
        if (visTimer != null) visTimer.stop();
        if (current == this) current = null;
        if (ChagasOS.instance != null) ChagasOS.instance.taskbar.setSong(" ");
        super.dispose();
    }
}

