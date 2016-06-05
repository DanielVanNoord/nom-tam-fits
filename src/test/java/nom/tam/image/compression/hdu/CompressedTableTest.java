package nom.tam.image.compression.hdu;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 1996 - 2016 nom-tam-fits
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

import static nom.tam.fits.header.Standard.XTENSION_BINTABLE;

import java.io.File;
import java.util.Random;

import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.Standard;
import nom.tam.fits.util.BlackBoxImages;
import nom.tam.util.Cursor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CompressedTableTest {

    private final double[][][] doubles = new double[50][5][5];

    public CompressedTableTest() {
        super();
        Random random = new Random();
        for (int i1 = 0; i1 < this.doubles.length; i1++) {
            for (int i2 = 0; i2 < this.doubles[i1].length; i2++) {
                for (int i3 = 0; i3 < this.doubles[i1][i2].length; i3++) {
                    this.doubles[i1][i2][i3] = random.nextDouble();
                }
            }
        }
    }

    @Before
    public void setup() {
        System.setProperty("compressed.table.experimental", "true");

    }

    @After
    public void teardown() {
        System.clearProperty("compressed.table.experimental");
    }

    @Test
    public void testBasicBinaryTable() throws Exception {

        Fits fitsComp = new Fits("src/test/resources/nom/tam/table/comp/testBinaryTable.fits.fz");
        CompressedTableHDU cfitsioTable = (CompressedTableHDU) fitsComp.getHDU(1);

        cfitsioTable.info(System.out);

        BinaryTableHDU binaryTable2HDU = cfitsioTable.asBinaryTableHDU();

        Fits fits = new Fits();
        fits.addHDU(binaryTable2HDU);
        fits.write(new File("target/testBinaryTable_uncompressed.fits"));
        fits.close();

        CompressedTableHDU compressed = CompressedTableHDU.fromBinaryTableHDU(binaryTable2HDU, 10).compress();
        compressed.compress();
        fits = new Fits();
        fits.addHDU(compressed);
        fits.write(new File("target/testBinaryTable_recompressed.fits"));
        fits.close();

        fits = new Fits("target/testBinaryTable_uncompressed.fits");
        Header header = fits.getHDU(1).getHeader();
        Cursor<String, HeaderCard> iter = header.iterator();
        assertStringCard(Standard.XTENSION, XTENSION_BINTABLE, iter.next());
        assertIntCard(Standard.BITPIX, 8, iter.next());
        assertIntCard(Standard.NAXIS, 2, iter.next());
        assertIntCard(Standard.NAXISn.n(1), 200, iter.next());
        assertIntCard(Standard.NAXISn.n(2), 50, iter.next());
        assertIntCard(Standard.PCOUNT, 0, iter.next());
        assertIntCard(Standard.GCOUNT, 1, iter.next());
        assertIntCard(Standard.TFIELDS, 1, iter.next());
        // the order of the next two fields is not fix
        assertStringCard(Standard.TFORMn.n(1), "25D", header.card(Standard.TFORMn.n(1)).card());
        assertStringCard(Standard.TDIMn.n(1), "(5,5)", header.card(Standard.TDIMn.n(1)).card());
        fits.close();

        fits = new Fits("target/testBinaryTable_recompressed.fits");
        header = fits.getHDU(1).getHeader();
        iter = header.iterator();
        assertStringCard(Standard.XTENSION, XTENSION_BINTABLE, iter.next());
        assertIntCard(Standard.BITPIX, 8, iter.next());
        assertIntCard(Standard.NAXIS, 2, iter.next());
        assertIntCard(Standard.NAXISn.n(1), 16, iter.next());
        assertIntCard(Standard.NAXISn.n(2), 5, iter.next());
        assertIntCard(Standard.PCOUNT, 1380, iter.next());
        assertIntCard(Standard.GCOUNT, 1, iter.next());
        assertIntCard(Standard.TFIELDS, 1, iter.next());
        // the order of the next two fields is not fix
        assertStringCard(Standard.TFORMn.n(1), "1QB", header.card(Standard.TFORMn.n(1)).card());
        assertStringCard(Standard.TDIMn.n(1), "(5,5)", header.card(Standard.TDIMn.n(1)).card());
        fits.close();
    }

    @Test
    public void testCompressedVarTable() throws Exception {
        String compressedVarTable = BlackBoxImages.getBlackBoxImage("map_one_source_a_level_1_cal.fits.fz");
        Fits fitsComp = new Fits(compressedVarTable);
        CompressedTableHDU cfitsioTable = (CompressedTableHDU) fitsComp.getHDU(1);

        cfitsioTable.info(System.out);

        BinaryTableHDU binaryTable2HDU = cfitsioTable.asBinaryTableHDU();
        Fits fits = new Fits();
        fits.addHDU(binaryTable2HDU);
        fits.write(new File("target/map_one_source_a_level_1_cal_uncompressed.fits"));
        fits.close();
        fitsComp.close();

        fits = new Fits("target/map_one_source_a_level_1_cal_uncompressed.fits");
        BinaryTableHDU table = (BinaryTableHDU) fits.getHDU(1);

        Assert.assertNotNull(table); // table could be read.

        Assert.assertEquals(21, table.getNCols());
        Assert.assertEquals(1, table.getNRows());

        String originalVarTable = BlackBoxImages.getBlackBoxImage("map_one_source_a_level_1_cal.fits");
        Fits fitsOrg = new Fits(originalVarTable);
        Header orgHeader = fitsOrg.getHDU(1).getHeader();

        Assert.assertEquals(orgHeader.getSize(), table.getHeader().getSize());
        Cursor<String, HeaderCard> iterator = orgHeader.iterator();
        while (iterator.hasNext()) {
            HeaderCard headerCard = (HeaderCard) iterator.next();
            String uncompressed = table.getHeader().getStringValue(headerCard.getKey());
            String original = orgHeader.getStringValue(headerCard.getKey());
            if (uncompressed != null || original != null) {
                Assert.assertEquals(original, uncompressed);
            }
        }
        fitsOrg.close();
    }

    private void assertIntCard(IFitsHeader expectedKey, int expectedValue, HeaderCard card) {
        Assert.assertEquals(expectedKey.key(), card.getKey());
        Assert.assertEquals(Integer.valueOf(expectedValue), card.getValue(Integer.class, -1));
    }

    private void assertStringCard(IFitsHeader expectedKey, String expectedValue, HeaderCard card) {
        Assert.assertEquals(expectedKey.key(), card.getKey());
        Assert.assertEquals(expectedValue, card.getValue());
    }
}
