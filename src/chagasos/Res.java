package chagasos;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga los recursos del proyecto (foto de perfil "profile.png" y los MP3)
 * y dibuja los íconos del sistema con Java2D (sin depender de fuentes raras).
 */
public final class Res {

    private static final String[] SEARCH_DIRS = {"resources", ".", ".."};
    private static BufferedImage profile;
    private static final Map<Integer, ImageIcon> AVATARS = new HashMap<Integer, ImageIcon>();
    private static final Map<String, ImageIcon> ICONS = new HashMap<String, ImageIcon>();

    private Res() {
    }

    /* ------------------------- recursos en disco ------------------------- */

    /** Busca un archivo primero en resources/, luego en la raíz del proyecto. */
    public static File find(String name) {
        for (String d : SEARCH_DIRS) {
            File f = new File(d, name);
            if (f.isFile()) return f;
        }
        return null;
    }

    /** Foto de perfil del usuario "Chagas" (se carga una sola vez). */
    public static synchronized BufferedImage profile() {
        if (profile == null) {
            BufferedImage img = null;
            try {
                File f = find("profile.png");
                if (f != null) img = ImageIO.read(f);
            } catch (Exception ignored) {
            }
            if (img == null) img = fallbackAvatar();
            profile = img;
        }
        return profile;
    }

    /** Avatar de emergencia si por alguna razón no existe la foto. */
    private static BufferedImage fallbackAvatar() {
        BufferedImage img = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 150, 136));
        g.fillRect(0, 0, 96, 96);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        g.drawString("C", 32, 62);
        g.dispose();
        return img;
    }

    /* ------------------------- utilidades de imagen ------------------------- */

    /** Escala una imagen a un cuadrado de "size" recortando al centro. */
    public static BufferedImage scale(Image src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = src.getWidth(null), h = src.getHeight(null);
        int s = Math.min(w, h);
        g.drawImage(src, 0, 0, size, size, (w - s) / 2, (h - s) / 2, (w + s) / 2, (h + s) / 2, null);
        g.dispose();
        return out;
    }

    /** Escala una imagen al ancho/alto indicado conservando la proporción. */
    public static BufferedImage scaleFit(Image src, int w, int h) {
        double ratio = Math.min((double) w / src.getWidth(null), (double) h / src.getHeight(null));
        int iw = Math.max(1, (int) Math.round(src.getWidth(null) * ratio));
        int ih = Math.max(1, (int) Math.round(src.getHeight(null) * ratio));
        BufferedImage out = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, iw, ih, null);
        g.dispose();
        return out;
    }

    /** Avatar circular con aro de acento (foto de perfil del Chagas). */
    public static BufferedImage round(Image src, int size) {
        BufferedImage scaled = scale(src, size);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ellipse2D.Float circle = new Ellipse2D.Float(0, 0, size, size);
        g.setClip(circle);
        g.drawImage(scaled, 0, 0, null);
        g.setClip(null);
        g.setStroke(new BasicStroke(Math.max(3f, size * 0.06f)));
        g.setColor(ChagasOS.accent());
        g.drawOval(0, 0, size - 1, size - 1);
        g.setStroke(new BasicStroke(Math.max(1f, size * 0.02f)));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawOval((int) (size * 0.07), (int) (size * 0.07), (int) (size * 0.86), (int) (size * 0.86));
        g.dispose();
        return out;
    }

    /** Ícono circular de la foto de perfil (con caché). */
    public static ImageIcon roundAvatar(int size) {
        ImageIcon cached = AVATARS.get(size);
        if (cached == null) {
            cached = new ImageIcon(round(profile(), size));
            AVATARS.put(size, cached);
        }
        return cached;
    }

    /* ------------------------- wallpaper pre-escalado ------------------------- */

    private static BufferedImage wallpaper;

    /**
     * Versi\u00F3n pre-escalada (m\u00E1x. 1024) de la foto de perfil para pintar
     * el fondo de pantalla. Escalar esta imagen en cada repaint es r\u00E1pido;
     * escalar la original de 2760x2760 congelaba la ventana al maximizar.
     */
    public static synchronized BufferedImage wallpaper() {
        if (wallpaper == null) {
            BufferedImage src = profile();
            int w = src.getWidth(), h = src.getHeight();
            double s = Math.min(1.0, 1024.0 / Math.max(w, h));
            int iw = Math.max(1, (int) Math.round(w * s));
            int ih = Math.max(1, (int) Math.round(h * s));
            BufferedImage out = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, iw, ih, null);
            g.dispose();
            wallpaper = out;
        }
        return wallpaper;
    }

    /**
     * Ícono de aplicación dibujado a mano: cuadrado redondeado de color
     * + símbolo/figura. Tipos: music, terminal, folder/explorer, notepad/txt,
     * tasks, calc, settings, photos/png, exe/dll/sys, bsod, about.
     */
    public static ImageIcon appIcon(String kind, Color bg, int size) {
        String key = kind + "@" + size + "#" + bg.getRGB();
        ImageIcon cached = ICONS.get(key);
        if (cached != null) return cached;

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float r = size * 0.22f;
        RoundRectangle2D.Float rr = new RoundRectangle2D.Float(0.5f, 0.5f, size - 1, size - 1, r * 2, r * 2);
        g.setColor(bg);
        g.fill(rr);
        g.setStroke(new BasicStroke(Math.max(1f, size * 0.02f)));
        g.setColor(mix(bg, Color.BLACK, 0.35f));
        g.draw(rr);

        float in = size * 0.16f;
        float s = size - 2 * in;
        if ("music".equals(kind)) {
            centerText(g, "\u266A", bold(s * 0.55f), size);                       // ♪
        } else if ("terminal".equals(kind)) {
            g.setColor(mix(bg, Color.BLACK, 0.55f));
            g.fillRoundRect((int) in, (int) in, (int) s, (int) s, (int) (size * 0.08f), (int) (size * 0.08f));
            centerText(g, ">_", bold(s * 0.34f), size);
        } else if ("folder".equals(kind) || "explorer".equals(kind)) {
            folder(g, in, s, size, bg);
        } else if ("notepad".equals(kind) || "txt".equals(kind)) {
            centerText(g, "\u00B6", bold(s * 0.5f), size);                       // ¶
        } else if ("tasks".equals(kind)) {
            centerText(g, "\u2261", bold(s * 0.45f), size);                       // ≡
        } else if ("calc".equals(kind)) {
            centerText(g, "\u00B1", bold(s * 0.5f), size);                        // ±
        } else if ("settings".equals(kind)) {
            gear(g, size / 2f, size / 2f, s * 0.45f);
        } else if ("photos".equals(kind) || "png".equals(kind)) {
            photo(g, in, s, size, bg);
        } else if ("exe".equals(kind) || "dll".equals(kind) || "sys".equals(kind)) {
            centerText(g, "\u25A0", bold(s * 0.4f), size);                         // ■
        } else if ("bsod".equals(kind)) {
            centerText(g, ":(", bold(s * 0.34f), size);
        } else {
            centerText(g, "i", bold(s * 0.55f), size);
        }
        g.dispose();
        ImageIcon ic = new ImageIcon(img);
        ICONS.put(key, ic);
        return ic;
    }

    /* ------------------------- figuras dibujadas ------------------------- */

    private static void centerText(Graphics2D g, String txt, Font f, int size) {
        g.setFont(f);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int x = (size - fm.stringWidth(txt)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(txt, Math.max(0, x), Math.max(f.getSize(), y));
    }

    private static Font bold(float size) {
        return new Font(Font.SANS_SERIF, Font.BOLD, Math.max(6, Math.round(size)));
    }

    private static void folder(Graphics2D g, float in, float s, float size, Color bg) {
        g.setColor(mix(bg, Color.WHITE, 0.18f));
        g.fillRoundRect((int) in, (int) (in + s * 0.06f), (int) (s * 0.44f), (int) (s * 0.30f), (int) (size * 0.06f), (int) (size * 0.06f));
        g.setColor(mix(bg, Color.WHITE, 0.30f));
        g.fillRoundRect((int) in, (int) (in + s * 0.24f), (int) s, (int) (s * 0.62f), (int) (size * 0.08f), (int) (size * 0.08f));
    }

    private static void gear(Graphics2D g, float cx, float cy, float r) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(r * 0.22f));
        g.draw(new Ellipse2D.Float(cx - r * 0.72f, cy - r * 0.72f, r * 1.44f, r * 1.44f));
        g.setStroke(new BasicStroke(r * 0.36f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(i * 60);
            g.drawLine(Math.round(cx), Math.round(cy),
                    Math.round(cx + (float) Math.cos(ang) * r),
                    Math.round(cy + (float) Math.sin(ang) * r));
        }
        g.fill(new Ellipse2D.Float(cx - r * 0.20f, cy - r * 0.20f, r * 0.40f, r * 0.40f));
    }

    private static void photo(Graphics2D g, float in, float s, float size, Color bg) {
        g.setColor(mix(bg, Color.BLACK, 0.45f));
        g.fillRoundRect((int) in, (int) in, (int) s, (int) s, (int) (size * 0.08f), (int) (size * 0.08f));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1f, size * 0.03f)));
        g.drawRoundRect((int) in, (int) in, (int) s, (int) s, (int) (size * 0.08f), (int) (size * 0.08f));
        g.setColor(new Color(255, 214, 10));
        g.fill(new Ellipse2D.Float(in + s * 0.58f, in + s * 0.12f, s * 0.20f, s * 0.20f));
        g.setColor(new Color(215, 220, 228));
        g.fillPolygon(new int[]{(int) (in + s * 0.06f), (int) (in + s * 0.45f), (int) (in + s * 0.62f), (int) (in + s * 0.92f)},
                new int[]{(int) (in + s * 0.90f), (int) (in + s * 0.42f), (int) (in + s * 0.72f), (int) (in + s * 0.32f)}, 4);
        g.setColor(new Color(180, 188, 196));
        g.fillPolygon(new int[]{(int) (in + s * 0.55f), (int) (in + s * 0.95f), (int) (in + s * 0.95f)},
                new int[]{(int) (in + s * 0.95f), (int) (in + s * 0.55f), (int) (in + s * 0.95f)}, 3);
    }

    /* ------------------------- utilidades generales ------------------------- */

    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));   // clamp: nunca fuera de rango (evita IllegalArgumentException)
        float[] ca = a.getRGBColorComponents(null);
        float[] cb = b.getRGBColorComponents(null);
        return new Color(ca[0] + (cb[0] - ca[0]) * t, ca[1] + (cb[1] - ca[1]) * t, ca[2] + (cb[2] - ca[2]) * t);
    }

    /** 75 -> "1:15" */
    public static String fmtTime(long totalSeconds) {
        long m = totalSeconds / 60, s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }
}


