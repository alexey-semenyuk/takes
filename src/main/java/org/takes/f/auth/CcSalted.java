/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.f.auth;

import com.jcabi.aspects.Tv;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import lombok.EqualsAndHashCode;

/**
 * Salted codec.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.1
 */
@EqualsAndHashCode(of = "origin")
public final class CcSalted implements Codec {

    /**
     * Random generator.
     */
    private static final Random RND = new SecureRandom();

    /**
     * Original codec.
     */
    private final transient Codec origin;

    /**
     * Ctor.
     * @param codec Original
     */
    public CcSalted(final Codec codec) {
        this.origin = codec;
    }

    @Override
    public String encode(final Identity identity) throws IOException {
        return new String(
            CcSalted.salt(
                this.origin.encode(identity).getBytes()
            )
        );
    }

    @Override
    public Identity decode(final String text) throws IOException {
        Identity idt;
        try {
            idt = this.origin.decode(
                new String(CcSalted.unsalt(text.getBytes()))
            );
        } catch (final CcSalted.Fault ex) {
            idt = Identity.ANONYMOUS;
        }
        return idt;
    }

    /**
     * Salt the string.
     * @param text Original text to salt
     * @return Salted string
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static byte[] salt(final byte[] text) {
        final byte size = (byte) CcSalted.RND.nextInt(Tv.TEN);
        final byte[] output = new byte[text.length + (int) size + 2];
        output[0] = size;
        byte sum = (byte) 0;
        for (int idx = 0; idx < (int) size; ++idx) {
            output[idx + 1] = (byte) CcSalted.RND.nextInt();
            sum += output[idx + 1];
        }
        System.arraycopy(text, 0, output, (int) size + 1, text.length);
        output[output.length - 1] = sum;
        return output;
    }

    /**
     * Un-salt the string.
     * @param text Salted text
     * @return Original text
     * @throws CcSalted.Fault If fails
     */
    private static byte[] unsalt(final byte[] text) throws CcSalted.Fault {
        if (text.length == 0) {
            throw new CcSalted.Fault("empty input");
        }
        final int size = text[0];
        if (text.length < size + 2) {
            throw new CcSalted.Fault(
                String.format(
                    "not enough bytes for salt, length is %d while %d required",
                    text.length, size + 2
                )
            );
        }
        byte sum = (byte) 0;
        for (int idx = 0; idx < size; ++idx) {
            sum += text[idx + 1];
        }
        if (text[text.length - 1] != sum) {
            throw new CcSalted.Fault(
                String.format(
                    "checksum %d failure, while %d expected",
                    text[text.length - 1], sum
                )
            );
        }
        final byte[] output = new byte[text.length - size - 2];
        System.arraycopy(text, size + 1, output, 0, output.length);
        return output;
    }

    /**
     * Thrown when there is a fault with unsalting.
     */
    private static final class Fault extends Exception {
        /**
         * Serialization marker.
         */
        private static final long serialVersionUID = 0x7529FA781EDA1479L;
        /**
         * Public ctor.
         * @param cause The cause of it
         */
        Fault(final String cause) {
            super(cause);
        }
    }

}