package nom.tam.image.comp;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2015 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import static nom.tam.fits.header.Compression.COMPRESSED_DATA_COLUMN;
import static nom.tam.fits.header.Compression.ZNAXIS;
import static nom.tam.fits.header.Compression.ZNAXISn;
import static nom.tam.fits.header.Compression.ZSCALE_COLUMN;
import static nom.tam.fits.header.Compression.ZTILEn;
import static nom.tam.fits.header.Compression.ZZERO_COLUMN;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageHDU;
import nom.tam.image.comp.filter.QuantizeOption;
import nom.tam.image.comp.hcompress.HCompressor.ShortHCompress;
import nom.tam.image.comp.hcompress.HCompressorOption;
import nom.tam.image.comp.plio.PLIOCompress;
import nom.tam.image.comp.rice.RiceCompress;
import nom.tam.image.comp.rice.RiceCompressOption;
import nom.tam.util.ArrayFuncs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReadProvidedCompressedImageTest {

    class ImageReader {

        protected double scale;

        protected double zero;

        protected void decompressRow(Header header, Object row) {
        }

        void read(String fileName) throws Exception {
            try (Fits f = new Fits(fileName)) {
                CompressedImageHDU bhdu = (CompressedImageHDU) f.getHDU(1);
                bhdu.getUncompressedData();

                Header hdr = bhdu.getHeader();
                int naxis = hdr.getIntValue(ZNAXIS);
                int[] axes = new int[naxis];
                for (int i = 1; i <= naxis; i += 1) {
                    axes[i - 1] = hdr.getIntValue(ZNAXISn.n(i), -1);
                    if (axes[i - 1] == -1) {
                        throw new FitsException("Required ZNAXISn not found");
                    }
                }
                int[] tiles = new int[axes.length];
                Arrays.fill(tiles, 1);
                tiles[0] = axes[1];
                for (int i = 1; i <= naxis; i += 1) {
                    HeaderCard card = hdr.findCard(ZTILEn.n(i));
                    if (card != null) {
                        tiles[i - 1] = card.getValue(Integer.class, axes[i - 1]);
                    }
                }
                Object[] rows = (Object[]) bhdu.getColumn(COMPRESSED_DATA_COLUMN);
                double[] zzero = null;
                double[] zscale = null;
                try {
                    zzero = (double[]) bhdu.getColumn(ZZERO_COLUMN);
                    zscale = (double[]) bhdu.getColumn(ZSCALE_COLUMN);
                } catch (Exception e) {
                    // columns not available
                }
                for (int tileIndex = 0; tileIndex < rows.length; tileIndex++) {
                    Object row = rows[tileIndex];
                    if (zzero != null) {
                        this.zero = zzero[tileIndex];
                    }
                    if (zscale != null) {
                        this.scale = zscale[tileIndex];
                    }
                    decompressRow(hdr, row);
                }
            }
        }

        Object readAll(String fileName) throws Exception {
            try (Fits f = new Fits(fileName)) {
                CompressedImageHDU bhdu = (CompressedImageHDU) f.getHDU(1);
                return bhdu.getUncompressedData();
            }
        }
    }

    private final boolean showImage = false;

    private ImageHDU m13;

    private short[][] m13_data;

    private ImageHDU m13real;

    private float[][] m13_data_real;

    private void assertData(float[][] data) {
        for (int x = 0; x < 300; x++) {
            for (int y = 0; y < 300; y++) {
                Assert.assertEquals(this.m13_data_real[x][y], data[x][y], 1f);
            }
        }
    }

    private void assertData(short[][] data) {
        for (int x = 0; x < 300; x++) {
            for (int y = 0; y < 300; y++) {
                Assert.assertEquals(this.m13_data[x][y], data[x][y]);
            }
        }
    }

    private void dispayImage(short[][] data) {
        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        BufferedImage img = (BufferedImage) frame.createImage(300, 300);

        Graphics2D g = img.createGraphics(); // Get a Graphics2D object

        for (int y = 0; y < 300; y++) {
            for (int x = 0; x < 300; x++) {
                float grayScale = data[x][y] / 4000f;
                g.setColor(new Color(grayScale, grayScale, grayScale));
                g.drawRect(x, y, 1, 1);
            }
        }
        ImageIcon icon = new ImageIcon(img);
        JLabel label = new JLabel(icon);
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
        frame.pack();
        frame.setVisible(true);
    }

    @Test
    public void readGzip() throws Exception {
        Object result = new ImageReader().readAll("src/test/resources/nom/tam/image/provided/m13_gzip.fits");

        short[][] data = new short[300][300];
        ArrayFuncs.copyInto(((ShortBuffer) result).array(), data);
        assertData(data);
        if (this.showImage) {
            dispayImage(data);
        }
    }

    @Test
    public void readHCompressed() throws Exception {
        final ShortBuffer decompressed = ShortBuffer.wrap(new short[300 * 300]);
        new ImageReader() {

            @Override
            protected void decompressRow(Header hdr, Object row) {
                HCompressorOption options = new HCompressorOption()//
                        .setScale(0)//
                        .setSmooth(false)//
                        .setTileWidth(hdr.getIntValue(ZTILEn.n(1)))//
                        .setTileHeigth(hdr.getIntValue(ZTILEn.n(2)));
                new ShortHCompress(options).decompress(ByteBuffer.wrap((byte[]) row), decompressed);
            }

        }.read("src/test/resources/nom/tam/image/provided/m13_hcomp.fits");

        short[][] data = new short[300][300];
        ArrayFuncs.copyInto(decompressed.array(), data);

        assertData(data);
        if (this.showImage) {
            dispayImage(data);
        }
    }

    @Test
    public void readPLIO() throws Exception {
        final ShortBuffer decompressed = ShortBuffer.wrap(new short[300 * 300]);
        new ImageReader() {

            @Override
            protected void decompressRow(Header hdr, Object row) {
                ShortBuffer slice = decompressed.slice();
                slice.limit(300);
                ByteBuffer bytes = ByteBuffer.wrap(new byte[((short[]) row).length * 2]);
                bytes.asShortBuffer().put((short[]) row);
                bytes.rewind();
                new PLIOCompress.ShortPLIOCompress().decompress(bytes, slice);
                decompressed.position(decompressed.position() + 300);
            }

        }.read("src/test/resources/nom/tam/image/provided/m13_plio.fits");
        short[][] data = new short[300][300];
        ArrayFuncs.copyInto(decompressed.array(), data);
        assertData(data);
        if (this.showImage) {
            dispayImage(data);
        }
    }

    @Test
    public void readReal() throws Exception {
        final FloatBuffer decompressed = FloatBuffer.wrap(new float[300 * 300]);
        new ImageReader() {

            @Override
            protected void decompressRow(Header hdr, Object row) {

                RiceCompressOption options = new RiceCompressOption()//
                        .setBlockSize(32);
                QuantizeOption quant = new QuantizeOption()//
                        .setBScale(this.scale)//
                        .setBZero(this.zero)//
                        .setTileHeigth(1)//
                        .setTileWidth(300);
                FloatBuffer wrapedint = FloatBuffer.wrap(new float[300]);
                new RiceCompress.FloatRiceCompress(quant, options).decompress(ByteBuffer.wrap((byte[]) row), wrapedint);
                for (int index = 0; index < 300; index++) {
                    decompressed.put(wrapedint.get(index));
                }
            }

        }.read("src/test/resources/nom/tam/image/provided/m13real_rice.fits");
        float[][] data = new float[300][300];
        ArrayFuncs.copyInto(decompressed.array(), data);
        assertData(data);
    }

    @Test
    public void readRice() throws Exception {
        Object result = new ImageReader().readAll("src/test/resources/nom/tam/image/provided/m13_rice.fits");

        short[][] data = new short[300][300];
        ArrayFuncs.copyInto(((ShortBuffer) result).array(), data);
        assertData(data);
        if (this.showImage) {
            dispayImage(data);
        }
    }

    @Before
    public void setpup() throws Exception {
        try (Fits f = new Fits("src/test/resources/nom/tam/image/provided/m13.fits")) {
            this.m13 = (ImageHDU) f.getHDU(0);
            this.m13_data = (short[][]) this.m13.getData().getData();
        }
        try (Fits f = new Fits("src/test/resources/nom/tam/image/provided/m13real.fits")) {
            this.m13real = (ImageHDU) f.getHDU(0);
            this.m13_data_real = (float[][]) this.m13real.getData().getData();
        }
    }
}
