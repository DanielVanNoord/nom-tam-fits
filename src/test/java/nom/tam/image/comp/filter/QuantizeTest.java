package nom.tam.image.comp.filter;

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

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

import nom.tam.image.comp.CompParameter;
import nom.tam.image.comp.INullCheck;
import nom.tam.util.ArrayFuncs;

import org.junit.Assert;
import org.junit.Test;

public class QuantizeTest {

    private static final double NULL_VALUE = -9.1191291391491004e-36;

    CompParameter nullCheckOff = CompParameter.create(INullCheck.class, new INullCheck() {

        @Override
        public double setNull(int index) {
            return 0;
        }

        @Override
        public boolean isNull(double d) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean isNull(int integer) {
            return false;
        }
    });

    CompParameter nullCheckValue = CompParameter.create(INullCheck.class, new INullCheck() {

        @Override
        public double setNull(int index) {
            return NULL_VALUE;
        }

        @Override
        public boolean isNull(double d) {
            return d == NULL_VALUE;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isNull(int integer) {
            return (Integer.MIN_VALUE + 1) == integer;
        }
    });

    @Test
    public void testQuant1Double() throws Exception {
        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/nom/tam/image/comp/bare/test100Data-64.bin", "r");//
        ) {
            byte[] bytes = new byte[(int) file.length()];
            double[] doubles = new double[bytes.length / 8];
            file.read(bytes);
            ByteBuffer.wrap(bytes).asDoubleBuffer().get(doubles);

            float qlevel = 4f;
            Quantize quantize = new Quantize(nullCheckOff.set(IDither.class, new SubtractiveDither(8864L)));
            quantize.quantize(doubles, 100, 100, qlevel);
            checkRequantedValues(quantize, doubles);

            // values extracted from cfitsio debugging
            Assert.assertEquals(1.2435136069284944e+17, quantize.getNoise2(), 1e-19);
            Assert.assertEquals(4511571366641730d, quantize.getNoise3(), 1e-19);
            Assert.assertEquals(9651138576018.3047d, quantize.getNoise5(), 1e-19);

            Assert.assertEquals(2412784644004.5762, quantize.getBScale(), 1e-19);
            Assert.assertEquals(0d, quantize.getBZero(), 1e-19);
            Assert.assertEquals(0, quantize.getIntMinValue());
            Assert.assertEquals(1911354, quantize.getIntMaxValue());
        }
    }

    private void checkRequantedValues(Quantize quantize, double[] doubles) {
        double[] output = new double[quantize.getIntData().length];
        quantize.unquantize(quantize.getIntData(), quantize.getIntData().length, output);
        // Assert.assertArrayEquals(doubles, output, 1);
    }

    @Test
    public void testQuant1Float() throws Exception {
        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/nom/tam/image/comp/bare/test100Data-32.bin", "r");//
        ) {
            byte[] bytes = new byte[(int) file.length()];
            float[] floats = new float[bytes.length / 4];
            double[] doubles = new double[bytes.length / 4];
            file.read(bytes);
            ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
            ArrayFuncs.copyInto(floats, doubles);

            float qlevel = 4f;
            Quantize quantize = new Quantize(nullCheckOff.set(IDither.class, new SubtractiveDither(3942L)));
            quantize.quantize(doubles, 100, 100, qlevel);
            checkRequantedValues(quantize, doubles);

            // values extracted from cfitsio debugging (but adapted a little
            // because we convert the float back to doubles) and assume they are
            // correct because the are so close.
            Assert.assertEquals(28952793.664512001, quantize.getNoise2(), 1e-19);
            Assert.assertEquals(1050418.9324832, quantize.getNoise3(), 1e-19);
            Assert.assertEquals(2251.2097792, quantize.getNoise5(), 1e-19);

            Assert.assertEquals(562.8024448, quantize.getBScale(), 1e-19);
            Assert.assertEquals(0d, quantize.getBZero(), 1e-19);
            Assert.assertEquals(0, quantize.getIntMinValue());
            Assert.assertEquals(1907849, quantize.getIntMaxValue());

        }
    }

    @Test
    public void testDifferentQuantCases() {
        int xsize = 12;
        int ysize = 2;
        double[] matrix = initMatrix();
        Quantize quantize;

        quantize = new Quantize(nullCheckOff.set(IDither.class, new SubtractiveDither2(3942L)));
        matrix = initMatrix();
        // matrix 0.00000000000000000000e+00, 9.99983333416666475557e+00,
        // 1.99986666933330816676e+01, 2.99955002024956591811e+01,
        // 3.99893341866341600621e+01, 4.99791692706783337030e+01,
        // 5.99640064794445919460e+01, 6.99428473375327683925e+01,
        // 7.99146939691726885258e+01, 8.98785491980110435861e+01,
        // 9.98334166468281551943e+01, 1.09778300837174811022e+02,
        // 1.19712207288919358916e+02, 1.29634142619694870291e+02,
        // 1.39543114644236482036e+02, 1.49438132473599210925e+02,
        // 1.59318206614245980290e+02, 1.69182349066996039255e+02,
        // 1.79029573425824167998e+02, 1.88858894976500579332e+02,
        // 1.98669330795061227946e+02, 2.08459899846099546039e+02,
        // 2.18229623080869316709e+02, 2.27977523535188396409e+02

        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize, -4f));
        Assert.assertArrayEquals(new int[]{
            -2147483646,
            -2147483634,
            -2147483632,
            -2147483629,
            -2147483627,
            -2147483625,
            -2147483622,
            -2147483619,
            -2147483617,
            -2147483615,
            -2147483612,
            -2147483609,
            -2147483607,
            -2147483604,
            -2147483602,
            -2147483599,
            -2147483597,
            -2147483595,
            -2147483593,
            -2147483590,
            -2147483587,
            -2147483585,
            -2147483582,
            -2147483580

        }, quantize.getIntData());
        Assert.assertEquals(4.000000e+00, quantize.getBScale(), 1e-20);
        Assert.assertEquals(8.589934548e+09, quantize.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantize.getIntMinValue());
        Assert.assertEquals(-2147483580, quantize.getIntMaxValue());

        quantize = new Quantize(nullCheckOff.set(IDither.class, new NoDither2()));
        matrix = initMatrix();
        Assert.assertTrue(quantize.quantize(matrix, xsize - 3, ysize, -0f));
        Assert.assertArrayEquals(new int[]{
            -2147483637,
            -2130848351,
            -2114214728,
            -2097584433,
            -2080959127,
            -2064340473,
            -2047730134,
            -2031129770,
            -2014541041,
            -1997965607,
            -1981405124,
            -1964861249,
            -1948335636,
            -1931829938,
            -1915345804,
            -1898884885,
            -1882448825,
            -1866039268

        }, quantize.getIntData());
        Assert.assertEquals(6.01121812296506193330e-07, quantize.getBScale(), 1e-20);
        Assert.assertEquals(1.29089925575053234752e+03, quantize.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantize.getIntMinValue());
        Assert.assertEquals(-1866039268, quantize.getIntMaxValue());
        checkRequantedValues(quantize, matrix);

        quantize = new Quantize(nullCheckOff.set(IDither.class, new NoDither()));
        matrix = initMatrix();
        Assert.assertFalse(quantize.quantize(matrix, 3, 2, 4f));

        quantize = new Quantize(nullCheckValue.set(IDither.class, new NoDither()));
        matrix = initMatrix();
        matrix[5] = NULL_VALUE;
        Assert.assertFalse(quantize.quantize(matrix, 3, 2, 4f));

        quantize = new Quantize(nullCheckValue.set(IDither.class, new NoDither()));
        matrix = initMatrix();
        Arrays.fill(matrix, 11, xsize + 1, NULL_VALUE);
        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize, 4f));
        Assert.assertEquals(xsize * ysize, quantize.getIntData().length);
        Assert.assertArrayEquals(new int[]{
            -2147483637,
            -2135162120,
            -2122841835,
            -2110524015,
            -2098209890,
            -2085900693,
            -2073597653,
            -2061302003,
            -2049014970,
            -2036737785,
            -2024471673,
            -2147483647,
            -2147483647,
            -1987752046,
            -1975542486,
            -1963350120,
            -1951176167,
            -1939021845,
            -1926888368,
            -1914776951,
            -1902688805,
            -1890625137,
            -1878587156,
            -1866576064

        }, quantize.getIntData());
        Assert.assertEquals(8.11574856349585578526e-07, quantize.getBScale(), 1e-20);
        Assert.assertEquals(1.74284372421136049525e+03, quantize.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantize.getIntMinValue());
        Assert.assertEquals(-1866576064, quantize.getIntMaxValue());
        checkRequantedValues(quantize, matrix);

        quantize = new Quantize(nullCheckValue.set(IDither.class, new NoDither()));
        matrix = initMatrix();
        Arrays.fill(matrix, 11, xsize + 1, NULL_VALUE);
        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize, 4f));
        Assert.assertArrayEquals(new int[]{
            -2147483637,
            -2135162120,
            -2122841835,
            -2110524015,
            -2098209890,
            -2085900693,
            -2073597653,
            -2061302003,
            -2049014970,
            -2036737785,
            -2024471673,
            -2147483647,
            -2147483647,
            -1987752046,
            -1975542486,
            -1963350120,
            -1951176167,
            -1939021845,
            -1926888368,
            -1914776951,
            -1902688805,
            -1890625137,
            -1878587156,
            -1866576064

        }, quantize.getIntData());
        Assert.assertEquals(8.11574856349585578526e-07, quantize.getBScale(), 1e-20);
        Assert.assertEquals(1.74284372421136049525e+03, quantize.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantize.getIntMinValue());
        Assert.assertEquals(-1866576064, quantize.getIntMaxValue());
        checkRequantedValues(quantize, matrix);

        // test very small image
        quantize = new Quantize(nullCheckValue.set(IDither.class, new NoDither()));
        matrix = initMatrix();
        Assert.assertFalse(quantize.quantize(matrix, 1, 1, 4f));

        // test null image
        quantize = new Quantize(nullCheckValue.set(IDither.class, new NoDither()));
        Arrays.fill(matrix, NULL_VALUE);
        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize, 4f));
        Assert.assertArrayEquals(new int[]{
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647,
            -2147483647

        }, quantize.getIntData());
        Assert.assertEquals(2.50000000000000000000e-01, quantize.getBScale(), 1e-20);
        Assert.assertEquals(5.36870909250000000000e+08, quantize.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantize.getIntMinValue());
        Assert.assertEquals(-2147483633, quantize.getIntMaxValue());
        checkRequantedValues(quantize, matrix);

        int[][] expectedData = {
            {
                -2147483646,
                -2139144306,
                -2130805810,
                -2122468981,
                -2114134654,
                -2105803661,
                -2097476837,
                -2089155012,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483637,
                -2139144306,
                -2130805810,
                -2122468981,
                -2114134654,
                -2105803661,
                -2097476837,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483646,
                -2139144306,
                -2130805810,
                -2122468981,
                -2114134654,
                -2105803661,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483637,
                -2139144306,
                -2130805810,
                -2122468981,
                -2114134654,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483646,
                -2139144306,
                -2130805810,
                -2122468981,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483637,
                -2139144306,
                -2130805810,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483646,
                -2139144306,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            },
            {
                -2147483637,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2147483647,
                -2047650006,
                -2039375639,
                -2031112082,
                -2022860162,
                -2014620704,
                -2006394533,
                -1998182470,
                -1989985337,
                -1981803954,
                -1973639139,
                -1965491708,
                -1957362476
            }
        };
        int expectedIndex = 0;
        for (int index = 8; index > 0; index--) {
            quantize = new Quantize(nullCheckValue.set(IDither.class, index % 2 == 1 ? new SubtractiveDither(3942L) : new SubtractiveDither2(3942L)));
            matrix = initMatrix();
            Arrays.fill(matrix, index, xsize, NULL_VALUE);
            Assert.assertTrue(quantize.quantize(matrix, xsize, ysize, 4f));
            Assert.assertArrayEquals(expectedData[expectedIndex], quantize.getIntData());

            Assert.assertEquals(1.19911703788955035348e-06, quantize.getBScale(), 1e-20);
            Assert.assertEquals(2.57508421771571829595e+03, quantize.getBZero(), 1e-20);
            Assert.assertEquals(-2147483637, quantize.getIntMinValue());
            Assert.assertEquals(-1957362476, quantize.getIntMaxValue());
            checkRequantedValues(quantize, matrix);

            expectedIndex++;
        }
    }

    private double[] initMatrix() {
        double[] matrix = new double[1000];
        for (int index = 0; index < matrix.length; index++) {
            matrix[index] = Math.sin(((double) index) / 100d) * 1000d;
        }
        return matrix;
    }

    @Test
    public void testNoDither() {
        NoDither dither = new NoDither();
        Assert.assertFalse(dither.centerOnZero());
        Assert.assertFalse(dither.isActive());
        Assert.assertFalse(dither.isZeroValue(0.0));
        Assert.assertFalse(dither.isZeroValue(0, 0));
        Assert.assertEquals(0.0, dither.nextRandom(), 1e-20);
        dither.incrementRandom();

    }
}
