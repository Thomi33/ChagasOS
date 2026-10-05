package chagasos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

/**
 * Administrador de Tareas del Chagas: tabla con procesos "reales" del
 * sistema, uso animado de CPU y RAM, y un botón que no deja matar nada
 * (solo el Chagas puede).
 */
public class TaskManagerApp extends JInternalFrame {

    final String[][] procs = {
            {"chagas_core.exe", "1024", "14", "890", "El kernel de la divisi\u00F3n"},
            {"ego.dll", "2048", "31", "6200", "Amplificador de ego"},
            {"himno_nacional.mp3", "4096", "3", "310", "Sonando de fondo (obligatorio)"},
            {"conta_billetes.exe", "1337", "22", "420", "Ministerio de finanzas"},
            {"patrulla.exe", "8080", "18", "256", "Patrulla del sistema"},
            {"agua_pipa.dll", "6969", "2", "128", "Purificadora de agua"},
            {"dictador_service.dll", "1984", "26", "512", "Servicio del r\u00E9gimen"},
            {"fan_corp.exe", "2718", "9", "384", "Cooler del Chagas"}
    };
    DefaultTableModel model;
    JProgressBar cpuBar = new JProgressBar(0, 100);
    JProgressBar ramBar = new JProgressBar(0, 100);
    javax.swing.Timer timer;

    public TaskManagerApp() {
        super("Admin. de Tareas", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("tasks").icon(16));
        setSize(620, 400);
        setContentPane(buildUI());
        timer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        timer.start();
    }

    JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // barras de uso total
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 6));
        top.setOpaque(false);
        top.add(barRow("CPU", cpuBar));
        top.add(barRow("RAM", ramBar));
        root.add(top, BorderLayout.NORTH);

        // tabla de procesos
        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        model.setColumnIdentifiers(new Object[]{"Proceso", "PID", "CPU %", "RAM MB", "Descripci\u00F3n"});
        for (String[] p : procs) model.addRow(p);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.setBackground(ChagasOS.BG_DEEP);
        table.setForeground(ChagasOS.TEXT_MAIN);
        table.setSelectionBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_CONTROL, 0.4f));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(ChagasOS.BG_CONTROL);
        table.getTableHeader().setBackground(ChagasOS.BG_CONTROL);
        table.getTableHeader().setForeground(ChagasOS.TEXT_MAIN);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ChagasOS.BG_CONTROL));
        root.add(scroll, BorderLayout.CENTER);

        // botones de abajo — ALEJADOS de la esquina inferior-derecha: esa zona
        // es la de agarre para redimensionar la ventana y no debe disparar
        // botones por accidente (bug: redimensionar provocaba el BSOD).
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(4, 8, 16, 16));
        JButton endBtn = new JButton("Finalizar tarea");
        endBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(TaskManagerApp.this,
                        "ACCESO DENEGADO.\nSolo el Chagas puede finalizar procesos del sistema.\n"
                                + "(y \u00E9l nunca lo har\u00EDa)",
                        "Admin. de Tareas", JOptionPane.WARNING_MESSAGE);
            }
        });
        JButton crashBtn = new JButton("Provocar ca\u00EDda del sistema");
        crashBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // confirmaci\u00F3n expl\u00EDcita: la pantalla azul solo si de verdad la quieres
                int r = JOptionPane.showConfirmDialog(TaskManagerApp.this,
                        "\u00BFTumbar ChagasOS de verdad?\n(La pantalla azul solo deber\u00EDa salir si T\u00DA la pides)",
                        "Provocar ca\u00EDda del sistema",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r == JOptionPane.YES_OPTION) {
                    ChagasOS.instance.blueScreen();
                }
            }
        });
        btns.add(endBtn);
        btns.add(crashBtn);
        root.add(btns, BorderLayout.SOUTH);
        return root;
    }

    JPanel barRow(String label, JProgressBar bar) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label + ":");
        l.setForeground(ChagasOS.TEXT_MAIN);
        l.setPreferredSize(new Dimension(44, 18));
        p.add(l, BorderLayout.WEST);
        bar.setForeground(ChagasOS.accent());
        bar.setBackground(ChagasOS.BG_DEEP);
        bar.setStringPainted(true);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    /** Random walk de los procesos, cada segundo. */
    void tick() {
        Random r = new Random();
        double cpuTotal = 0, ramTotal = 0;
        for (int i = 0; i < procs.length; i++) {
            double cpu = clamp(Math.round(Double.parseDouble((String) model.getValueAt(i, 2))
                    + (r.nextDouble() * 10 - 5)));
            double ram = clamp(Math.round(Double.parseDouble((String) model.getValueAt(i, 3))
                    + (r.nextDouble() * 40 - 20)));
            model.setValueAt(String.valueOf((long) cpu), i, 2);
            model.setValueAt(String.valueOf((long) ram), i, 3);
            cpuTotal += cpu;
            ramTotal += ram;
        }
        cpuBar.setValue((int) clamp(cpuTotal / procs.length));
        ramBar.setValue((int) clamp(ramTotal / procs.length * 1.6));
        cpuBar.setString(cpuBar.getValue() + "% (ChagIntel \u00ABFandango\u00BB)");
        ramBar.setString(ramBar.getValue() + "% de 8 GB (reales: 2)");
    }

    double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    @Override
    public void dispose() {
        if (timer != null) timer.stop();
        super.dispose();
    }
}
