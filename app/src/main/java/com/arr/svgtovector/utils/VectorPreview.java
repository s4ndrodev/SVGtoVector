package com.arr.svgtovector.utils;

import android.graphics.*;
import androidx.core.graphics.PathParser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class VectorPreview {

    public static Bitmap render(String xml, int size) throws Exception {
        Document doc =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();

        float vw = Float.parseFloat(root.getAttribute("android:viewportWidth"));
        float vh = Float.parseFloat(root.getAttribute("android:viewportHeight"));

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        float scale = Math.min(size / vw, size / vh);
        canvas.translate((size - vw * scale) / 2f, (size - vh * scale) / 2f);
        canvas.scale(scale, scale);

        NodeList paths = doc.getElementsByTagName("path");
        for (int i = 0; i < paths.getLength(); i++) {
            Element e = (Element) paths.item(i);
            Path path = PathParser.createPathFromPathData(e.getAttribute("android:pathData"));
            if (path == null) continue;

            if ("evenOdd".equals(e.getAttribute("android:fillType"))) {
                path.setFillType(Path.FillType.EVEN_ODD);
            }

            String fill = e.getAttribute("android:fillColor");
            if (!fill.isEmpty()) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.FILL);
                p.setColor(parseColor(fill));
                canvas.drawPath(path, p);
            }

            String stroke = e.getAttribute("android:strokeColor");
            if (!stroke.isEmpty()) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(parseColor(stroke));
                String w = e.getAttribute("android:strokeWidth");
                p.setStrokeWidth(w.isEmpty() ? 1f : Float.parseFloat(w));
                switch (e.getAttribute("android:strokeLineCap")) {
                    case "round":
                        p.setStrokeCap(Paint.Cap.ROUND);
                        break;
                    case "square":
                        p.setStrokeCap(Paint.Cap.SQUARE);
                        break;
                }
                switch (e.getAttribute("android:strokeLineJoin")) {
                    case "round":
                        p.setStrokeJoin(Paint.Join.ROUND);
                        break;
                    case "bevel":
                        p.setStrokeJoin(Paint.Join.BEVEL);
                        break;
                }
                canvas.drawPath(path, p);
            }
        }
        return bmp;
    }

    // El blanco por defecto se dibuja oscuro para que se vea sobre el fondo claro
    private static int parseColor(String c) {
        if (c.startsWith("@android:color/white")) return 0xFF222222;
        try {
            return Color.parseColor(c);
        } catch (Exception e) {
            return 0xFF222222;
        }
    }
}
