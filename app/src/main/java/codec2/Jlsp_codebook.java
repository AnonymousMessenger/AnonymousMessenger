package codec2;


/** describes each codebook  */
final class Jlsp_codebook {
	/** dimension of vector */
	final int     k;
	/** number of bits in m */
	final int     log2m;
	/** elements in codebook */
	// final int     m;// java: removed. use cb.length
	/** The elements */
	final float[] cb;
	//
	/**
	 *
	 * @param dim dimension of vector
	 * @param numbits number of bits in m
	 * @param elems elements in codebook. java: use cb.length
	 * @param codebook The elements
	 */
	Jlsp_codebook(final int dim, final int numbits,/* final int elems,*/ final float[] codebook) {
		k = dim;
		log2m = numbits;
		// m = elems;
		cb = codebook;
	}
}
