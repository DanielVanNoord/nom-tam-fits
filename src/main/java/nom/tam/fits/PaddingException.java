package nom.tam.fits;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2015 nom-tam-fits
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

/**
 * This exception is thrown if an error is found reading the padding following a
 * valid FITS HDU. This padding is required by the FITS standard, but some FITS
 * writes forego writing it. To access such data users can use something like:
 * <code>
 *     Fits f = new Fits("somefile");
 *     try {
 *          f.read();
 *     } catch (PaddingException e) {
 *          f.addHDU(e.getHDU());
 *     }
 * </code> to ensure that a truncated HDU is included in the FITS object.
 * Generally the FITS file have already added any HDUs prior to the truncatd
 * one.
 */
public class PaddingException extends FitsException {

    /**
     * serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The HDU where the error happened.
     */
    private BasicHDU<?> truncatedHDU;

    /**
     * When the error is thrown, the data object being read must be supplied. We
     * initially create a dummy header for this. If someone is reading the
     * entire HDU, then they can trap the exception and set the header to the
     * appropriate value.
     * 
     * @param data
     *            the data that was not padded.
     * @throws FitsException
     *             if the data could not be used to craete a hdu.
     */
    public PaddingException(Data data) throws FitsException {
        super("data not padded");
        this.truncatedHDU = FitsFactory.HDUFactory(data.getKernel());
        // We want to use the original Data object... so
        this.truncatedHDU = FitsFactory.HDUFactory(this.truncatedHDU.getHeader(), data);
    }

    public PaddingException(String msg, Data data) throws FitsException {
        super(msg);
        this.truncatedHDU = FitsFactory.HDUFactory(data.getKernel());
        this.truncatedHDU = FitsFactory.HDUFactory(this.truncatedHDU.getHeader(), data);
    }

    public BasicHDU<?> getTruncatedHDU() {
        return this.truncatedHDU;
    }

    void updateHeader(Header hdr) throws FitsException {
        this.truncatedHDU = FitsFactory.HDUFactory(hdr, this.truncatedHDU.getData());
    }
}
