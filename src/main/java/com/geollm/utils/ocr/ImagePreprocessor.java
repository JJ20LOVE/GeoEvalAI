package com.geollm.utils.ocr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ImagePreprocessor {

    public static BufferedImage decode(byte[] bytes) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    public static BufferedImage cropByRatio(BufferedImage img, double x1, double y1, double x2, double y2) {
        int w = img.getWidth();
        int h = img.getHeight();
        int left = (int) Math.round(x1 * w);
        int top = (int) Math.round(y1 * h);
        int right = (int) Math.round(x2 * w);
        int bottom = (int) Math.round(y2 * h);
        left = Math.max(0, Math.min(left, w - 1));
        top = Math.max(0, Math.min(top, h - 1));
        right = Math.max(left + 1, Math.min(right, w));
        bottom = Math.max(top + 1, Math.min(bottom, h));
        BufferedImage sub = img.getSubimage(left, top, right - left, bottom - top);

        // 拷贝到新图，避免引用原图导致内存保留
        BufferedImage copy = new BufferedImage(sub.getWidth(), sub.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(sub, 0, 0, null);
        g.dispose();
        return copy;
    }

    // 灰度 + Otsu 二值化 + 简单膨胀增强笔画
    public static BufferedImage preprocessHandwriting(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        WritableRaster r = gray.getRaster();
        int[] hist = new int[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = r.getSample(x, y, 0);
                hist[v]++;
            }
        }
        int thr = otsu(hist, w * h);

        BufferedImage bw = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster br = bw.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = r.getSample(x, y, 0);
                br.setSample(x, y, 0, v > thr ? 1 : 0);
            }
        }

        // 膨胀：黑色为 0，白色为 1；把黑色扩张
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster or = out.getRaster();
        WritableRaster in = bw.getRaster();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = in.getSample(x, y, 0);
                if (val == 0) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                or.setSample(nx, ny, 0, 0);
                            }
                        }
                    }
                } else {
                    // 默认白
                    if (or.getSample(x, y, 0) != 0) or.setSample(x, y, 0, 1);
                }
            }
        }
        return out;
    }

    private static int otsu(int[] hist, int total) {
        double sum = 0;
        for (int t = 0; t < 256; t++) sum += (double) t * hist[t];
        double sumB = 0;
        int wB = 0;
        int wF;
        double varMax = -1;
        int threshold = 127;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            wF = total - wB;
            if (wF == 0) break;
            sumB += (double) t * hist[t];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double varBetween = (double) wB * wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold;
    }

    public static byte[] encodePng(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}

