package codec2;

/*
  Copyright (C) 2010 Perens LLC <bruce@perens.com>

  All rights reserved.

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License version 2.1, as
  published by the Free Software Foundation.  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, see <http://www.gnu.org/licenses/>.
*/

// pack.c

final class Jpack {

	/* Compile-time constants */
	/** Size of unsigned char in bits. Assumes 8 bits-per-char. */
	private static final int WordSize = 8;

	/** Mask to pick the bit component out of bitIndex. */
	private static final int IndexMask = 0x7;

	/** Used to pick the word component out of bitIndex. */
	private static final int ShiftRight = 3;

	/** Pack a bit field into a bit string, encoding the field in Gray code.
	 *
	 * The output is an array of unsigned char data. The fields are efficiently
	 * packed into the bit string. The Gray coding is a naive attempt to reduce
	 * the effect of single-bit errors, we expect to do a better job as the
	 * codec develops.
	 *
	 * This code would be simpler if it just set one bit at a time in the string,
	 * but would hit the same cache line more often. I'm not sure the complexity
	 * gains us anything here.
	 *
	 * Although field is currently of int type rather than unsigned for
	 * compatibility with the rest of the code, indices are always expected to
	 * be >= 0.
	 *
	 * @param bitArray The output bit string.
	 * @param bitIndex Index into the string in BITS, not bytes.
	 * @param field The bit field to be packed.
	 * @param fieldWidth Width of the field in BITS, not bytes.
	 * @return java: bitIndex
	 */
	static final int pack( final byte[] bitArray, final int bitIndex, final int field, final int fieldWidth )
	{
		return pack_natural_or_gray( bitArray, bitIndex, field, fieldWidth, true );
	}

	/**
	 *
	 * @param bitArray The output bit string.
	 * @param bitIndex Index into the string in BITS, not bytes.
	 * @param field The bit field to be packed.
	 * @param fieldWidth Width of the field in BITS, not bytes.
	 * @param gray non-zero for gray coding
	 * @return java: bitIndex
	 */
	static final int pack_natural_or_gray( final byte[] bitArray, int bitIndex, int field, int fieldWidth, final boolean gray )
	{
		if( gray ) {
			/* Convert the field to Gray code */
			field = (field >> 1) ^ field;
		}

		do {
			// final int bI = bitIndex;
			final int bitsLeft = WordSize - (bitIndex & IndexMask);
			final int sliceWidth = bitsLeft < fieldWidth ? bitsLeft : fieldWidth;
			final int wordIndex = bitIndex >>> ShiftRight;

			bitArray[wordIndex] |= ((byte)((field >> (fieldWidth - sliceWidth)) << (bitsLeft - sliceWidth)));

			bitIndex += sliceWidth;
			fieldWidth -= sliceWidth;
		} while( fieldWidth != 0 );
		return bitIndex;
	}

	/** Unpack a field from a bit string, converting from Gray code to binary.
	 *
	 * @param bitArray The input bit string.
	 * @param bitIndex Index into the string in BITS, not bytes.
	 * @param fieldWidth Width of the field in BITS, not bytes.
	 */
	static final int unpack( final byte[] bitArray, final int[] bitIndex, final int fieldWidth )
	{
		return unpack_natural_or_gray( bitArray, bitIndex, fieldWidth, true );
	}

	/** Unpack a field from a bit string, to binary, optionally using
	 * natural or Gray code.
	 *
	 * @param bitArray The input bit string.
	 * @param bitIndex Index into the string in BITS, not bytes.
	 * @param fieldWidth Width of the field in BITS, not bytes.
	 * @param gray non-zero for Gray coding
	 */
	static final int unpack_natural_or_gray( final byte[] bitArray, final int[] bitIndex, int fieldWidth, final boolean gray )
	{
		int bI = bitIndex[0];// java
		int field = 0;

		do {
			final int bitsLeft = WordSize - (bI & IndexMask);
			final int sliceWidth = bitsLeft <= fieldWidth ? bitsLeft : fieldWidth;
			// TODO java: check if need (bitArray[bI >>> ShiftRight] & 0xff)
			field |= (((bitArray[bI >>> ShiftRight] >>> (bitsLeft - sliceWidth)) & ((1 << sliceWidth) - 1)) << (fieldWidth - sliceWidth));

			bI += sliceWidth;
			fieldWidth -= sliceWidth;
		} while( fieldWidth != 0 );
		bitIndex[0] = bI;// java

		if( gray ) {
			/* Convert from Gray code to binary. Works for maximum 8-bit fields. */
			int t = field ^ (field >>> 8);
			t ^= (t >>> 4);
			t ^= (t >>> 2);
			t ^= (t >>> 1);
			return t;
		}
		// else {
			// t = field;
		//}

		return field;
	}
}