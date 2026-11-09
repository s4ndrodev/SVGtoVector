package com.arr.svgtovector.utils;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Locale;

public class SvgConverter {

    public static String toVectorDrawable(InputStream in, String tint) throws Exception {
        if (tint == null || tint.trim().isEmpty()) {
            tint = "?attr/colorControlNormal";
        }

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        Document doc = f.newDocumentBuilder().parse(in);
        Element svg = doc.getDocumentElement();

        float vw = 24, vh = 24;
        String vb = svg.getAttribute("viewBox");
        if (!vb.isEmpty()) {
            String[] p = vb.trim().split("[\\s,]+");
            if (p.length == 4) {
                vw = Float.parseFloat(p[2]);
                vh = Float.parseFloat(p[3]);
            }
        } else {
            vw = parseSize(svg.getAttribute("width"), 24);
            vh = parseSize(svg.getAttribute("height"), 24);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!--XML Created by SVGtoVector-->\n")
                .append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
                .append("    android:width=\"24dp\"\n")
                .append("    android:height=\"24dp\"\n")
                .append("    android:viewportWidth=\"")
                .append(num(vw))
                .append("\"\n")
                .append("    android:viewportHeight=\"")
                .append(num(vh))
                .append("\"\n");
        sb.append("    android:tint=\"").append(escapeAttr(tint.trim())).append("\">\n");

        walk(svg, sb);
        sb.append("</vector>");
        return sb.toString();
    }

    private static String escapeAttr(String s) {
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static void walk(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element) n;
            String d = null;

            switch (e.getTagName()) {
                case "path":
                    d = e.getAttribute("d");
                    break;
                case "rect":
                    {
                        float x = f(e, "x"), y = f(e, "y"), w = f(e, "width"), h = f(e, "height");
                        float rx = f(e, "rx"), ry = f(e, "ry");
                        if (rx == 0) rx = ry;
                        if (ry == 0) ry = rx;
                        rx = Math.min(rx, w / 2);
                        ry = Math.min(ry, h / 2);
                        if (rx > 0) {
                            d =
                                    String.format(
                                            Locale.US,
                                            "M%s,%sH%sA%s,%s 0 0 1 %s,%sV%sA%s,%s 0 0 1 %s,%sH%sA%s,%s 0 0 1 %s,%sV%sA%s,%s 0 0 1 %s,%sz",
                                            num(x + rx),
                                            num(y),
                                            num(x + w - rx),
                                            num(rx),
                                            num(ry),
                                            num(x + w),
                                            num(y + ry),
                                            num(y + h - ry),
                                            num(rx),
                                            num(ry),
                                            num(x + w - rx),
                                            num(y + h),
                                            num(x + rx),
                                            num(rx),
                                            num(ry),
                                            num(x),
                                            num(y + h - ry),
                                            num(y + ry),
                                            num(rx),
                                            num(ry),
                                            num(x + rx),
                                            num(y));
                        } else {
                            d =
                                    String.format(
                                            Locale.US,
                                            "M%s,%sh%sv%sh-%sz",
                                            num(x),
                                            num(y),
                                            num(w),
                                            num(h),
                                            num(w));
                        }
                        break;
                    }
                case "circle":
                    {
                        float cx = f(e, "cx"), cy = f(e, "cy"), r = f(e, "r");
                        d = ellipse(cx, cy, r, r);
                        break;
                    }
                case "ellipse":
                    d = ellipse(f(e, "cx"), f(e, "cy"), f(e, "rx"), f(e, "ry"));
                    break;
                case "line":
                    d =
                            String.format(
                                    Locale.US,
                                    "M%s,%sL%s,%s",
                                    num(f(e, "x1")),
                                    num(f(e, "y1")),
                                    num(f(e, "x2")),
                                    num(f(e, "y2")));
                    break;
                case "polygon":
                    d = "M" + e.getAttribute("points").trim() + "z";
                    break;
                case "polyline":
                    d = "M" + e.getAttribute("points").trim();
                    break;
                default:
                    walk(e, sb); // g, defs, etc.
                    continue;
            }

            if (d != null && !d.isEmpty()) appendPath(sb, e, d);
        }
    }

    private static int numEnd(String s, int i) {
        int n = s.length();
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
        while (i < n && Character.isDigit(s.charAt(i))) i++;
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && Character.isDigit(s.charAt(i))) i++;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) j++;
            if (j < n && Character.isDigit(s.charAt(j))) {
                while (j < n && Character.isDigit(s.charAt(j))) j++;
                i = j;
            }
        }
        return i;
    }

    // Separa los flags de los arcos y los números pegados ("012-2" -> "0 1 2 -2")
    private static String normalizePath(String d) {
        StringBuilder out = new StringBuilder();
        int i = 0, n = d.length(), argIdx = 0;
        char cmd = 0;
        while (i < n) {
            char c = d.charAt(i);
            if (Character.isLetter(c)) {
                cmd = c;
                argIdx = 0;
                out.append(c);
                i++;
            } else if (Character.isWhitespace(c) || c == ',') {
                i++;
            } else {
                boolean isArc = (cmd == 'a' || cmd == 'A');
                int end;
                if (isArc && (argIdx % 7 == 3 || argIdx % 7 == 4)) {
                    end = i + 1; // los flags ocupan un solo carácter
                } else {
                    end = numEnd(d, i);
                }
                if (end <= i) {
                    i++;
                    continue;
                }
                if (out.length() > 0 && !Character.isLetter(out.charAt(out.length() - 1))) {
                    out.append(' ');
                }
                out.append(d, i, end);
                i = end;
                argIdx++;
            }
        }
        return out.toString();
    }

    // Lee el atributo del elemento, de su style, o lo hereda de los padres
    private static String attr(Element e, String name) {
        Node cur = e;
        while (cur instanceof Element) {
            Element el = (Element) cur;
            for (String part : el.getAttribute("style").split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2 && kv[0].trim().equals(name)) return kv[1].trim();
            }
            String v = el.getAttribute(name);
            if (!v.isEmpty()) return v;
            cur = cur.getParentNode();
        }
        return "";
    }

    private static void appendPath(StringBuilder sb, Element e, String d) {
        String fill = attr(e, "fill");
        String stroke = attr(e, "stroke");
        boolean hasFill = !"none".equals(fill);
        boolean hasStroke = !stroke.isEmpty() && !"none".equals(stroke);

        sb.append("  <path\n");

        if (hasFill) {
            sb.append("      android:fillColor=\"")
                    .append(color(fill, "@android:color/white"))
                    .append("\"\n");
        }

        if (hasStroke) {
            sb.append("      android:strokeColor=\"")
                    .append(color(stroke, "@android:color/white"))
                    .append("\"\n");

            String sw = attr(e, "stroke-width");
            sb.append("      android:strokeWidth=\"")
                    .append(sw.isEmpty() ? "1" : sw)
                    .append("\"\n");

            String cap = attr(e, "stroke-linecap");
            if (!cap.isEmpty())
                sb.append("      android:strokeLineCap=\"").append(cap).append("\"\n");

            String join = attr(e, "stroke-linejoin");
            if (!join.isEmpty())
                sb.append("      android:strokeLineJoin=\"").append(join).append("\"\n");

            String miter = attr(e, "stroke-miterlimit");
            if (!miter.isEmpty())
                sb.append("      android:strokeMiterLimit=\"").append(miter).append("\"\n");
        }

        if ("evenodd".equals(attr(e, "fill-rule"))) {
            sb.append("      android:fillType=\"evenOdd\"\n");
        }

        sb.append("      android:pathData=\"").append(normalizePath(d)).append("\"/>\n");
    }

    private static String color(String c, String def) {
        if (c.isEmpty() || c.equals("currentColor")) return def;
        if (c.startsWith("#")) {
            if (c.length() == 4) { // #abc -> #aabbcc
                c =
                        "#"
                                + c.charAt(1)
                                + c.charAt(1)
                                + c.charAt(2)
                                + c.charAt(2)
                                + c.charAt(3)
                                + c.charAt(3);
            }
            return c;
        }
        switch (c.toLowerCase()) {
            case "black":
                return "#000000";
            case "white":
                return "#FFFFFF";
            case "red":
                return "#FF0000";
            case "green":
                return "#008000";
            case "blue":
                return "#0000FF";
            default:
                return def;
        }
    }

    private static String ellipse(float cx, float cy, float rx, float ry) {
        return String.format(
                Locale.US,
                "M%s,%sa%s,%s 0,1,1 %s,0a%s,%s 0,1,1 %s,0z",
                num(cx - rx),
                num(cy),
                num(rx),
                num(ry),
                num(rx * 2),
                num(rx),
                num(ry),
                num(-rx * 2));
    }

    private static float f(Element e, String a) {
        String v = e.getAttribute(a);
        return v.isEmpty() ? 0 : parseSize(v, 0);
    }

    private static float parseSize(String s, float def) {
        if (s == null || s.isEmpty()) return def;
        try {
            return Float.parseFloat(s.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception e) {
            return def;
        }
    }

    private static String num(float v) {
        return (v == (int) v) ? String.valueOf((int) v) : String.format(Locale.US, "%.3f", v);
    }
}
