package chagasos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Calculadora Chagas: calculadora normal y funcional... con error oficial
 * "CHAGAS ERROR" en vez de mensajes aburridos.
 */
public class CalculatorApp extends JInternalFrame {

    static final String OP_ADD = "+";
    static final String OP_SUB = "\u2212";   // −
    static final String OP_MUL = "\u00D7";   // ×
    static final String OP_DIV = "\u00F7";   // ÷

    final JTextField display = new JTextField("0");
    String current = "0";
    double acc = 0;
    String op = "";
    boolean fresh = true;   // el pr\u00F3ximo d\u00EDgito empieza de cero
    boolean error = false;

    public CalculatorApp() {
        super("Calculadora Chagas", true, true, true, true);
        setFrameIcon(ChagasOS.APPS.get("calc").icon(16));
        setSize(300, 400);
        setContentPane(buildUI());
    }

    JComponent buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(ChagasOS.BG_PANEL);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        display.setBackground(ChagasOS.BG_DEEP);
        display.setForeground(ChagasOS.TEXT_MAIN);
        display.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ChagasOS.BG_CONTROL, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        root.add(display, BorderLayout.NORTH);

        String[][] keys = {
                {"C", "\u2190", "%", OP_DIV},
                {"7", "8", "9", OP_MUL},
                {"4", "5", "6", OP_SUB},
                {"1", "2", "3", OP_ADD},
                {"\u00B1", "0", ".", "="}
        };
        JPanel grid = new JPanel(new GridLayout(5, 4, 6, 6));
        grid.setOpaque(false);
        for (String[] row : keys) {
            for (final String k : row) {
                JButton b = new JButton(k);
                b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                if (k.equals("=")) {
                    b.setBackground(ChagasOS.accent());
                    b.setForeground(Color.WHITE);
                } else if (Character.isDigit(k.charAt(0)) || k.equals(".")) {
                    b.setBackground(ChagasOS.BG_CONTROL);
                    b.setForeground(ChagasOS.TEXT_MAIN);
                } else {
                    b.setBackground(Res.mix(ChagasOS.accent(), ChagasOS.BG_PANEL, 0.55f));
                    b.setForeground(Color.WHITE);
                }
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        press(k);
                    }
                });
                grid.add(b);
            }
        }
        root.add(grid, BorderLayout.CENTER);
        return root;
    }

    void press(String k) {
        if (error && !k.equals("C")) {
            return; // tras un CHAGAS ERROR solo queda reiniciar con C
        }
        if (k.equals("C")) {
            current = "0";
            acc = 0;
            op = "";
            fresh = true;
            error = false;
        } else if (k.equals("\u2190")) { // borrar \u00FAltimo d\u00EDgito
            current = current.length() > 1 ? current.substring(0, current.length() - 1) : "0";
            fresh = false;
        } else if (k.equals("\u00B1")) {
            if (current.startsWith("-")) current = current.substring(1);
            else if (!current.equals("0")) current = "-" + current;
        } else if (k.equals(".")) {
            if (fresh) {
                current = "0.";
                fresh = false;
            } else if (!current.contains(".")) {
                current = current + ".";
            }
        } else if (Character.isDigit(k.charAt(0))) {
            if (fresh) {
                current = k;
                fresh = false;
            } else if (current.replace("-", "").replace(".", "").length() < 12) {
                current = current + k;
            }
        } else if (k.equals("%")) {
            current = fmt(parse(current) / 100.0);
            fresh = true;
        } else if (k.equals("=")) {
            compute();
        } else { // operador
            if (!op.isEmpty()) compute();
            acc = parse(current);
            op = k;
            fresh = true;
        }
        refreshDisplay();
    }

    void compute() {
        if (op.isEmpty()) {
            acc = parse(current);
            return;
        }
        double v = parse(current);
        double r;
        if (OP_ADD.equals(op)) r = acc + v;
        else if (OP_SUB.equals(op)) r = acc - v;
        else if (OP_MUL.equals(op)) r = acc * v;
        else if (OP_DIV.equals(op)) {
            if (v == 0) {
                error = true;
                display.setText("CHAGAS ERROR");
                current = "0";
                acc = 0;
                op = "";
                fresh = true;
                return;
            }
            r = acc / v;
        } else {
            r = v;
        }
        acc = r;
        op = "";
        current = fmt(r);
        fresh = true;
    }

    double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9 && Math.abs(v) < 1e12) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.6g", v);
    }

    void refreshDisplay() {
        if (error) return;
        display.setText(current);
    }
}
