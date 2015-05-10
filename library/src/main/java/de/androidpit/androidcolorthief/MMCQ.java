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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MMCQ {

	private static final int SIGBITS = 5;
	private static final int RSHIFT = 8 - SIGBITS;
	private static final int MAX_ITERATIONS = 1000;
	private static final double FRACT_BY_POPULATION = 0.75;
	
	public static List<int[]> compute(Bitmap image, int maxcolors) throws IOException {
		List<int[]> pixels = getPixels(image);
		return compute(pixels, maxcolors);
	}
	
	public static List<int[]> compute(List<int[]> pixels, int maxcolors) {
		CMap map = quantize(pixels, maxcolors);
		return map.palette();
	}
	
	private static List<int[]> getPixels(Bitmap image) {
		int width = image.getWidth();
		int height = image.getHeight();
		List<int[]> res = new ArrayList<int[]>();
		List<Integer> t = new ArrayList<Integer>();
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

	static class VBox {
		private int r1;
		private int r2;
		private int g1;
		private int g2;
		private int b1;
		private int b2;

		@Override
		public String toString() {
			return "r1: " + r1 + " / r2: " + r2 + " / g1: " + g1 + " / g2: " + g2 + " / b1: " + b1 + " / b2: " + b2 + "\n";
		}

		// r1: 0 / r2: 18 / g1: 0 / g2: 31 / b1: 0 / b2: 31
		public VBox(int r1, int r2, int g1, int g2, int b1, int b2, List<Integer> histo) {
			super();
			this.r1 = r1;
			this.r2 = r2;
			this.g1 = g1;
			this.g2 = g2;
			this.b1 = b1;
			this.b2 = b2;
			this.histo = histo;
		}

		private int[] _avg;
		private Integer _volume;
		private Integer _count;
		private List<Integer> histo = new ArrayList<Integer>();

		public int getVolume(boolean recompute) {
			if (_volume == null || recompute) {
				_volume = ((r2 - r1 + 1) * (g2 - g1 + 1) * (b2 - b1 + 1));
			}
			return _volume;
		}

		public VBox clone() {
			VBox clone = new VBox(r1, r2, g1, g2, b1, b2, histo);
			return clone;
		}

		public int[] avg(boolean recompute) {
			if (_avg == null || recompute) {
				int ntot = 0, mult = 1 << (8 - SIGBITS), rsum = 0, gsum = 0, bsum = 0, hval, i, j, k, histoindex;
				for (i = r1; i <= r2; i++) {
					for (j = g1; j <= g2; j++) {
						for (k = b1; k <= b2; k++) {
							histoindex = getColorIndex(i, j, k);
							Integer g = histo.get(histoindex);
							hval = (g != null ? g : 0);
							ntot += hval;
							rsum += (hval * (i + 0.5) * mult);
							gsum += (hval * (j + 0.5) * mult);
							bsum += (hval * (k + 0.5) * mult);
						}
					}
				}
				if (ntot > 0) {
					_avg = new int[] { ~~(rsum / ntot), ~~(gsum / ntot), ~~(bsum / ntot) };
				} else {
					_avg = new int[] { ~~(mult * (r1 + r2 + 1) / 2), ~~(mult * (g1 + g2 + 1) / 2), ~~(mult * (b1 + b2 + 1) / 2) };
				}

			}
			return _avg;
		}

		public boolean contains(int[] pixel) {
			int rval = pixel[0] >> RSHIFT, gval = pixel[1] >> RSHIFT, bval = pixel[2] >> RSHIFT;
			return (rval >= r1 && rval <= r2 && gval >= g1 && gval <= g2 && bval >= b1 && bval <= b2);
		}

		public int count(boolean recompute) {
			if (_count == null || recompute) {
				int npix = 0, i, j, k, index;
				for (i = r1; i <= r2; i++) {
					for (j = g1; j <= g2; j++) {
						for (k = b1; k <= b2; k++) {
							index = getColorIndex(i, j, k);
							Integer g = histo.get(index);
							npix += (g != null ? g : 0);
						}
					}
				}
				_count = npix;
			}
			return _count;
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

		public List<Integer> getHisto() {
			return histo;
		}

		public void setHisto(List<Integer> histo) {
			this.histo = histo;
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
	}

	static class DenormalizedVBox {
		private VBox vbox;
		private int[] color;

		public DenormalizedVBox(VBox vbox, int[] color) {
			this.vbox = vbox;
			this.color = color;
		}

		public VBox getVbox() {
			return vbox;
		}

		public void setVbox(VBox vbox) {
			this.vbox = vbox;
		}

		public int[] getColor() {
			return color;
		}

		public void setColor(int[] color) {
			this.color = color;
		}
	}

	static class CMap {
		private ArrayList<DenormalizedVBox> vboxes = new ArrayList<DenormalizedVBox>();

		public void push(VBox box) {
			vboxes.add(new DenormalizedVBox(box, box.avg(false)));
		}

		public List<int[]> palette() {
			List<int[]> r = new ArrayList<int[]>();
			Iterator<DenormalizedVBox> it = vboxes.iterator();
			while (it.hasNext()) {
				DenormalizedVBox denormalizedVBox = (DenormalizedVBox) it.next();
				r.add(denormalizedVBox.getColor());
			}
			Collections.reverse(r);
			return r;
		}

		public int size() {
			return vboxes.size();
		}

		public int[] map(int[] color) {
			Iterator<DenormalizedVBox> it = vboxes.iterator();
			while (it.hasNext()) {
				DenormalizedVBox vb = (DenormalizedVBox) it.next();
				if (vb.vbox.contains(color))
					return vb.color;
			}
			return nearest(color);
		}

		public int[] nearest(int[] color) {
			Double d1 = null, d2 = null;
			int[] pColor = null;
			Iterator<DenormalizedVBox> it = vboxes.iterator();
			while (it.hasNext()) {
				DenormalizedVBox vb = (DenormalizedVBox) it.next();
				d2 = Math.sqrt(Math.pow(color[0] - vb.getColor()[0], 2) + Math.pow(color[1] - vb.getColor()[1], 2) + Math.pow(color[2] - vb.getColor()[2], 2));
				if (d2 < d1 || d1 == null) {
					d1 = d2;
					pColor = vb.getColor();
				}
			}
			return pColor;
		}
	}

	private static List<Integer> getHisto(List<int[]> pixels) {
		int histosize = 1 << (3 * SIGBITS);
		List<Integer> histo = new ArrayList<Integer>(histosize);
		for (int i = 0; i < histosize; i++) {
			histo.add(0);
		}
		int index, rval, gval, bval;
		Iterator<int[]> it = pixels.iterator();
		while (it.hasNext()) {
			int[] pixel = (int[]) it.next();
			rval = pixel[0] >> RSHIFT;
			gval = pixel[1] >> RSHIFT;
			bval = pixel[2] >> RSHIFT;
			index = getColorIndex(rval, gval, bval);
			Integer cur = histo.get(index);
			histo.set(index, (cur == null ? 0 : cur) + 1);
		}
		return histo;
	}

	private static VBox vboxFromPixels(List<int[]> pixels, List<Integer> histo) {
		int rmin = 1000000, rmax = 0, gmin = 1000000, gmax = 0, bmin = 1000000, bmax = 0, rval, gval, bval;
		Iterator<int[]> it = pixels.iterator();
		while (it.hasNext()) {
			int[] pixel = (int[]) it.next();
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
		VBox vbox = new VBox(rmin, rmax, gmin, gmax, bmin, bmax, histo);
		return vbox;
	}

	private static VBox[] medianCutApply(List<Integer> histo, VBox vbox) {
		if (vbox.count(false) == 0)
			return null;
		if (vbox.count(false) == 1) {
			return new VBox[] { vbox.clone() };
		}
		int rw = vbox.r2 - vbox.r1 + 1, gw = vbox.g2 - vbox.g1 + 1, bw = vbox.b2 - vbox.b1 + 1, maxw = Math.max(Math.max(rw, gw), bw);

		int total = 0, i, j, k, sum, index;
		List<Integer> partialsum = new ArrayList<Integer>();
		List<Integer> lookaheadsum = new ArrayList<Integer>();
		if (maxw == rw) {
			for (i = vbox.r1; i <= vbox.r2; i++) {
				sum = 0;
				for (j = vbox.g1; j <= vbox.g2; j++) {
					for (k = vbox.b1; k <= vbox.b2; k++) {
						index = getColorIndex(i, j, k);
						Integer r = histo.get(index);
						sum += (r != null ? r : 0);
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
			for (i = vbox.g1; i <= vbox.g2; i++) {
				sum = 0;
				for (j = vbox.r1; j <= vbox.r2; j++) {
					for (k = vbox.b1; k <= vbox.b2; k++) {
						index = getColorIndex(j, i, k);
						Integer r = histo.get(index);
						sum += (r != null ? r : 0);
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
			for (i = vbox.b1; i <= vbox.b2; i++) {
				sum = 0;
				for (j = vbox.r1; j <= vbox.r2; j++) {
					for (k = vbox.g1; k <= vbox.g2; k++) {
						index = getColorIndex(j, k, i);
						Integer r = histo.get(index);
						sum += (r != null ? r : 0);
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
		Iterator<Integer> it = partialsum.iterator();
		i = 0;
		while (it.hasNext()) {
			Integer integer = (Integer) it.next();
			lookaheadsum.add(i, total - integer);
			i++;
		}
		return maxw == rw ? doCut("r", vbox, partialsum, lookaheadsum, total) : maxw == gw ? doCut("g", vbox, partialsum, lookaheadsum, total) : doCut("b", vbox, partialsum, lookaheadsum, total);
	}

	private static VBox[] doCut(String color, VBox vbox, List<Integer> partialsum, List<Integer> lookaheadsum, int total) {
		int dim1 = 0, dim2 = 0;
		if ("r".equals(color)) {
			dim1 = vbox.getR1();
			dim2 = vbox.getR2();
		} else if ("g".equals(color)) {
			dim1 = vbox.getG1();
			dim2 = vbox.getG2();
		} else if ("b".equals(color)) {
			dim1 = vbox.getB1();
			dim2 = vbox.getB2();
		}
		VBox vbox1 = null, vbox2 = null;
		int left, right, d2;
		Integer count2;
		for (int i = dim1; i < dim2; i++) {
			if (partialsum.get(i) > total / 2) {
				vbox1 = vbox.clone();
				vbox2 = vbox.clone();
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
				if ("r".equals(color)) {
					vbox1.setR2(d2);
					vbox2.setR1(vbox1.getR2() + 1);
				} else if ("g".equals(color)) {
					vbox1.setG2(d2);
					vbox2.setG1(vbox1.getG2() + 1);
				} else if ("b".equals(color)) {
					vbox1.setB2(d2);
					vbox2.setB1(vbox1.getB2() + 1);
				}
				return new VBox[] { vbox1, vbox2 };
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static CMap quantize(List<int[]> pixels, int maxcolors) {
		if (pixels.size() == 0 || maxcolors < 2 || maxcolors > 256) {
			return null; 
		}
		List<Integer> histo = getHisto(pixels);
		int nColors = 0;
		VBox vbox = vboxFromPixels(pixels, histo);
		List<VBox> pq = new ArrayList<VBox>();
		pq.add(vbox);
		int niters = 0;
		Object[] r = iter(pq, FRACT_BY_POPULATION * maxcolors, histo, nColors, niters);
		pq = (List<VBox>) r[0];
		nColors = (Integer) r[1];
		niters = (Integer) r[2];
		Collections.sort(pq, new Comparator<VBox>() {
			@Override
			public int compare(VBox o1, VBox o2) {
				return new Double(o1.count(false) * o1.getVolume(false)).compareTo(new Double(o1.count(false) * o1.getVolume(false)));
			}
		});
		r = iter(pq, maxcolors - pq.size(), histo, nColors, niters);
		pq = (List<VBox>) r[0];
		nColors = (Integer) r[1];
		niters = (Integer) r[2];
		CMap cmap = new CMap();
		for (Iterator<VBox> iterator = pq.iterator(); iterator.hasNext();) {
			VBox vBox2 = iterator.next();
			cmap.push(vBox2);
		}
		return cmap;
	}

	private static Object[] iter(List<VBox> lh, double target, List<Integer> histo, int nColors, int niters) {
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
				return new Object[] { lh, nColors, niters };
			lh.add(vbox1);
			if (vbox2 != null) {
				lh.add(vbox2);
				nColors++;
			}
			if (nColors >= target)
				return new Object[] { lh, nColors, niters };
			if (niters++ > MAX_ITERATIONS) {
				return new Object[] { lh, nColors, niters };
			}
			Collections.sort(lh, new Comparator<VBox>() {
				@Override
				public int compare(VBox o1, VBox o2) {
					return new Double(o1.count(false) * o1.getVolume(false)).compareTo(new Double(o2.count(false) * o2.getVolume(false)));
				}
			});
		}
		return new Object[] { lh, nColors, niters };
	}

}
