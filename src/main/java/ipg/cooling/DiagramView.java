package ipg.cooling;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Two-column textbook figure: isothermal wall vs heat-flux from laser modules.
 */
public class DiagramView extends Pane {
    private static final Color INK = Color.web("#12324d");
    private static final Color MUTED = Color.web("#5a6b7b");
    private static final Color WATER = Color.web("#1f6f9f");
    private static final Color WATER_FILL = Color.web("#d4e7f2");
    private static final Color HEAT = Color.web("#b42318");
    private static final Color WALL = Color.web("#d9b2a8");
    private static final Color PLATE = Color.web("#c8cfd6");
    private static final Color MODULE = Color.web("#f3efe6");
    private static final Color PLOT_BG = Color.web("#f7fafc");
    private static final Color GRID = Color.web("#d5dee7");
    private static final Pattern TOKEN = Pattern.compile("([A-Za-zΑ-Ωα-ωΔλρνṁ]|Nu|Δ[PT])_([A-Za-z]+)");

    private final Canvas canvas = new Canvas();

    public DiagramView() {
        getChildren().add(canvas);
        getStyleClass().add("diagram-view");
        setMinSize(0, 0);
    }

    public void redraw() {
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(getWidth(), 1);
        double height = Math.max(getHeight(), 1);
        canvas.relocate(0, 0);
        canvas.setWidth(width);
        canvas.setHeight(height);
        paint(canvas.getGraphicsContext2D(), width, height);
    }

    private void paint(GraphicsContext g, double width, double height) {
        g.clearRect(0, 0, width, height);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.setImageSmoothing(true);

        double pad = 12;
        double gap = 16;
        double colW = (width - pad * 2 - gap) / 2.0;
        double colH = height - pad * 2;
        if (colW < 220 || colH < 360) {
            return;
        }
        drawCase(g, pad, pad, colW, colH, true);
        drawCase(g, pad + colW + gap, pad, colW, colH, false);
    }

    private void drawCase(GraphicsContext g, double x, double y, double w, double h, boolean isothermal) {
        g.setStroke(GRID);
        g.setLineWidth(1);
        g.setFill(Color.WHITE);
        roundRect(g, x, y, w, h, 8, true, true);

        double inner = 14;
        double titleY = y + 22;
        g.setFill(INK);
        g.setFont(ui(FontWeight.BOLD, 14));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(I18n.t(isothermal ? "diagram.left.title" : "diagram.right.title"), x + inner, titleY);

        double sketchTop = y + 34;
        double sketchH = h * 0.42;
        double plotTop = sketchTop + sketchH + 8;
        double captionH = 42;
        double plotH = h - (plotTop - y) - captionH - 8;

        if (isothermal) {
            drawIsothermalSketch(g, x + inner, sketchTop, w - inner * 2, sketchH);
            drawIsothermalPlot(g, x + inner, plotTop, w - inner * 2, plotH);
        } else {
            drawPlateSketch(g, x + inner, sketchTop, w - inner * 2, sketchH);
            drawPlatePlot(g, x + inner, plotTop, w - inner * 2, plotH);
        }

        g.setFill(MUTED);
        g.setFont(ui(FontWeight.NORMAL, 11));
        wrap(g, I18n.t(isothermal ? "diagram.left.caption" : "diagram.right.caption"),
                x + inner, y + h - captionH + 4, w - inner * 2, 14);
    }

    private void drawIsothermalSketch(GraphicsContext g, double x, double y, double w, double h) {
        double tubeY = y + h * 0.38;
        double tubeH = Math.min(56, h * 0.38);
        double tubeX = x + 8;
        double tubeW = w - 16;

        g.setFill(WALL);
        g.setStroke(HEAT);
        g.setLineWidth(1.6);
        roundRect(g, tubeX, tubeY - 16, tubeW, 16, 2, true, true);
        g.setFill(INK);
        g.setFont(math(13));
        fillFormula(g, "T_w = const", tubeX + 8, tubeY - 22);

        g.setFill(WATER_FILL);
        g.setStroke(WATER);
        g.setLineWidth(1.6);
        roundRect(g, tubeX, tubeY, tubeW, tubeH, 3, true, true);

        int arrows = 6;
        for (int i = 0; i < arrows; i++) {
            double t = (i + 0.5) / arrows;
            double ax = tubeX + t * tubeW;
            double len = 22 - i * 3.0;
            arrow(g, HEAT, ax, tubeY - 2, ax, tubeY + len, 1.8);
        }
        g.setFill(HEAT);
        g.setFont(math(12));
        g.fillText("Q", tubeX + tubeW * 0.12, tubeY - 4);

        flowArrow(g, tubeX + 18, tubeY + tubeH * 0.58, tubeX + tubeW - 18);
        g.setFill(WATER);
        g.setFont(ui(FontWeight.NORMAL, 11));
        g.fillText(I18n.t("diagram.water"), tubeX + tubeW * 0.42, tubeY + tubeH * 0.42);

        g.setFill(INK);
        g.setFont(math(12));
        fillFormula(g, "T_in", tubeX, tubeY + tubeH + 16);
        g.setTextAlign(TextAlignment.RIGHT);
        fillFormula(g, "T_out", tubeX + tubeW, tubeY + tubeH + 16);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawPlateSketch(GraphicsContext g, double x, double y, double w, double h) {
        double plateX = x + 10;
        double plateW = w - 20;
        double plateH = Math.min(h * 0.48, 88);
        double plateY = y + h * 0.38;

        double[] mx = {plateX + plateW * 0.18, plateX + plateW * 0.50, plateX + plateW * 0.82};
        double modW = Math.min(70, plateW * 0.20);
        double modH = 28;
        double modY = plateY - modH - 2;

        g.setFill(HEAT);
        g.setFont(math(12));
        g.fillText("Q", mx[0] - 18, modY - 16);
        for (double cx : mx) {
            arrow(g, HEAT, cx, modY - 22, cx, modY - 1, 2.0);
            g.setFill(MODULE);
            g.setStroke(INK);
            g.setLineWidth(1.4);
            roundRect(g, cx - modW / 2, modY, modW, modH, 3, true, true);
            g.setFill(INK);
            g.setFont(ui(FontWeight.NORMAL, 10));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(I18n.t("diagram.module"), cx, modY + 18);
        }
        g.setTextAlign(TextAlignment.LEFT);

        g.setFill(PLATE);
        g.setStroke(INK);
        g.setLineWidth(1.5);
        roundRect(g, plateX, plateY, plateW, plateH, 4, true, true);
        g.setFill(MUTED);
        g.setFont(ui(FontWeight.NORMAL, 10));
        g.fillText(I18n.t("diagram.plate"), plateX + 8, plateY + 14);

        double tubeY = plateY + plateH * 0.58;
        double inset = 18;
        g.setStroke(WATER);
        g.setLineWidth(3.2);
        g.beginPath();
        g.moveTo(plateX + inset, tubeY - 16);
        g.lineTo(plateX + plateW - inset, tubeY - 16);
        g.lineTo(plateX + plateW - inset, tubeY);
        g.lineTo(plateX + inset, tubeY);
        g.lineTo(plateX + inset, tubeY + 16);
        g.lineTo(plateX + plateW - inset, tubeY + 16);
        g.stroke();

        g.setFill(WATER);
        g.setFont(ui(FontWeight.NORMAL, 10));
        g.fillText(I18n.t("diagram.inlet"), plateX + inset, plateY + plateH - 8);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText(I18n.t("diagram.outlet"), plateX + plateW - inset, plateY + plateH - 8);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawIsothermalPlot(GraphicsContext g, double x, double y, double w, double h) {
        PlotBox box = axes(g, x, y, w, h);
        g.setStroke(HEAT);
        g.setLineWidth(2);
        g.strokeLine(box.x0, box.yOf(0.82), box.x1, box.yOf(0.82));
        g.setStroke(WATER);
        g.beginPath();
        for (int i = 0; i <= 40; i++) {
            double t = i / 40.0;
            double temp = 0.18 + 0.64 * (1.0 - Math.exp(-3.2 * t));
            if (i == 0) {
                g.moveTo(box.xOf(t), box.yOf(temp));
            } else {
                g.lineTo(box.xOf(t), box.yOf(temp));
            }
        }
        g.stroke();
        g.setFill(HEAT);
        g.setFont(math(12));
        fillFormula(g, "T_w", box.x1 - 28, box.yOf(0.82) - 6);
        g.setFill(WATER);
        fillFormula(g, "T_b", box.x1 - 36, box.yOf(0.72) + 14);
    }

    private void drawPlatePlot(GraphicsContext g, double x, double y, double w, double h) {
        PlotBox box = axes(g, x, y, w, h);
        g.setStroke(WATER);
        g.setLineWidth(2);
        g.strokeLine(box.xOf(0), box.yOf(0.18), box.xOf(1), box.yOf(0.62));
        g.setStroke(HEAT);
        g.beginPath();
        for (int i = 0; i <= 60; i++) {
            double t = i / 60.0;
            double water = 0.18 + 0.44 * t;
            double humps = 0.16 * (bump(t, 0.22) + bump(t, 0.50) + bump(t, 0.78));
            double temp = water + 0.14 + humps;
            if (i == 0) {
                g.moveTo(box.xOf(t), box.yOf(temp));
            } else {
                g.lineTo(box.xOf(t), box.yOf(temp));
            }
        }
        g.stroke();
        g.setFill(HEAT);
        g.setFont(math(12));
        fillFormula(g, "T_Al", box.x1 - 32, box.yOf(0.86) - 2);
        g.setFill(WATER);
        fillFormula(g, "T_b", box.x1 - 28, box.yOf(0.62) + 14);
    }

    private static double bump(double x, double center) {
        double u = (x - center) / 0.07;
        return Math.exp(-u * u);
    }

    private PlotBox axes(GraphicsContext g, double x, double y, double w, double h) {
        double left = x + 28;
        double right = x + w - 8;
        double top = y + 8;
        double bottom = y + h - 22;
        g.setFill(PLOT_BG);
        g.setStroke(GRID);
        g.setLineWidth(1);
        g.fillRect(left, top, right - left, bottom - top);
        g.strokeRect(left, top, right - left, bottom - top);
        g.setStroke(INK);
        g.setLineWidth(1.2);
        g.strokeLine(left, bottom, right, bottom);
        g.strokeLine(left, top, left, bottom);
        g.setFill(MUTED);
        g.setFont(math(11));
        g.fillText("T", x + 6, top + 10);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("x", right, bottom + 16);
        g.setTextAlign(TextAlignment.LEFT);
        return new PlotBox(left, right, top, bottom);
    }

    private record PlotBox(double x0, double x1, double y0, double y1) {
        double xOf(double t) {
            return x0 + t * (x1 - x0);
        }

        double yOf(double t) {
            return y1 - t * (y1 - y0);
        }
    }

    private static void arrow(GraphicsContext g, Color color, double x1, double y1, double x2, double y2, double width) {
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(width);
        g.strokeLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double s = 6;
        g.beginPath();
        g.moveTo(x2, y2);
        g.lineTo(x2 - s * Math.cos(angle - 0.45), y2 - s * Math.sin(angle - 0.45));
        g.lineTo(x2 - s * Math.cos(angle + 0.45), y2 - s * Math.sin(angle + 0.45));
        g.closePath();
        g.fill();
    }

    private static void flowArrow(GraphicsContext g, double x1, double y, double x2) {
        arrow(g, WATER, x1, y, x2, y, 1.8);
    }

    private static void roundRect(
            GraphicsContext g, double x, double y, double w, double h, double r, boolean fill, boolean stroke
    ) {
        if (fill) {
            g.fillRoundRect(x, y, w, h, r * 2, r * 2);
        }
        if (stroke) {
            g.strokeRoundRect(x, y, w, h, r * 2, r * 2);
        }
    }

    private static void wrap(GraphicsContext g, String text, double x, double y, double maxW, double lineH) {
        String[] words = text.split(" ");
        double cx = x;
        double cy = y + lineH;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String gap = i == 0 || cx == x ? "" : " ";
            double wordW = measureFormula(g, gap + word);
            if (cx > x && cx + wordW > x + maxW) {
                cx = x;
                cy += lineH;
                gap = "";
                wordW = measureFormula(g, word);
            }
            fillFormula(g, gap + word, cx, cy);
            cx += wordW;
        }
    }

    private static void fillFormula(GraphicsContext g, String source, double x, double y) {
        if (source == null || source.isEmpty()) {
            return;
        }
        double size = g.getFont().getSize();
        Font current = g.getFont();
        Font letter = math(size);
        Font sub = Font.font("Times New Roman", FontWeight.NORMAL, FontPosture.REGULAR, size * 0.72);
        double width = measureFormula(g, source, letter, sub, current);
        double cursor = switch (g.getTextAlign()) {
            case RIGHT -> x - width;
            case CENTER -> x - width / 2.0;
            default -> x;
        };
        TextAlignment align = g.getTextAlign();
        g.setTextAlign(TextAlignment.LEFT);
        Matcher matcher = TOKEN.matcher(source);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                g.setFont(current);
                String bit = source.substring(last, matcher.start());
                g.fillText(bit, cursor, y);
                cursor += textWidth(g, bit);
            }
            g.setFont(letter);
            String name = matcher.group(1);
            g.fillText(name, cursor, y);
            cursor += textWidth(g, name);
            g.setFont(sub);
            String index = matcher.group(2);
            g.fillText(index, cursor, y + size * 0.22);
            cursor += textWidth(g, index);
            last = matcher.end();
        }
        if (last < source.length()) {
            g.setFont(current);
            g.fillText(source.substring(last), cursor, y);
        }
        g.setFont(current);
        g.setTextAlign(align);
    }

    private static double measureFormula(GraphicsContext g, String source) {
        double size = g.getFont().getSize();
        return measureFormula(g, source, math(size),
                Font.font("Times New Roman", FontWeight.NORMAL, FontPosture.REGULAR, size * 0.72),
                g.getFont());
    }

    private static double measureFormula(GraphicsContext g, String source, Font letter, Font sub, Font plain) {
        if (source == null || source.isEmpty()) {
            return 0;
        }
        Font previous = g.getFont();
        double width = 0;
        Matcher matcher = TOKEN.matcher(source);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                g.setFont(plain);
                width += textWidth(g, source.substring(last, matcher.start()));
            }
            g.setFont(letter);
            width += textWidth(g, matcher.group(1));
            g.setFont(sub);
            width += textWidth(g, matcher.group(2));
            last = matcher.end();
        }
        if (last < source.length()) {
            g.setFont(plain);
            width += textWidth(g, source.substring(last));
        }
        g.setFont(previous);
        return width;
    }

    private static double textWidth(GraphicsContext g, String text) {
        javafx.scene.text.Text probe = new javafx.scene.text.Text(text);
        probe.setFont(g.getFont());
        return probe.getLayoutBounds().getWidth();
    }

    private static Font ui(FontWeight weight, double size) {
        return Font.font("System", weight, size);
    }

    private static Font math(double size) {
        return Font.font("Times New Roman", FontWeight.NORMAL, FontPosture.ITALIC, size);
    }
}
