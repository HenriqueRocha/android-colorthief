/*
 * Copyright (C) 2015 Henrique Rocha
 * Copyright (C) 2014 Fonpit AG
 *
 * License
 * -------
 * Creative Commons Attribution 2.5 License:
 * http://creativecommons.org/licenses/by/2.5/
 *
 * Thanks
 * ------
 * Simon Oualid - For creating java-colorthief
 * available at https://github.com/soualid/java-colorthief
 *
 * Lokesh Dhakar - for the original Color Thief javascript version
 * available at http://lokeshdhakar.com/projects/color-thief/
 *
 */

package de.androidpit.androidcolorthief;

import android.graphics.Bitmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MMCQ {

    private static final int SIGBITS = 5;
    private static final int RSHIFT = 8 - SIGBITS;
    private static final int MAX_ITERATIONS = 1000;
    private static final double FRACT_BY_POPULATION = 0.75;

    private static final int RED = 0;
    private static final int GREEN = 1;
    private static final int BLUE = 2;

    public static List<int[]> compute(Bitmap image, int maxcolors) throws IOException {
        List<int[]> pixels = getPixels(image);
        return compute(pixels, maxcolors);
    }

    public static List<int[]> compute(List<int[]> pixels, int maxcolors) {
        ColorMap map = quantize(pixels, maxcolors);
        return map.palette();
    }

    private static List<int[]> getPixels(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        List<int[]> res = new ArrayList<>();
        List<Integer> t = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                t.add(image.getPixel(col, row));
            }
        }
        for (int i = 0; i < t.size(); i += 10) {
            int[] rr = new int[3];
            int argb = t.get(i);
            rr[0] = (argb >> 16) & 0xFF;
            rr[1] = (argb >> 8) & 0xFF;
            rr[2] = (argb) & 0xFF;
            if (!(rr[0] > 250 && rr[1] > 250 && rr[2] > 250)) {
                res.add(rr);
            }
        }
        return res;
    }

    private static int getColorIndex(int r, int g, int b) {
        return (r << (2 * SIGBITS)) + (g << SIGBITS) + b;
    }

    private static int[] histogram(List<int[]> pixels) {
        int[] histogram = new int[1 << (3 * SIGBITS)];

        for (int[] pixel : pixels) {
            int rval = pixel[0] >> RSHIFT;
            int gval = pixel[1] >> RSHIFT;
            int bval = pixel[2] >> RSHIFT;
            histogram[getColorIndex(rval, gval, bval)]++;
        }

        return histogram;
    }

    private static VBox vboxFromPixels(List<int[]> pixels, int[] histo) {
        int rmin = 1000000, rmax = 0, gmin = 1000000, gmax = 0, bmin = 1000000, bmax = 0, rval, gval, bval;
        for (int[] pixel : pixels) {
            rval = pixel[0] >> RSHIFT;
            gval = pixel[1] >> RSHIFT;
            bval = pixel[2] >> RSHIFT;
            if (rval < rmin)
                rmin = rval;
            else if (rval > rmax)
                rmax = rval;
            if (gval < gmin)
                gmin = gval;
            else if (gval > gmax)
                gmax = gval;
            if (bval < bmin)
                bmin = bval;
            else if (bval > bmax)
                bmax = bval;
        }
        return new VBox(rmin, rmax, gmin, gmax, bmin, bmax, histo);
    }

    private static VBox[] medianCutApply(int[] histo, VBox vbox) {
        if (vbox.count(false) == 0)
            return null;
        if (vbox.count(false) == 1) {
            return new VBox[]{vbox.copy()};
        }
        int rw = vbox.r2 - vbox.r1 + 1, gw = vbox.g2 - vbox.g1 + 1, bw = vbox.b2 - vbox.b1 + 1, maxw = Math.max(Math.max(rw, gw), bw);

        int total = 0;
        List<Integer> partialsum = new ArrayList<>();
        List<Integer> lookaheadsum = new ArrayList<>();

        if (maxw == rw) {
            for (int i = vbox.r1; i <= vbox.r2; i++) {
                int sum = 0;
                for (int j = vbox.g1; j <= vbox.g2; j++) {
                    for (int k = vbox.b1; k <= vbox.b2; k++) {
                        sum += histo[getColorIndex(i, j, k)];
                    }
                }
                total += sum;
                if (partialsum.size() < i) {
                    int toAdd = i - partialsum.size();
                    for (int l = partialsum.size(); l < toAdd; l++) {
                        partialsum.add(0);
                    }
                }
                partialsum.add(i, total);
            }
        } else if (maxw == gw) {
            for (int i = vbox.g1; i <= vbox.g2; i++) {
                int sum = 0;
                for (int j = vbox.r1; j <= vbox.r2; j++) {
                    for (int k = vbox.b1; k <= vbox.b2; k++) {
                        sum += histo[getColorIndex(j, i, k)];
                    }
                }
                total += sum;
                if (partialsum.size() < i) {
                    int toAdd = i - partialsum.size();
                    for (int l = partialsum.size(); l < toAdd; l++) {
                        partialsum.add(0);
                    }
                }
                partialsum.add(i, total);
            }
        } else {
            for (int i = vbox.b1; i <= vbox.b2; i++) {
                int sum = 0;
                for (int j = vbox.r1; j <= vbox.r2; j++) {
                    for (int k = vbox.g1; k <= vbox.g2; k++) {
                        sum += histo[getColorIndex(j, k, i)];
                    }
                }
                total += sum;
                if (partialsum.size() < i) {
                    int toAdd = i - partialsum.size();
                    for (int l = partialsum.size(); l < toAdd; l++) {
                        partialsum.add(0);
                    }
                }
                partialsum.add(i, total);
            }
        }

        for (int i = 0; i < partialsum.size(); i++) {
            lookaheadsum.add(i, total - partialsum.get(i));
        }

        return maxw == rw
                ? doCut(RED, vbox, partialsum, lookaheadsum, total)
                : maxw == gw
                ? doCut(GREEN, vbox, partialsum, lookaheadsum, total)
                : doCut(BLUE, vbox, partialsum, lookaheadsum, total);
    }

    private static VBox[] doCut(int color, VBox vbox, List<Integer> partialsum, List<Integer> lookaheadsum, int total) {
        int dim1 = 0, dim2 = 0;
        if (color == RED) {
            dim1 = vbox.getR1();
            dim2 = vbox.getR2();
        } else if (color == GREEN) {
            dim1 = vbox.getG1();
            dim2 = vbox.getG2();
        } else if (color == BLUE) {
            dim1 = vbox.getB1();
            dim2 = vbox.getB2();
        }
        VBox vbox1, vbox2;
        int left, right, d2;
        Integer count2;
        for (int i = dim1; i < dim2; i++) {
            if (partialsum.get(i) > total / 2) {
                vbox1 = vbox.copy();
                vbox2 = vbox.copy();
                left = i - dim1;
                right = dim2 - i;
                if (left <= right) {
                    d2 = Math.min(dim2 - 1, ~~(i + right / 2));
                } else {
                    d2 = Math.max(dim1, ~~(i - 1 - left / 2));
                }
                while (partialsum.get(d2) == null)
                    d2++;
                count2 = lookaheadsum.get(d2);
                while (count2 == null && partialsum.get(d2 - 1) == null)
                    count2 = lookaheadsum.get(--d2);
                if (color == RED) {
                    vbox1.setR2(d2);
                    vbox2.setR1(vbox1.getR2() + 1);
                } else if (color == GREEN) {
                    vbox1.setG2(d2);
                    vbox2.setG1(vbox1.getG2() + 1);
                } else if (color == BLUE) {
                    vbox1.setB2(d2);
                    vbox2.setB1(vbox1.getB2() + 1);
                }
                return new VBox[]{vbox1, vbox2};
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ColorMap quantize(List<int[]> pixels, int maxcolors) {
        if (pixels.size() == 0 || maxcolors < 2 || maxcolors > 256) {
            return null;
        }
        int[] histo = histogram(pixels);
        int nColors = 0;
        VBox vbox = vboxFromPixels(pixels, histo);
        List<VBox> pq = new ArrayList<>();
        pq.add(vbox);
        int niters = 0;
        Object[] r = iter(pq, FRACT_BY_POPULATION * maxcolors, histo, nColors, niters);
        pq = (List<VBox>) r[0];
        nColors = (Integer) r[1];
        niters = (Integer) r[2];
        Collections.sort(pq);
        r = iter(pq, maxcolors - pq.size(), histo, nColors, niters);
        pq = (List<VBox>) r[0];
        ColorMap cmap = new ColorMap();
        for (VBox vBox2 : pq) {
            cmap.push(vBox2);
        }
        return cmap;
    }

    private static Object[] iter(List<VBox> lh, double target, int[] histo, int nColors, int niters) {
        VBox vbox;
        while (niters < MAX_ITERATIONS) {
            vbox = lh.get(lh.size() - 1);
            lh.remove(lh.size() - 1);
            if (vbox.count(false) == 0) {
                lh.add(vbox);
                niters++;
                continue;
            }
            VBox[] vboxes = medianCutApply(histo, vbox);

            VBox vbox1 = vboxes[0];
            VBox vbox2 = vboxes[1];

            if (vbox1 == null)
                return new Object[]{lh, nColors, niters};
            lh.add(vbox1);
            if (vbox2 != null) {
                lh.add(vbox2);
                nColors++;
            }
            if (nColors >= target)
                return new Object[]{lh, nColors, niters};
            if (niters++ > MAX_ITERATIONS) {
                return new Object[]{lh, nColors, niters};
            }
            Collections.sort(lh);
        }
        return new Object[]{lh, nColors, niters};
    }

    static class VBox implements Comparable {
        private int r1;
        private int r2;
        private int g1;
        private int g2;
        private int b1;
        private int b2;
        private int[] avg;
        private Integer volume;
        private Integer count;
        private int[] histo;

        // r1: 0 / r2: 18 / g1: 0 / g2: 31 / b1: 0 / b2: 31
        public VBox(int r1, int r2, int g1, int g2, int b1, int b2, int[] histo) {
            super();
            this.r1 = r1;
            this.r2 = r2;
            this.g1 = g1;
            this.g2 = g2;
            this.b1 = b1;
            this.b2 = b2;
            this.histo = histo;
        }

        @Override
        public String toString() {
            return "r1: " + r1 + " / r2: " + r2 + " / g1: " + g1 + " / g2: " + g2 + " / b1: " + b1 + " / b2: " + b2 + "\n";
        }

        public int getVolume(boolean recompute) {
            if (volume == null || recompute) {
                volume = ((r2 - r1 + 1) * (g2 - g1 + 1) * (b2 - b1 + 1));
            }
            return volume;
        }

        public VBox copy() {
            return new VBox(r1, r2, g1, g2, b1, b2, histo);
        }

        public int[] avg(boolean recompute) {
            if (avg != null && !recompute) return avg;

            int ntot = 0;
            int mult = 1 << (8 - SIGBITS);
            int redSum = 0, greenSum = 0, blueSum = 0;
            int hval;

            for (int i = r1; i <= r2; i++) {
                for (int j = g1; j <= g2; j++) {
                    for (int k = b1; k <= b2; k++) {
                        hval = histo[getColorIndex(i, j, k)];
                        ntot += hval;
                        redSum += (hval * (i + 0.5) * mult);
                        greenSum += (hval * (j + 0.5) * mult);
                        blueSum += (hval * (k + 0.5) * mult);
                    }
                }
            }

            if (ntot > 0) {
                avg = new int[]{~~(redSum / ntot), ~~(greenSum / ntot), ~~(blueSum / ntot)};
            } else {
                avg = new int[]{~~(mult * (r1 + r2 + 1) / 2), ~~(mult * (g1 + g2 + 1) / 2), ~~(mult * (b1 + b2 + 1) / 2)};
            }

            return avg;
        }

        public int count(boolean recompute) {
            if (count != null && !recompute) return count;

            int npix = 0;
            for (int i = r1; i <= r2; i++) {
                for (int j = g1; j <= g2; j++) {
                    for (int k = b1; k <= b2; k++) {
                        int index = getColorIndex(i, j, k);
                        int g = histo[index];
                        npix += g;
                    }
                }
            }
            count = npix;

            return count;
        }

        public int getR1() {
            return r1;
        }

        public void setR1(int r1) {
            this.r1 = r1;
        }

        public int getR2() {
            return r2;
        }

        public void setR2(int r2) {
            this.r2 = r2;
        }

        public int getG1() {
            return g1;
        }

        public void setG1(int g1) {
            this.g1 = g1;
        }

        public int getG2() {
            return g2;
        }

        public void setG2(int g2) {
            this.g2 = g2;
        }

        public int getB1() {
            return b1;
        }

        public void setB1(int b1) {
            this.b1 = b1;
        }

        public int getB2() {
            return b2;
        }

        public void setB2(int b2) {
            this.b2 = b2;
        }

        @Override
        public int compareTo(Object o) {
            VBox anotherVBox = (VBox) o;
            return count(false) * getVolume(false) - anotherVBox.count(false) * anotherVBox.getVolume(false);
        }
    }

    static class ColorMap {
        private ArrayList<int[]> vboxes = new ArrayList<>();

        public void push(VBox box) {
            vboxes.add(0, box.avg(false));
        }

        public List<int[]> palette() {
            return vboxes;
        }

    }

}
