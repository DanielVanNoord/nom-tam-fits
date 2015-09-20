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

    private CompParameter nullCheckOff() {
        return CompParameter.create(INullCheck.class, new INullCheck() {

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

            @Override
            public double getNullValue() {
                return NULL_VALUE;
            }
        });
    }

    private CompParameter nullCheckValue() {
        return CompParameter.create(INullCheck.class, new INullCheck() {

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

            @Override
            public double getNullValue() {
                return NULL_VALUE;
            }

        });
    }

    @Test
    public void testQuant1Double() throws Exception {
        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/nom/tam/image/comp/bare/test100Data-64.bin", "r");//
        ) {
            byte[] bytes = new byte[(int) file.length()];
            double[] doubles = new double[bytes.length / 8];
            file.read(bytes);
            ByteBuffer.wrap(bytes).asDoubleBuffer().get(doubles);

            QuantizeOption quantizeParameter = new QuantizeOption();
            Quantize quantize = new Quantize(nullCheckOff().set(IDither.class, new SubtractiveDither(8864L)).set(quantizeParameter.setQLevel(4)));
            quantize.quantize(doubles, 100, 100);
            checkRequantedValues(quantize, doubles);

            // values extracted from cfitsio debugging
            Assert.assertEquals(1.2435136069284944e+17, quantize.getNoise2(), 1e-19);
            Assert.assertEquals(4511571366641730d, quantize.getNoise3(), 1e-19);
            Assert.assertEquals(9651138576018.3047d, quantize.getNoise5(), 1e-19);

            Assert.assertEquals(2412784644004.5762, quantizeParameter.getBScale(), 1e-19);
            Assert.assertEquals(0d, quantizeParameter.getBZero(), 1e-19);
            Assert.assertEquals(0, quantizeParameter.getIntMinValue());
            Assert.assertEquals(1911354, quantizeParameter.getIntMaxValue());
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

            QuantizeOption quantizeParameter = new QuantizeOption();
            Quantize quantize = new Quantize(nullCheckOff().set(IDither.class, new SubtractiveDither(3942L)).set(quantizeParameter.setQLevel(4)));
            quantize.quantize(doubles, 100, 100);
            checkRequantedValues(quantize, doubles);

            // values extracted from cfitsio debugging (but adapted a little
            // because we convert the float back to doubles) and assume they are
            // correct because the are so close.
            Assert.assertEquals(28952793.664512001, quantize.getNoise2(), 1e-19);
            Assert.assertEquals(1050418.9324832, quantize.getNoise3(), 1e-19);
            Assert.assertEquals(2251.2097792, quantize.getNoise5(), 1e-19);

            Assert.assertEquals(562.8024448, quantizeParameter.getBScale(), 1e-19);
            Assert.assertEquals(0d, quantizeParameter.getBZero(), 1e-19);
            Assert.assertEquals(0, quantizeParameter.getIntMinValue());
            Assert.assertEquals(1907849, quantizeParameter.getIntMaxValue());

        }
    }

    @Test
    public void testDifferentQuantCases2() {
        final int xsize = 12;
        final int ysize = 2;

        QuantizeOption quantizeParameter = new QuantizeOption();
        quantizeParameter.setCheckZero(true);
        Quantize quantize = new Quantize(nullCheckOff().set(IDither.class, new NoDither2()).set(quantizeParameter.setQLevel(0)));
        double[] matrix = initMatrix();
        Assert.assertTrue(quantize.quantize(matrix, xsize - 3, ysize));
      
        Assert.assertEquals(6.01121812296506193330e-07, quantizeParameter.getBScale(), 1e-20);
        Assert.assertEquals(1.29089925575053234752e+03, quantizeParameter.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
        Assert.assertEquals(-1866039268, quantizeParameter.getIntMaxValue());
        checkRequantedValues(quantize, matrix);
    }

    @Test
    public void testDifferentQuantCases3() {
        final int xsize = 12;
        final int ysize = 2;

        QuantizeOption quantizeParameter = new QuantizeOption();
        Quantize quantize = new Quantize(nullCheckValue().set(IDither.class, new NoDither()).set(quantizeParameter.setQLevel(4)));
        double[] matrix = initMatrix();
        Arrays.fill(matrix, 11, xsize + 1, NULL_VALUE);
        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize));
        Assert.assertEquals(xsize * ysize, quantize.getIntData().length);
      
        Assert.assertEquals(8.11574856349585578526e-07, quantizeParameter.getBScale(), 1e-20);
        Assert.assertEquals(1.74284372421136049525e+03, quantizeParameter.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
        Assert.assertEquals(-1866576064, quantizeParameter.getIntMaxValue());
        checkRequantedValues(quantize, matrix);
    }

    @Test
    public void testDifferentQuantCases4() {
        final int xsize = 12;
        final int ysize = 2;

        QuantizeOption quantizeParameter = new QuantizeOption();
        Quantize quantize = new Quantize(nullCheckValue().set(IDither.class, new NoDither()).set(quantizeParameter.setQLevel(4)));
        double[] matrix = initMatrix();
        Arrays.fill(matrix, 11, xsize + 1, NULL_VALUE);
        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize));
     
        Assert.assertEquals(8.11574856349585578526e-07, quantizeParameter.getBScale(), 1e-20);
        Assert.assertEquals(1.74284372421136049525e+03, quantizeParameter.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
        Assert.assertEquals(-1866576064, quantizeParameter.getIntMaxValue());
        checkRequantedValues(quantize, matrix);
    }

    @Test
    public void testDifferentQuantCases5() {
        final int xsize = 12;
        final int ysize = 2;

        QuantizeOption quantizeParameter = new QuantizeOption();
        // test null image
         Quantize quantize = new Quantize(nullCheckValue().set(IDither.class, new
         NoDither()).set(quantizeParameter.setQLevel(4)));
         double[] matrix = initMatrix();
         Arrays.fill(matrix, NULL_VALUE);
         Assert.assertTrue(quantize.quantize(matrix, xsize, ysize));
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
         Assert.assertEquals(2.50000000000000000000e-01,
         quantizeParameter.getBScale(), 1e-20);
         Assert.assertEquals(5.36870909250000000000e+08,
         quantizeParameter.getBZero(), 1e-20);
         Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
         Assert.assertEquals(-2147483633, quantizeParameter.getIntMaxValue());
         checkRequantedValues(quantize, matrix);
    }
    @Test
    public void testDifferentQuantCases() {
        final int xsize = 12;
        final int ysize = 2;
        double[] matrix = initMatrix();
        Quantize quantize;
        QuantizeOption quantizeParameter = new QuantizeOption();

        quantize = new Quantize(nullCheckOff().set(IDither.class, new SubtractiveDither2(3942L)).set(quantizeParameter.setQLevel(-4.)));
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

        Assert.assertTrue(quantize.quantize(matrix, xsize, ysize));
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
        Assert.assertEquals(4.000000e+00, quantizeParameter.getBScale(), 1e-20);
        Assert.assertEquals(8.589934548e+09, quantizeParameter.getBZero(), 1e-20);
        Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
        Assert.assertEquals(-2147483580, quantizeParameter.getIntMaxValue());

        quantizeParameter = new QuantizeOption();
        quantize = new Quantize(nullCheckOff().set(IDither.class, new NoDither()).set(quantizeParameter.setQLevel(4)));
        matrix = initMatrix();
        Assert.assertFalse(quantize.quantize(matrix, 3, 2));

        quantizeParameter = new QuantizeOption();
        quantize = new Quantize(nullCheckValue().set(IDither.class, new NoDither()).set(quantizeParameter.setQLevel(4)));
        matrix = initMatrix();
        matrix[5] = NULL_VALUE;
        Assert.assertFalse(quantize.quantize(matrix, 3, 2));

        // test very small image
        quantizeParameter = new QuantizeOption();
        quantize = new Quantize(nullCheckValue().set(IDither.class, new NoDither()).set(quantizeParameter.setQLevel(4)));
        matrix = initMatrix();
        Assert.assertFalse(quantize.quantize(matrix, 1, 1));

      

    }

    @Test
    public void manyDifferentNullCases() {
        final int xsize = 12;
        final int ysize = 2;
        double[] matrix;
        Quantize quantize;
        QuantizeOption quantizeParameter;
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
            quantizeParameter = new QuantizeOption();
            quantizeParameter.setNullValue(NULL_VALUE);
            quantize =
                    new Quantize(nullCheckValue().set(IDither.class, (index % 2 == 1 ? new SubtractiveDither(3942L) : new SubtractiveDither2(3942L))).set(
                            quantizeParameter.setQLevel(4)));
            matrix = initMatrix();
            Arrays.fill(matrix, index, xsize, NULL_VALUE);
            Assert.assertTrue(quantize.quantize(matrix, xsize, ysize));
            Assert.assertArrayEquals(expectedData[expectedIndex], quantize.getIntData());

            Assert.assertEquals(1.19911703788955035348e-06, quantizeParameter.getBScale(), 1e-20);
            Assert.assertEquals(2.57508421771571829595e+03, quantizeParameter.getBZero(), 1e-20);
            Assert.assertEquals(-2147483637, quantizeParameter.getIntMinValue());
            Assert.assertEquals(-1957362476, quantizeParameter.getIntMaxValue());
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
