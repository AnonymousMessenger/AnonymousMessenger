package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: quantise.c
  AUTHOR......: David Rowe
  DATE CREATED: 31/5/92

  Quantisation functions for the sinusoidal coder.

\*---------------------------------------------------------------------------*/

/*
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

// quantise.c

final class Jquantise {
	static final int WO_BITS     = 7;
	// private static final int WO_LEVELS   = (1 << WO_BITS);
	// private static final int WO_DT_BITS  = 3;

	static final int E_BITS      = 5;
	// private static final int E_LEVELS    = (1 << E_BITS);// FIXME never usees
	private static final float E_MIN_DB  = -10.0f;
	private static final float E_MAX_DB  = 40.0f;

	static final int LSP_SCALAR_INDEXES    = 10;
	static final int LSPD_SCALAR_INDEXES   = 10;
	static final int LSP_PRED_VQ_INDEXES   = 3;
	// private static final int LSP_DIFF_FREQ_INDEXES = 5;// FIXME never usees
	// private static final int LSP_DIFF_TIME_BITS    = 7;// FIXME never usees

	// private static final int LSPDT_ALL   = 0;// FIXME never usees
	// private static final int LSPDT_LOW   = 1;// FIXME never usees
	// private static final int LSPDT_HIGH  = 2;// FIXME never usees

	static final int WO_E_BITS   = 8;

	static final float LPCPF_GAMMA = 0.5f;
	static final float LPCPF_BETA  = 0.2f;

	/** grid spacing for LSP root searches */
	private static final float LSP_DELTA1 = 0.01f;

/*---------------------------------------------------------------------------*\

                             FUNCTIONS

\*---------------------------------------------------------------------------*/

	static final int lsp_bits(final int i) {
		return Jcodebook.lsp_cb[i].log2m;
	}

	static final int lspd_bits(final int i) {
		return Jcodebookd.lsp_cbd[i].log2m;
	}

// #ifndef CORTEX_M4
	static final int mel_bits(final int i) {
		return Jcodebookmel.mel_cb[i].log2m;
	}

	static final int lspmelvq_cb_bits(final int i) {
		return Jcodebooklspmelvq.lspmelvq_cb[i].log2m;
	}
// #endif

/* #ifdef __EXPERIMENTAL__
	private static final int lspdt_bits(int i) {
		return lsp_cbdt[i].log2m;
	}
#endif */

	static final int lsp_pred_vq_bits(final int i) {
		return Jcodebookjvm.lsp_cbjvm[i].log2m;
	}

	/**

	  quantise_init

	  Loads the entire LSP quantiser comprised of several vector quantisers
	  (codebooks).

	*/
	static final void quantise_init()
	{
	}

	/**

	  quantise

	  Quantises vec by choosing the nearest vector in codebook cb, and
	  returns the vector index.  The squared error of the quantised vector
	  is added to se.

	  @param cb current VQ codebook
	  @param vec vector to quantise
	  @param w weighting vector
	  @param k dimension of vectors
	  @param m size of codebook
	  @param se accumulated squared error. java: removed, unuses

	*/
	private static final int quantise( final float[] cb, final float vec[], final int voffset, final float w[], final int k, final int m)//, final float[] se)
	{
		int besti = 0;/* best index so far		*/
		float beste = 1E32f;/* best error so far		*/
		for( int j = 0, n = 0; j < m; j++, n += k ) {
			float e = 0.0f;/* current error		*/
			for( int i = 0, p = n, v = voffset; i < k; i++, p++ ) {
				float diff = cb[p] - vec[v++];
				// e += Math.pow( diff * w[i], 2.0 );// FIXME why pow?
				diff *= w[i];
				diff *= diff;
				e += diff;
			}
			if( e < beste ) {
				beste = e;
				besti = j;
			}
		}
		// se[0] += beste;
		return besti;
	}



	/**

	  encode_lspds_scalar()

	  Scalar/VQ LSP difference quantiser.

	*/
	static final void encode_lspds_scalar(final int indexes[], final float lsp[], final int order)
	{
		final float wt[] = new float[order];
		for( int i = 0; i < order; i++ ) {
			wt[i] = 1.0f;
		}

		/* convert from radians to Hz so we can use human readable
		frequencies */

		// FIXME why need lsp_hz?
		/* final float lsp_hz[] = new float[order];
		for( int i = 0; i < order; i++ ) {
			lsp_hz[i] = (4000.0f / Jdefines.PI) * lsp[i];
		}*/

		//printf("\n");

		// final float lsp__hz[] = new float[order];// FIXME why need lsp__hz ?
		float lsp__hz = 0f;
		final float dlsp[] = new float[order];
		// final float dlsp_[] = new float[order];// FIXME why need dlsp_ ?
		// final float[] se = new float[1];// never uses
		wt[0] = 1.0f;
		for( int i = 0; i < order; i++ ) {

			/* find difference from previous qunatised lsp */

			if( i != 0 ) {
				// dlsp[i] = lsp_hz[i] - lsp__hz[i - 1];
				dlsp[i] = (4000.0f / Jdefines.PI) * lsp[i] - lsp__hz;
			} else {
				// dlsp[0] = lsp_hz[0];
				dlsp[0] = (4000.0f / Jdefines.PI) * lsp[0];
			}

			final Jlsp_codebook cb_i = Jcodebookd.lsp_cbd[i];// java
			final int k = cb_i.k;
			final int m = cb_i.cb.length;// cb_i.m;
			final float[] cb = cb_i.cb;
			indexes[i] = quantise( cb, dlsp, i, wt, k, m );//, se );
			final float dlsp_ = cb[ indexes[i] * k ];


			if( i != 0 ) {
				lsp__hz += dlsp_;
			} else {
				lsp__hz = dlsp_;
			}

			//printf("%d lsp %3.2f dlsp %3.2f dlsp_ %3.2f lsp_ %3.2f\n", i, lsp_hz[i], dlsp[i], dlsp_[i], lsp__hz[i]);
		}
	}


	static final void decode_lspds_scalar(final float lsp_[], final int indexes[], final int order)
	{
		// final float lsp__hz[] = new float[order];// FIXME why need lsp__hz ?
		float lsp__hz = 0f;
		// final float dlsp_[] = new float[order];// FIXME why need dlsp_ ?

		final Jlsp_codebook[] codebooks = Jcodebookd.lsp_cbd;// java
		for( int i = 0; i < order; i++ ) {
			final Jlsp_codebook cb_i = codebooks[i];// java
			final int k = cb_i.k;
			final float[] cb = cb_i.cb;
			final float dlsp_ = cb[ indexes[i] * k ];

			if( i != 0 ) {
				lsp__hz += dlsp_;
			} else {
				lsp__hz = dlsp_;
			}

			lsp_[i] = (Jdefines.PI / 4000.0f) * lsp__hz;

			//printf("%d dlsp_ %3.2f lsp_ %3.2f\n", i, dlsp_[i], lsp__hz[i]);
		}

	}

	// private static final int MAX_ENTRIES = 16384;// FIXME never usees

	private static final void compute_weights( final float[] x, final float[] w, int ndim )
	{
		float v1 = x[0];
		float v2 = x[1] - v1;
		w[0] = ( v1 <= v2 ? v1 : v2 );
		ndim--;// java
		for( int i = 1; i < ndim; i++ ) {
			v1 = x[i];// java
			v2 = x[i + 1] - v1;
			v1 -= x[i - 1];
			w[i] = (v1 <= v2 ? v1 : v2);
		}
		v1 = x[ndim];// java
		v2 = Jdefines.PI - v1;
		v1 -= x[ndim - 1];
		w[ndim] = (v1 <= v2 ? v1 : v2);

		for( int i = 0; i <= ndim; i++ ) {
			w[i] = 1.f / (.01f + w[i]);
		//w[0]*=3;
		//w[1]*=2;
		}
	}

	private static final int find_nearest( final float[] codebook, final int nb_entries, final float[] x, final int ndim)
	{
		float min_dist = 1e15f;
		int nearest = 0;

		for( int i = 0, k = 0; i < nb_entries; i++, k += ndim )
		{
			float dist = 0f;
			for( int j = 0, n = k; j < ndim; j++, n++ ) {
				final float v = x[j] - codebook[n];// java
				dist += v * v;
			}
			if( dist < min_dist )
			{
				min_dist = dist;
				nearest = i;
			}
		}
		return nearest;
	}

	private static final int find_nearest_weighted( final float[] codebook, final int nb_entries, final float[] x, final float[] w, final int ndim)
	{
		float min_dist = 1e15f;
		int nearest = 0;

		for( int i = 0, k = 0; i < nb_entries; i++, k += ndim )
		{
			float dist = 0f;
			for( int j = 0, n = k; j < ndim; j++, n++ ) {
				final float v = x[j] - codebook[n];// java
				dist += w[j] * v * v;
			}
			if( dist < min_dist )
			{
				min_dist = dist;
				nearest = i;
			}
		}
		return nearest;
	}

	/* java: never uses
	private static final void lspjvm_quantise(final float[] x, final float[] xq, final int order)
	{
		final float w[] = new float[order];
		float v1 = x[0];// java
		float v2 = x[1] - v1;// java
		w[0] = ( v1 <= v2 ? v1 : v2 );
		for( int i = 1, ie = order - 1; i < ie; i++ ) {
			v1 = x[i];// java
			v2 = x[i + 1] - v1;
			v1 -= x[i - 1];
			w[i] = ( v1 <= v2 ? v1 : v2 );
		}
		v1 = x[order - 1];// java
		v2 = Jdefines.PI - v1;
		v1 -= x[order - 2];
		w[order - 1] = (v1 <= v2 ? v1 : v2);

		compute_weights( x, w, order );

		final float[] codebook1 = Jcodebookjvm.lsp_cbjvm[0].cb;
		int n1 = find_nearest( codebook1, codebook1.length, x, order ) * order;// java changed

		final float err[] = new float[order];
		for( int i = 0; i < order; i++, n1++ )
		{
			v1 = codebook1[n1];
			xq[i] = v1;
			err[i] = x[i] - v1;
		}
		final float err2[] = new float[order];
		final float err3[] = new float[order];
		final float w2[] = new float[order];
		final float w3[] = new float[order];
		final int order2 = order >> 1;// java
		for( int i = 0, i2 = 0; i < order2; i++ )
		{
			err2[i] = err[i2];
			w2[i] = w[i2];
			err3[i] = err[++i2];
			w3[i] = w[i2++];
		}
		final float[] codebook2 = Jcodebookjvm.lsp_cbjvm[1].cb;
		int n2 = (find_nearest_weighted( codebook2, codebook2.length, err2, w2, order2 ) * order) >> 1;// java changed
		final float[] codebook3 = Jcodebookjvm.lsp_cbjvm[2].cb;
		int n3 = (find_nearest_weighted( codebook3, codebook3.length, err3, w3, order2 ) * order) >> 1;// java changed

		for( final int i = 0; i < order2; )
		{
			int i2 = i << 1;
			xq[i2++] += codebook2[n2++];
			xq[i2] += codebook3[n3++];
		}
	}
	*/

// #ifndef CORTEX_M4
/* simple (non mbest) 6th order LSP MEL VQ quantiser.  Returns MSE of result */

	// java: uses only for development
	/* private static final float lspmelvq_quantise(final float[] x, final float[] xq, final int order)
	{
		// assert(order == lspmelvq_cb[0].k);

		final float[] codebook1 = Jcodebooklspmelvq.lspmelvq_cb[0].cb;
		int n1 = find_nearest( codebook1, Jcodebooklspmelvq.lspmelvq_cb[0].m, x, order ) * order;// java changed

		final float err[] = new float[order];
		final float tmp[] = new float[order];
		for( int i = 0; i < order; i++ ) {
			final float t = codebook1[n1++];
			tmp[i] = t;
			err[i] = x[i] - t;
		}

		final float[] codebook2 = Jcodebooklspmelvq.lspmelvq_cb[1].cb;
		int n2 = find_nearest( codebook2, Jcodebooklspmelvq.lspmelvq_cb[1].m, err, order ) * order;// java changed

		for( int i = 0; i < order; i++ ) {
			float t = tmp[i];
			t += codebook2[n2++];
			tmp[i] = t;
			err[i] = x[i] - t;
		}

		final float[] codebook3 = Jcodebooklspmelvq.lspmelvq_cb[2].cb;
		int n3 = find_nearest( codebook3, Jcodebooklspmelvq.lspmelvq_cb[2].m, err, order ) * order;// java changed

		float mse = 0.0f;
		for( int i = 0; i < order; i++ ) {
			float t = tmp[i];
			t += codebook3[n3++];
			final float e = x[i] - t;
			mse += e * e;
			xq[i] = t;// java
		}

		// for( int i = 0; i < order; i++ ) {// java: moved up inside the loop
		//	xq[i] = tmp[i];
		//}

		return mse;
	} */

	private static final int MBEST_STAGES = 4;

	private static final class JMBEST_LIST {
		/** index of each stage that lead us to this error */
		private final int index[] = new int[MBEST_STAGES];// = 0
		private float error = 1E32f;
	}

	private static final class JMBEST {
		// final int entries;/* number of entries in mbest list */// java: list.length
		private final JMBEST_LIST[] list;
		//
		// private final JMBEST mbest_create(final int entries) {
		private JMBEST( final int entries ) {
			// assert(entries > 0);
			// final JMBEST mbest = new JMBEST();
			// assert(mbest != NULL);

			// mbest.entries = entries;
			final JMBEST_LIST[] m_list = new JMBEST_LIST[ entries ];
			this.list = m_list;
			// assert(mbest.list != NULL);

			for( int i = 0; i < entries; i++ ) {
				m_list[i] = new JMBEST_LIST();// java
				/* for( int j = 0; j < MBEST_STAGES; j++ ) {
					list[i].index[j] = 0;// java already zeroed
				}
				list[i].error = 1E32f; */// java moved to JMBEST_LIST constructor
			}

			// return mbest;
		}
		/**

		  mbest_insert

		  Insert the results of a vector to codebook entry comparison. The
		  list is ordered in order or error, so those entries with the
		  smallest error will be first on the list.

		*/
		private final void mbest_insert(final int index[], final float error) {
			final JMBEST_LIST mlist[] = this.list;
			final int entries = this.list.length;// mbest.entries;

			boolean found = false;
			for( int i = 0; i < entries && ! found; i++ ) {
				final JMBEST_LIST mbest = mlist[i];// java
				if( error < mbest.error ) {
					found = true;
					for( int j = entries - 1; j > i; j-- ) {
						mlist[j] = mlist[j - 1];
					}
					final int[] ind = mbest.index;// java
					for( int j = 0; j < MBEST_STAGES; j++ ) {
						ind[j] = index[j];
					}
					mbest.error = error;
				}
			}
		}
		/**

		  mbest_search

		  Searches vec[] to a codebbook of vectors, and maintains a list of the mbest
		  closest matches.

		  @param cb VQ codebook to search
		  @param vec target vector
		  @param w weighting vector
		  @param k dimension of vector
		  @param m number on entries in codebook
		  @param mbest list of closest matches
		  @param index indexes that lead us here

		*/
		private final void mbest_search( final float[] cb, final float vec[], final float w[], final int k, final int m, final int index[] )
		{
			for( int j = 0; j < m; j++ ) {
				float e = 0.0f;
				for( int i = 0, n = j * k; i < k; i++, n++ ) {
					float diff = cb[n] - vec[i];
					// e += powf( diff * w[i], 2.0 );// FIXME why pow?
					diff *= w[i];
					e += ( diff * diff );
				}
				index[0] = j;
				mbest_insert( index, e );
			}
		}
	}

	/* private static final void mbest_destroy(final JMBEST mbest) {// java: use mbest = null
		assert(mbest != NULL);
		free(mbest.list);
		free(mbest);
	}*/

	/*
	private static final void mbest_print(final char title[], final JMBEST mbest) {
		System.err.printf("%s\n", title );
		for( int i = 0; i < mbest.list.length; i++) {
			for( int j = 0; j < MBEST_STAGES; j++ ) {
				System.err.printf("  %4d ", mbest.list[i].index[j] );
			}
			System.err.printf(" %f\n", mbest.list[i].error );
		}
	}
	*/

	/** 3 stage VQ LSP quantiser useing mbest search.  Design and guidance kindly submitted by Anssi, OH3GDD */
	static final float lspmelvq_mbest_encode(final int[] indexes, final float[] x, final float[] xq, final int ndim, final int mbest_entries)
	{
		final float w[] = new float[ndim];
		for( int i = 0; i < ndim; i++ ) {
			w[i] = 1.0f;
		}

		JMBEST mbest_stage1 = new JMBEST( mbest_entries );
		JMBEST mbest_stage2 = new JMBEST( mbest_entries );
		JMBEST mbest_stage3 = new JMBEST( mbest_entries );
		int index[] = new int[MBEST_STAGES];
		/* for( int i = 0; i < MBEST_STAGES; i++ ) {// java: already eroed
			index[i] = 0;
		}*/

		/* Stage 1 */
		final float[] codebook1 = Jcodebooklspmelvq.lspmelvq_cb[0].cb;
		mbest_stage1.mbest_search( codebook1, x, w, ndim, codebook1.length, index );
		//mbest_print("Stage 1:", mbest_stage1);

		/* Stage 2 */
		final float[] codebook2 = Jcodebooklspmelvq.lspmelvq_cb[1].cb;
		final float target[] = new float[ndim];
		JMBEST_LIST list[] = mbest_stage1.list;// java
		for( int j = 0; j < mbest_entries; j++ ) {
			int n1 = list[j].index[0];
			index[1] = n1;
			n1 *= ndim;// java
			for( int i = 0; i < ndim; i++ ) {
				target[i] = x[i] - codebook1[n1++];
			}
			mbest_stage2.mbest_search( codebook2, target, w, ndim, codebook2.length, index );
		}
		//mbest_print("Stage 2:", mbest_stage2);

		/* Stage 3 */
		final float[] codebook3 = Jcodebooklspmelvq.lspmelvq_cb[2].cb;
		list = mbest_stage2.list;// java
		for( int j = 0; j < mbest_entries; j++ ) {
			int n1 = list[j].index[1];
			index[2] = n1;
			n1 *= ndim;// java
			int n2 = list[j].index[0];
			index[1] = n2;
			n2 *= ndim;// java
			for( int i = 0; i < ndim; i++ ) {
				target[i] = x[i] - codebook1[n1++] - codebook2[n2++];
			}
			mbest_stage3.mbest_search( codebook3, target, w, ndim, codebook3.length, index );
		}
		//mbest_print("Stage 3:", mbest_stage3);

		index = mbest_stage3.list[0].index;// java
		int n1 = index[2];
		int n2 = index[1];
		int n3 = index[0];
		indexes[0] = n1; indexes[1] = n2; indexes[2] = n3;
		n1 *= ndim;// java changed
		n2 *= ndim;// java changed
		n3 *= ndim;// java changed
		float mse = 0.0f;
		for( int i = 0; i <ndim; i++ ) {
			final float tmp = codebook1[n1++] + codebook2[n2++] + codebook3[n3++];
			mse += (x[i] - tmp) * (x[i] - tmp);
			xq[i] = tmp;
		}

		mbest_stage1 = null;
		mbest_stage2 = null;
		mbest_stage3 = null;

		return mse;
	}


	static final void lspmelvq_decode(final int[] indexes, final float[] xq, final int ndim)
	{
		final float[] codebook1 = Jcodebooklspmelvq.lspmelvq_cb[0].cb;
		final float[] codebook2 = Jcodebooklspmelvq.lspmelvq_cb[1].cb;
		final float[] codebook3 = Jcodebooklspmelvq.lspmelvq_cb[2].cb;

		int n1 = indexes[0] * ndim;
		int n2 = indexes[1] * ndim;
		int n3 = indexes[2] * ndim;
		for( int i = 0; i < ndim; i++ ) {
			xq[i] = codebook1[n1++] + codebook2[n2++] + codebook3[n3++];
		}
	}
// #endif // #ifndef CORTEX_M4


	static final int check_lsp_order(final float lsp[], final int order)
	{
		int swaps = 0;

		for( int i = 1; i < order; i++ ) {
			if( lsp[i] < lsp[i - 1] ) {
				//fprintf(stderr, "swap %d\n",i);
				swaps++;
				final float tmp = lsp[i - 1];
				lsp[i - 1] = lsp[i] - 0.1f;
				lsp[i] = tmp + 0.1f;
				i = 1; /* start check again, as swap may have caused out of order */
			}
		}

		return swaps;
	}

	// java: never uses
	/* private static final void force_min_lsp_dist(final float lsp[], final int order)
	{
		for( int i = 1; i < order; i++ ) {
			if( (lsp[i] - lsp[i - 1]) < 0.01f ) {
				lsp[i] += 0.01f;
			}
		}
	}*/


	/**

	   lpc_post_filter()

	   Applies a post filter to the LPC synthesis filter power spectrum
	   Pw, which supresses the inter-formant energy.

	   The algorithm is from p267 (Section 8.6) of "Digital Speech",
	   edited by A.M. Kondoz, 1994 published by Wiley and Sons.  Chapter 8
	   of this text is on the MBE vocoder, and this is a freq domain
	   adaptation of post filtering commonly used in CELP.

	   I used the Octave simulation lpcpf.m to get an understanding of the
	   algorithm.

	   Requires two more FFTs which is significantly more MIPs.  However
	   it should be possible to implement this more efficiently in the
	   time domain.  Just not sure how to handle relative time delays
	   between the synthesis stage and updating these coeffs.  A smaller
	   FFT size might also be accetable to save CPU.

	   TODO:
	   [ ] sync var names between Octave and C version
	   [ ] doc gain normalisation
	   [ ] I think the first FFT is not rqd as we do the same
		   thing in aks_to_M2().

	*/
	private static final void lpc_post_filter(final Jkiss_fft_state fft_fwd_cfg, final Jkiss_fft_cpx Pw[], final float ak[],
		final int order, /*final boolean dump,*/ final float beta, final float gamma, final boolean bass_boost, final float E)
	{
		// PROFILE_VAR(tstart, tfft1, taw, tfft2, tww, tr);

		// PROFILE_SAMPLE(tstart);

		/* Determine weighting filter spectrum W(exp(jw)) ---------------*/
		final Jkiss_fft_cpx x[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];   /* input to FFTs                */
		final Jkiss_fft_cpx Ww[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];  /* weighting spectrum           */
		for( int i = 0; i < Jdefines.FFT_ENC; i++) {
			x[i] = new Jkiss_fft_cpx();// java: already zeroed
			// x[i].real = 0.0;
			// x[i].imag = 0.0;
			Ww[i] = new Jkiss_fft_cpx();
		}

		x[0].r = ak[0];
		float coeff = gamma;
		for( int i = 1; i <= order; i++) {
			x[i].r = ak[i] * coeff;
			coeff *= gamma;
		}

		fft_fwd_cfg.kiss_fft( x, Ww );

		// PROFILE_SAMPLE_AND_LOG(tfft2, taw, "        fft2");
		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			final Jkiss_fft_cpx w = Ww[i];// java
			float real = w.r;
			real *= real;
			float image = w.i;
			image *= image;
			Ww[i].r = real + image;
		}

		// PROFILE_SAMPLE_AND_LOG(tww, tfft2, "        Ww");

		/* Determined combined filter R = WA ---------------------------*/
		final float Rw[] = new float[Jdefines.FFT_ENC];  /* R = WA                       */
		float max_Rw = 0.0f;
		float min_Rw = 1E32f;
		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			final float r = (float)Math.sqrt( (double)(Ww[i].r * Pw[i].r) );
			Rw[i] = r;
			if( r > max_Rw ) {
				max_Rw = r;
			}
			if( r < min_Rw ) {
				min_Rw = r;
			}
		}

		// PROFILE_SAMPLE_AND_LOG(tr, tww, "        R");

/* # ifdef DUMP
		if (dump) {
			dump_Rw(Rw);
		}
#endif */

		/* create post filter mag spectrum and apply ------------------*/

		/* measure energy before post filtering */

		float e_before = 1E-4f;
		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			e_before += Pw[i].r;
		}

		/* apply post filter and measure energy  */

/* # ifdef DUMP
		if (dump) {
			dump_Pwb(Pw);
		}
#endif */

		final double dbeta = (double)beta;// java
		float e_after = 1E-4f;
		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			final float Pfw = (float)Math.pow( (double)Rw[i], dbeta );
			Pw[i].r *= Pfw * Pfw;
			e_after += Pw[i].r;
		}
		float gain = e_before / e_after;

		/* apply gain factor to normalise energy, and LPC Energy */

		gain *= E;
		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			Pw[i].r *= gain;
		}

		if( bass_boost ) {
			/* add 3dB to first 1 kHz to account for LP effect of PF */

			for( int i = 0; i < Jdefines.FFT_ENC / 8; i++ ) {
				Pw[i].r *= 1.4f * 1.4f;
			}
		}

		// PROFILE_SAMPLE_AND_LOG2(tr, "        filt");
	}


	/**

	   aks_to_M2()

	   Transforms the linear prediction coefficients to spectral amplitude
	   samples.  This function determines A(m) from the average energy per
	   band using an FFT.

	   @param fft_fwd_cfg
	   @param ak LPC's
	   @param order
	   @param model sinusoidal model parameters for this frame
	   @param E energy term
	   @param dump true to dump sample to dump file
	   @param sim_pf true to simulate a post filter
	   @param pf true to enable actual LPC post filter
	   @param bass_boost enable LPC filter 0-1kHz 3dB boost
	   @param beta
	   @param gamma LPC post filter parameters
	   @param Aw output power spectrum
	   @return signal to noise ratio for this frame in dB

	*/
	static final float aks_to_M2(
		final Jkiss_fft_state fft_fwd_cfg,
		final float         ak[],
		final int           order,
		final JMODEL        model,
		final float         E,
		// final float        []snr,// java: returned
		// final boolean       dump,
		final boolean       sim_pf,
		final boolean       pf,
		final boolean       bass_boost,
		final float         beta,
		final float         gamma,
		final Jkiss_fft_cpx Aw[]
		)
	{
		// PROFILE_VAR(tstart, tfft, tpw, tpf);

		// PROFILE_SAMPLE(tstart);

		final float r = Jdefines.TWO_PI / (Jdefines.FFT_ENC);/* no. rads/bin */

		/* Determine DFT of A(exp(jw)) --------------------------------------------*/
		final Jkiss_fft_cpx a[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC]; /* input to FFT for power spectrum */
		final Jkiss_fft_cpx Pw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC]; /* output power spectrum */
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			a[i] = new Jkiss_fft_cpx();
			// a[i].real = 0.0;
			// a[i].imag = 0.0;
			Pw[i] = new Jkiss_fft_cpx();
			// Pw[i].real = 0.0;
			// Pw[i].imag = 0.0;
		}

		for( int i = 0; i <= order; i++ ) {
			a[i].r = ak[i];
		}
		fft_fwd_cfg.kiss_fft( a, Aw );

		// PROFILE_SAMPLE_AND_LOG(tfft, tstart, "      fft");

		/* Determine power spectrum P(w) = E/(A(exp(jw))^2 ------------------------*/

		for( int i = 0; i < Jdefines.FFT_ENC / 2; i++ ) {
			final Jkiss_fft_cpx ai = Aw[i];// java
			float real = ai.r;
			real *= real;
			float image = ai.i;
			image *= image;
			Pw[i].r = 1.0f / (real + image + 1E-6f);
		}

		// PROFILE_SAMPLE_AND_LOG(tpw, tfft, "      Pw");

		if( pf ) {
			lpc_post_filter( fft_fwd_cfg, Pw, ak, order, /* dump,*/ beta, gamma, bass_boost, E );
		} else {
			for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
				Pw[i].r *= E;
			}
		}

		// PROFILE_SAMPLE_AND_LOG(tpf, tpw, "      LPC post filter");

/* #ifdef DUMP
		if( dump ) {
			dump_Pw(Pw);
		}
#endif */

		/* Determine magnitudes from P(w) ----------------------------------------*/

		/* when used just by decoder {A} might be all zeroes so init signal
		 and noise to prevent log(0) errors */

		float signal = 1E-30f;
		float noise = 1E-32f;

		final float model_a[] = model.A;// java
		final float wo_r = model.Wo / r;// java
		for( int m = 1, count = model.L; m <= count; m++ ) {
			final int am = (int)((m - 0.5f) * wo_r + 0.5f);/* limit of current band */
			final int bm = (int)((m + 0.5f) * wo_r + 0.5f);/* limit of current band */
			float Em = 0.0f;/* energy in band */

			for( int i = am; i < bm; i++ ) {
				Em += Pw[i].r;
			}
			float Am = (float)Math.sqrt( (double)Em );/* spectral amplitude sample */

			final float a_m = model_a[m];// java
			signal += a_m * a_m;
			noise  += (a_m - Am) * (a_m - Am);

			/* This code significantly improves perf of LPC model, in
			 particular when combined with phase0.  The LPC spectrum tends
			 to track just under the peaks of the spectral envelope, and
			 just above nulls.  This algorithm does the reverse to
			 compensate - raising the amplitudes of spectral peaks, while
			 attenuating the null.  This enhances the formants, and
			 supresses the energy between formants. */

			if( sim_pf ) {
				if( Am > a_m ) {
					Am *= 0.7f;
				}
				if( Am < a_m ) {
					Am *= 1.4f;
				}
			}

			model_a[m] = Am;
		}
		return /*snr[0] =*/ 10.0f * (float)Math.log10( (double)(signal / noise) );

		// PROFILE_SAMPLE_AND_LOG2(tpf, "      rec");
	}

	/**

	  FUNCTION....: encode_Wo()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Encodes Wo using a WO_LEVELS quantiser.

	*/
	static final int encode_Wo(final float Wo, final int bits)
	{
		int Wo_levels = 1 << bits;
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;

		final float norm = (Wo - Wo_min) / (Wo_max - Wo_min);
		int index = (int)Math.floor( (double)(Wo_levels * norm + 0.5f) );
		if( index < 0 ) {
			index = 0;
		}
		Wo_levels--;
		if( index > Wo_levels ) {
			index = Wo_levels;
		}

		return index;
	}

	/**

	  FUNCTION....: decode_Wo()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Decodes Wo using a WO_LEVELS quantiser.

	*/
	static final float decode_Wo(final int index, final int bits)
	{
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;
		final int Wo_levels = 1 << bits;

		final float step = (Wo_max - Wo_min) / Wo_levels;
		final float Wo   = Wo_min + step * (index);

		return Wo;
	}

	/**

	  FUNCTION....: encode_log_Wo()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Encodes Wo in the log domain using a WO_LEVELS quantiser.

	*/
	static final int encode_log_Wo(final float Wo, final int bits)
	{
		int Wo_levels = 1 << bits;
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;

		final float norm = (float)((Math.log10( Wo ) - Math.log10( Wo_min )) / (Math.log10( Wo_max ) - Math.log10( Wo_min )));
		int index = (int)Math.floor( (double)(Wo_levels * norm + 0.5f) );
		if( index < 0 ) {
			index = 0;
		}
		Wo_levels--;
		if( index > Wo_levels ) {
			index = Wo_levels;
		}

		return index;
	}

	/**

	  FUNCTION....: decode_log_Wo()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Decodes Wo using a WO_LEVELS quantiser in the log domain.

	*/
	static final float decode_log_Wo(final int index, final int bits)
	{
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;
		final int   Wo_levels = 1 << bits;

		final double step = (Math.log10( Wo_max ) - Math.log10( Wo_min )) / (double)Wo_levels;
		final double Wo   = Math.log10( Wo_min ) + step * (double)index;

		return (float)Math.pow( 10.0, Wo );
	}

	// java: never uses
	/**

	  FUNCTION....: encode_Wo_dt()
	  AUTHOR......: David Rowe
	  DATE CREATED: 6 Nov 2011

	  Encodes Wo difference from last frame.

	*/
	/* private static final int encode_Wo_dt(final float Wo, final float prev_Wo)
	{
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;

		final float norm = (Wo - prev_Wo) / (Wo_max - Wo_min);
		int index = (int)Math.floor( (double)(WO_LEVELS * norm + 0.5f) );
		//printf("ENC index: %d ", index);

		// hard limit

		final int max_index = (1 << (WO_DT_BITS - 1)) - 1;
		final int min_index = - (max_index + 1);
		if( index > max_index ) {
			index = max_index;
		}
		if( index < min_index ) {
			index = min_index;
			//printf("max_index: %d  min_index: %d hard index: %d ",
			//	   max_index,  min_index, index);
		}

		// mask so that only LSB WO_DT_BITS remain, bit WO_DT_BITS is the sign bit

		final int mask = ((1 << WO_DT_BITS) - 1);
		index &= mask;
		//printf("mask: 0x%x index: 0x%x\n", mask, index);

		return index;
	} */

	// java: never uses
	/**

	  FUNCTION....: decode_Wo_dt()
	  AUTHOR......: David Rowe
	  DATE CREATED: 6 Nov 2011

	  Decodes Wo using WO_DT_BITS difference from last frame.

	*/
	/* private static final float decode_Wo_dt(int index, final float prev_Wo)
	{
		final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;

		// sign extend index

		//printf("DEC index: %d ");
		if( (index & (1 << (WO_DT_BITS - 1))) != 0 ) {
			final int mask = ~((1 << WO_DT_BITS) - 1);
			index |= mask;
		}
		//printf("DEC mask: 0x%x  index: %d \n", mask, index);

		final float step = (Wo_max - Wo_min) / WO_LEVELS;
		float Wo   = prev_Wo + step * (index);

		// bit errors can make us go out of range leading to all sorts of
		// probs like seg faults

		if( Wo > Wo_max ) {
			Wo = Wo_max;
		}
		if( Wo < Wo_min ) {
			Wo = Wo_min;
		}

		return Wo;
	} */

	/**

	  FUNCTION....: speech_to_uq_lsps()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Analyse a windowed frame of time domain speech to determine LPCs
	  which are the converted to LSPs for quantisation and transmission
	  over the channel.

	*/
	static final float speech_to_uq_lsps(final float lsp[], final float ak[], final float Sn[], final float w[], final int order )
	{
		final float Wn[] = new float[Jdefines.M];
		final float R[] = new float[order + 1];

		float e = 0.0f;
		for( int i = 0; i < Jdefines.M; i++ ) {
			Wn[i] = Sn[i] * w[i];
			e += Wn[i] * Wn[i];
		}

		/* trap 0 energy case as LPC analysis will fail */

		final float pi_order = Jdefines.PI / order;// java
		if( e == 0.0f ) {
			for( int i = 0; i < order; i++ ) {
				lsp[i] = pi_order * (float)i;
			}
			return 0.0f;
		}

		Jlpc.autocorrelate( Wn, R, Jdefines.M, order );
		Jlpc.levinson_durbin( R, ak, order );

		float E = 0.0f;
		for( int i = 0; i <= order; i++ ) {
			E += ak[i] * R[i];
		}

		/* 15 Hz BW expansion as I can't hear the difference and it may help
		   help occasional fails in the LSP root finding.  Important to do this
		   after energy calculation to avoid -ve energy values.
		*/

		for( int i = 0; i <= order; i++ ) {
			ak[i] *= Math.pow( 0.994, (double)i );
		}

		final float roots = Jlsp.lpc_to_lsp( ak, order, lsp, 5, LSP_DELTA1 );
		if( roots != order ) {
			/* if root finding fails use some benign LSP values instead */
			for( int i = 0; i < order; i++ ) {
				lsp[i] = pi_order * (float)i;
			}
		}

		return E;
	}

	/**

	  FUNCTION....: encode_lsps_scalar()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Thirty-six bit sclar LSP quantiser. From a vector of unquantised
	  (floating point) LSPs finds the quantised LSP indexes.

	*/
	static final void encode_lsps_scalar(final int indexes[], final float lsp[], final int order)
	{
		final float lsp_hz[] = new float[order];

		/* convert from radians to Hz so we can use human readable
		frequencies */

		for( int i = 0; i < order; i++ ) {
			lsp_hz[i] = (4000.0f / Jdefines.PI) * lsp[i];
		}

		/* scalar quantisers */

		// final float[] se = new float[1];// java: never uses
		final float wt[] = { 1.0f };
		final Jlsp_codebook[] codebooks = Jcodebook.lsp_cb;// java
		for( int i = 0; i < order; i++ ) {
			final Jlsp_codebook codebook = codebooks[i];
			indexes[i] = quantise( codebook.cb, lsp_hz, i, wt, codebook.k, codebook.cb.length );// , se );
		}
	}

	/**

	  FUNCTION....: decode_lsps_scalar()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  From a vector of quantised LSP indexes, returns the quantised
	  (floating point) LSPs.

	*/
	static final void decode_lsps_scalar(final float lsp[], final int indexes[], final int order)
	{
		// final float lsp_hz[] = new float[order];// FIXME why need lsp_hz?

		final Jlsp_codebook[] codebooks = Jcodebook.lsp_cb;// java
		/* for( int i = 0; i < order; i++ ) {
			final Jlsp_codebook codebook = codebooks[i];
			lsp_hz[i] = codebook.cb[ indexes[i] * codebook.k ];
		}*/

		/* convert back to radians */

		for( int i = 0; i < order; i++ ) {
			// lsp[i] = (Jdefines.PI / 4000.0f) * lsp_hz[i];
			final Jlsp_codebook codebook = codebooks[i];
			lsp[i] = (Jdefines.PI / 4000.0f) * codebook.cb[ indexes[i] * codebook.k ];
		}
	}


// #ifndef CORTEX_M4

	/**

	  FUNCTION....: encode_mels_scalar()
	  AUTHOR......: David Rowe
	  DATE CREATED: April 2015

	  Low bit rate mel coeff encoder.

	*/
	static final void encode_mels_scalar(final int indexes[], final float mels[], final int order)
	{
		// final float se[] = new float[1];// java: never uses
		final float dmel[] = new float[1];

		/* scalar quantisers */

		final Jlsp_codebook[] codebooks = Jcodebookmel.mel_cb;// java
		final float wt[] = { 1.0f };
		for( int i = 0; i < order; i++ ) {
			final float[] cb = codebooks[i].cb;
			if( (i & 1) != 0 ) {
				/* on odd mels quantise difference */
				final float mel_ = codebooks[i - 1].cb[ indexes[i - 1] ];
				dmel[0] = mels[i] - mel_;
				indexes[i] = quantise( cb, dmel, 0, wt, 1, cb.length );// , se );
				//printf("%d mel: %f mel_: %f dmel: %f index: %d\n", i, mels[i], mel_, dmel, indexes[i]);
			}
			else {
				indexes[i] = quantise( cb, mels, i, wt, 1, cb.length );// , se );
				//printf("%d mel: %f dmel: %f index: %d\n", i, mels[i], 0.0, indexes[i]);
			}
		}
	}


	/**

	  FUNCTION....: decode_mels_scalar()
	  AUTHOR......: David Rowe
	  DATE CREATED: April 2015

	  From a vector of quantised mel indexes, returns the quantised
	  (floating point) mels.

	*/
	static final void decode_mels_scalar(final float mels[], final int indexes[], final int order)
	{
		final Jlsp_codebook[] codebooks = Jcodebookmel.mel_cb;// java
		for( int i = 0; i < order; i++ ) {
			final float[] cb = codebooks[i].cb;
			if( (i & 1) != 0 ) {
				/* on odd mels quantise difference */
				mels[i] = mels[i - 1] + cb[ indexes[i] ];
			} else {
				mels[i] = cb[ indexes[i] ];
			}
		}

	}

// #endif // #ifndef CORTEX_M4

// #ifdef __EXPERIMENTAL__

	/**

	  FUNCTION....: encode_lsps_diff_freq_vq()
	  AUTHOR......: David Rowe
	  DATE CREATED: 15 November 2011

	  Twenty-five bit LSP quantiser.  LSPs 1-4 are quantised with scalar
	  LSP differences (in frequency, i.e difference from the previous
	  LSP).  LSPs 5-10 are quantised with a VQ trained generated using
	  vqtrainjnd.c

	*/
	/* private static final void encode_lsps_diff_freq_vq(final int indexes[], final float lsp[], final int order)
	{
		final int   i, k, m;
		final float lsp_hz[] = new float[order];
		final float lsp__hz[] = new float[order];
		final float dlsp[] = new float[order];
		final float dlsp_[] = new float[order];
		final float wt[] = new float[order];
		float[] cb;
		final float[] se = new float[1];

		for( i = 0; i < order; i++ ) {
			wt[i] = 1.0f;
		}

		// convert from radians to Hz so we can use human readable
		// frequencies

		for( i = 0; i < order; i++ ) {
			lsp_hz[i] = (4000.0f / Jdefines.PI) * lsp[i];
		}

		// scalar quantisers for LSP differences 1..4

		wt[0] = 1.0f;
		for( i = 0; i < 4; i++ ) {
			if( i != 0 ) {
				dlsp[i] = lsp_hz[i] - lsp__hz[i-1];
			} else {
				dlsp[0] = lsp_hz[0];
			}

			k = Jcodebookd.lsp_cbd[i].k;
			m = Jcodebookd.lsp_cbd[i].m;
			cb = Jcodebookd.lsp_cbd[i].cb;
			indexes[i] = quantise( cb, dlsp, i, wt, k, m, se );
			dlsp_[i] = cb[ indexes[i] * k ];

			if( i != 0 ) {
				lsp__hz[i] = lsp__hz[i - 1] + dlsp_[i];
			} else {
				lsp__hz[0] = dlsp_[0];
			}
		}

		// VQ LSPs 5,6,7,8,9,10

		k = Jcodebookjnd.lsp_cbjnd[4].k;
		m = Jcodebookjnd.lsp_cbjnd[4].m;
		cb = Jcodebookjnd.lsp_cbjnd[4].cb;
		indexes[4] = quantise( cb, lsp_hz, 4, wt, 4, k, m, se );
	} */


	/**

	  FUNCTION....: decode_lsps_diff_freq_vq()
	  AUTHOR......: David Rowe
	  DATE CREATED: 15 Nov 2011

	  From a vector of quantised LSP indexes, returns the quantised
	  (floating point) LSPs.

	*/
	/* private static final void decode_lsps_diff_freq_vq(final float lsp_[], final int indexes[], final int order)
	{
		final int   i, k, m;
		final float dlsp_[] = new float[order];
		final float lsp__hz[] = new float[order];
		float[] cb;

		// scalar LSP differences

		for(i = 0; i<4; i++) {
			cb = Jcodebookd.lsp_cbd[i].cb;
			dlsp_[i] = cb[ indexes[i] ];
			if( i != 0 ) {
				lsp__hz[i] = lsp__hz[i - 1] + dlsp_[i];
			} else {
				lsp__hz[0] = dlsp_[0];
			}
		}

		// VQ

		k = Jcodebookjnd.lsp_cbjnd[4].k;
		m = Jcodebookjnd.lsp_cbjnd[4].m;
		cb = Jcodebookjnd.lsp_cbjnd[4].cb;
		for( i = 4; i < order; i++ ) {
			lsp__hz[i] = cb[ indexes[4] * k + i - 4 ];
		}

		// convert back to radians

		for( i = 0; i < order; i++ ) {
			lsp_[i] = (Jdefines.PI / 4000.0f) * lsp__hz[i];
		}
	} */


	/**

	  FUNCTION....: encode_lsps_diff_time()
	  AUTHOR......: David Rowe
	  DATE CREATED: 12 Sep 2012

	  Encode difference from preious frames's LSPs using
	  3,3,2,2,2,2,1,1,1,1 scalar quantisers (18 bits total).

	*/
	/* private static final void encode_lsps_diff_time(final int indexes[],
				   final float lsps[],
				   final float lsps__prev[],
				   final int order)
	{
		final int   i, k, m;
		final float lsps_dt[] = new float[order];
		final float wt[] = new float[LPC_MAX];
		float[] cb;
		final float[] se = new float[1];

		// Determine difference in time and convert from radians to Hz so
		//   we can use human readable frequencies

		for( i = 0; i < order; i++ ) {
			lsps_dt[i] = (4000.f / Jdefines.PI) * (lsps[i] - lsps__prev[i]);
		}

		// scalar quantisers

		wt[0] = 1.0f;
		for( i = 0; i < order; i++ ) {
			k = Jcodebookdt.lsp_cbdt[i].k;
			m = Jcodebookdt.lsp_cbdt[i].m;
			cb = Jcodebookdt.lsp_cbdt[i].cb;
			indexes[i] = quantise(cb, lsps_dt, i, wt, k, m, se);
		}
	} */


	/**

	  FUNCTION....: decode_lsps_diff_time()
	  AUTHOR......: David Rowe
	  DATE CREATED: 15 Nov 2011

	  From a quantised LSP indexes, returns the quantised
	  (floating point) LSPs.

	*/
	/* private static final void decode_lsps_diff_time(
		final float lsps_[],
		final int indexes[],
		final float lsps__prev[],
		final int order)
	{
		final int i, k;
		final int m;
		float[] cb;

		for( i = 0; i < order; i++ ) {
			lsps_[i] = lsps__prev[i];
		}

		for( i = 0; i < order; i++ ) {
			k = Jcodebookdt.lsp_cbdt[i].k;
			cb = Jcodebookdt.lsp_cbdt[i].cb;
			lsps_[i] += (Jdefines.PI / 4000.0f) * cb[ indexes[i] * k ];
		}
	} */
// #endif // #ifdef __EXPERIMENTAL__

	/**

	  FUNCTION....: encode_lsps_vq()
	  AUTHOR......: David Rowe
	  DATE CREATED: 15 Feb 2012

	  Multi-stage VQ LSP quantiser developed by Jean-Marc Valin.

	*/
	static final void encode_lsps_vq(final int[] indexes, final float[] x, final float[] xq, int order)
	{
		final Jlsp_codebook[] codebooks = Jcodebookjvm.lsp_cbjvm;// java
		final float[] codebook1 = codebooks[0].cb;
		final float[] codebook2 = codebooks[1].cb;
		final float[] codebook3 = codebooks[2].cb;

		final float w[] = new float[order];
		float v1 = x[0];// java
		float v2 = x[1] = v1;
		w[0] = ( v1 <= v2 ? v1 : v2 );
		final int order1 = order - 1;// java
		for( int i = 1; i < order1; i++ ) {
			v1 = x[i];
			v2 = x[i + 1] - v1;
			v1 -= x[i - 1];
			w[i] = ( v1 <= v2 ? v1 : v2 );
		}
		v1 = x[order1];
		v2 = Jdefines.PI - v1;
		v1 -= x[order - 2];
		w[order1] = ( v1 <= v2 ? v1 : v2 );

		compute_weights( x, w, order );

		int n1 = find_nearest( codebook1, codebook1.length, x, order );
		indexes[0] = n1;
		n1 *= order;// java

		final float err[] = new float[order];
		for( int i = 0; i < order; i++ )
		{
			xq[i]  = codebook1[n1++];
			err[i] = x[i] - xq[i];
		}
		final float err2[] = new float[order];
		final float err3[] = new float[order];
		final float w2[] = new float[order];
		final float w3[] = new float[order];
		order >>= 1;// java
		for( int i = 0, i2 = 0; i < order; i++ )
		{
			err2[i] = err[i2];
			w2[i] = w[i2];
			err3[i] = err[++i2];
			w3[i] = w[i2++];
		}
		final int n2 = find_nearest_weighted( codebook2, codebook2.length, err2, w2, order );
		final int n3 = find_nearest_weighted( codebook3, codebook3.length, err3, w3, order );

		indexes[1] = n2;
		indexes[2] = n3;
	}


	/**

	  FUNCTION....: decode_lsps_vq()
	  AUTHOR......: David Rowe
	  DATE CREATED: 15 Feb 2012

	*/
	static final void decode_lsps_vq(final int[] indexes, final float[] xq, int order, final int stages)
	{
		final Jlsp_codebook[] codebooks = Jcodebookjvm.lsp_cbjvm;// java
		final float[] codebook1 = codebooks[0].cb;

		int n1 = indexes[0] * order;// java changed

		for( int i = 0; i < order; i++ ) {
			xq[i] = codebook1[n1++];
		}

		if( stages != 1 ) {
			final float[] codebook2 = codebooks[1].cb;
			final float[] codebook3 = codebooks[2].cb;
			int n2 = (indexes[1] * order) >> 1;// java changed
			int n3 = (indexes[2] * order) >> 1;// java changed
			order >>= 1;// java
			for( int i = 0; i < order; i++ ) {
				int i2 = i << 1;
				xq[i2++] += codebook2[n2++];
				xq[i2] += codebook3[n3++];
			}
		}
	}


	/**

	  FUNCTION....: bw_expand_lsps()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Applies Bandwidth Expansion (BW) to a vector of LSPs.  Prevents any
	  two LSPs getting too close together after quantisation.  We know
	  from experiment that LSP quantisation errors < 12.5Hz (25Hz step
	  size) are inaudible so we use that as the minimum LSP separation.

	*/
	static final void bw_expand_lsps(final float lsp[], final int order, float min_sep_low, float min_sep_high)
	{
		min_sep_low *= (Jdefines.PI / 4000.0f);// java
		for( int i = 1; i < 4; i++ ) {

			if( (lsp[i] - lsp[i - 1]) < min_sep_low ) {
				lsp[i] = lsp[i - 1] + min_sep_low;
			}

		}

		/* As quantiser gaps increased, larger BW expansion was required
		   to prevent twinkly noises.  This may need more experiment for
		   different quanstisers.
		*/
		min_sep_high *= (Jdefines.PI / 4000.0f);// java
		for( int i = 4; i < order; i++ ) {
			if( lsp[i] - lsp[i - 1] < min_sep_high ) {
				lsp[i] = lsp[i - 1] + min_sep_high;
			}
		}
	}

	/* java: never uses
	private static final void bw_expand_lsps2(final float lsp[], final int order)
	{
		for( int i = 1; i < 4; i++ ) {

			if( (lsp[i] - lsp[i - 1]) < 100.0f * (Jdefines.PI / 4000.0f) ) {
				lsp[i] = lsp[i - 1] + 100.0f * ( Jdefines.PI / 4000.0f );
			}

		}

		// As quantiser gaps increased, larger BW expansion was required
		// to prevent twinkly noises.  This may need more experiment for
		// different quanstisers.

		for( int i = 4; i < order; i++ ) {
			if( lsp[i] - lsp[i - 1] < 200.0f * (Jdefines.PI / 4000.0f) ) {
				lsp[i] = lsp[i - 1] + 200.0f * (Jdefines.PI / 4000.0f);
			}
		}
	} */

	/**

	  FUNCTION....: locate_lsps_jnd_steps()
	  AUTHOR......: David Rowe
	  DATE CREATED: 27/10/2011

	  Applies a form of Bandwidth Expansion (BW) to a vector of LSPs.
	  Listening tests have determined that "quantising" the position of
	  each LSP to the non-linear steps below introduces a "just noticable
	  difference" in the synthesised speech.

	  This operation can be used before quantisation to limit the input
	  data to the quantiser to a number of discrete steps.

	  This operation can also be used during quantisation as a form of
	  hysteresis in the calculation of quantiser error.  For example if
	  the quantiser target of lsp1 is 500 Hz, candidate vectors with lsp1
	  of 515 and 495 Hz sound effectively the same.

	*/
	public static final void locate_lsps_jnd_steps(final float lsps[], final int order)
	{
		// assert(order == 10);

		/* quantise to 25Hz steps */
		final float k = Jdefines.PI / 4000.0f;// java
		float step = 25f;
		float k_step = k * step;// java
		for( int i = 0; i < 2; i++ ) {
			final float lsp_hz = (float)Math.floor( lsps[i] / k_step + 0.5f ) * step;
			lsps[i] = lsp_hz * k;
			if( i != 0 ) {
				if( lsps[i] == lsps[i - 1] ) {
					lsps[i] += k_step;
				}
			}
		}

		/* quantise to 50Hz steps */

		step = 50f;
		k_step = k * step;// java
		for( int i = 2; i < 4; i++ ) {
			final float lsp_hz = (float)Math.floor( lsps[i] / k_step + 0.5f ) * step;
			lsps[i] = lsp_hz * k;
			if( i != 0 ) {
				if( lsps[i] == lsps[i - 1] ) {
					lsps[i] += k_step;
				}
			}
		}

		/* quantise to 100Hz steps */

		step = 100f;
		k_step = k * step;// java
		for( int i = 4; i < 10; i++ ) {
			final float lsp_hz = (float)Math.floor( lsps[i] / k_step + 0.5f ) * step;
			lsps[i] = lsp_hz * k;
			if( i != 0 ) {
				if( lsps[i] == lsps[i - 1] ) {
					lsps[i] += k_step;
				}
			}
		}
	}

	/**

	  FUNCTION....: encode_energy()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Encodes LPC energy using an E_LEVELS quantiser.

	*/
	static final int encode_energy(float e, final int bits)
	{
		int e_levels = 1 << bits;

		e = 10.0f * (float)Math.log10( (double)e );
		final float norm = (e - E_MIN_DB) / (E_MAX_DB - E_MIN_DB);
		int index = (int)Math.floor( (double)(e_levels * norm + 0.5f) );
		if( index < 0 ) {
			index = 0;
		}
		e_levels--;
		if( index > e_levels ) {
			index = e_levels;
		}

		return index;
	}

	/**

	  FUNCTION....: decode_energy()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Decodes energy using a E_LEVELS quantiser.

	*/
	static final float decode_energy(final int index, final int bits)
	{
		final float e_levels = (float)(1 << bits);

		final float step = (E_MAX_DB - E_MIN_DB) / e_levels;
		float e = E_MIN_DB + step * (index);
		e = (float)Math.pow( 10.0, (double)(e / 10.0f) );

		return e;
	}

// #ifdef NOT_USED
	/**

	  FUNCTION....: decode_amplitudes()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Given the amplitude quantiser indexes recovers the harmonic
	  amplitudes.

	*/
	/* private static final float decode_amplitudes(final Jkiss_fft_state fft_fwd_cfg,
		final JMODEL model,
		final float  ak[],
		final int    lsp_indexes[],
		final int    energy_index,
		final float  lsps[],
		final float[] e
		)
	{
		decode_lsps_scalar( lsps, lsp_indexes, Jdefines.LPC_ORD );
		bw_expand_lsps( lsps, Jdefines.LPC_ORD );
		lsp_to_lpc( lsps, ak, Jdefines.LPC_ORD );
		e[0] = decode_energy( energy_index );
		// aks_to_M2( ak, Jdefines.LPC_ORD, model, e[0], &snr, 1, false, false, 1 );
		final float snr = aks_to_M2( ak, Jdefines.LPC_ORD, model, e[0], 1, false, false, 1 );
		apply_lpc_correction( model );

		return snr;
	} */
// #endif

	private static final float ge_coeff[/* 2 */] = { 0.8f, 0.9f };

	private static final void compute_weights2(final float[] x, final float[] xp, final float[] w)
	{
		final float x1 = x[1];// java
		float w0 = 30f;
		float w1 = 1f;
		if( x1 < 0f )
		{
			w0 *= .6f;
			w1 *= .3f;
		}
		if( x1 < -10f )
		{
			w0 *= .3f;
			w1 *= .3f;
		}
		float abs_dx = x[0] - xp[0];// java
		if( abs_dx < 0 ) {
			abs_dx = -abs_dx;
		}
		/* Higher weight if pitch is stable */
		if( abs_dx < .2f )
		{
			w0 *= 2f;
			w1 *= 1.5f;
		} else if( abs_dx > .5f ) /* Lower if not stable */
		{
			w0 *= .5f;
		}

		final float xp1 = xp[1];// java
		/* Lower weight for low energy */
		if( x1 < xp1 - 10f )
		{
			w1 *= .5f;
		}
		if( x1 < xp1 - 20f )
		{
			w1 *= .5f;
		}

		//w[0] = 30;
		//w[1] = 1;

		/* Square the weights because it's applied on the squared error */
		w0 *= w0;
		w1 *= w1;

		w[0] = w0;
		w[1] = w1;
	}

	/**

	  FUNCTION....: quantise_WoE()
	  AUTHOR......: Jean-Marc Valin & David Rowe
	  DATE CREATED: 29 Feb 2012

	  Experimental joint Wo and LPC energy vector quantiser developed by
	  Jean-Marc Valin.  Exploits correlations between the difference in
	  the log pitch and log energy from frame to frame.  For example
	  both the pitch and energy tend to only change by small amounts
	  during voiced speech, however it is important that these changes be
	  coded carefully.  During unvoiced speech they both change a lot but
	  the ear is less sensitve to errors so coarser quantisation is OK.

	  The ear is sensitive to log energy and loq pitch so we quantise in
	  these domains.  That way the error measure used to quantise the
	  values is close to way the ear senses errors.

	  See http://jmspeex.livejournal.com/10446.html

	  @return java: new value of the e

	*/
	/* private static final float quantise_WoE(final JMODEL model, final float e, final float xq[])
	{// java: never uses
		final float x[] = new float[2];
		x[0] = (float)Math.log10( (double)((model.Wo / Jdefines.PI) * 4000.0f / 50.0f) ) / (float)Math.log10( 2. );
		x[1] = 10.0f * (float)Math.log10( (double)(1e-4f + e) );

		final float w[] = new float[2];
		compute_weights2( x, xq, w );

		final float err[] = new float[2];
		final Jlsp_codebook codebook = Jcodebookge.ge_cb[0];// java
		final int ndim = codebook.k;
		for( int i = 0; i < ndim; i++ ) {
			err[i] = x[i] - ge_coeff[i] * xq[i];
		}
		final float[] codebook1 = codebook.cb;
		// final int nb_entries = codebook.m;
		int n1 = find_nearest_weighted( codebook1, codebook1.length, err, w, ndim ) * ndim;// java changed

		for( int i = 0; i < ndim; i++, n1++ )
		{
			xq[i] = ge_coeff[i] * xq[i] + codebook1[n1];
			err[i] -= codebook1[n1];
		}

		// x = log2(4000*Wo/(PI*50));
		// 2^x = 4000*Wo/(PI*50)
		// Wo = (2^x)*(PI*50)/4000;

		model.Wo = (float)Math.pow( 2.0, xq[0]) * (Jdefines.PI * 50.0f) / 4000.0f;

		// bit errors can make us go out of range leading to all sorts of
		// probs like seg faults

		// final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		// final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;
		if( model.Wo > Jdefines.TWO_PI / Jdefines.P_MIN ) {// if( model.Wo > Wo_max ) {
			model.Wo = Jdefines.TWO_PI / Jdefines.P_MIN;// Wo_max
		}
		if( model.Wo < Jdefines.TWO_PI / Jdefines.P_MAX ) {// if( model.Wo < Wo_min ) {
			model.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;// Wo_min
		}

		model.L = (int)(Jdefines.PI / model.Wo);// if we quantise Wo re-compute L

		// e = powf( 10.0, xq[1] / 10.0f );
		return (float)Math.pow( 10.0, (double)(xq[1] / 10.0f) );
	}
	*/
	/**

	  FUNCTION....: encode_WoE()
	  AUTHOR......: Jean-Marc Valin & David Rowe
	  DATE CREATED: 11 May 2012

	  Joint Wo and LPC energy vector quantiser developed my Jean-Marc
	  Valin.  Returns index, and updated states xq[].

	*/
	static final int encode_WoE(final JMODEL model, float e, final float xq[])
	{
		final Jlsp_codebook codebook = Jcodebookge.ge_cb[0];// java
		final float[] codebook1 = codebook.cb;
		final int     ndim = codebook.k;

		// assert((1 << WO_E_BITS) == nb_entries);

		if( e < 0.0f ) {
			e = 0f;
		}  /* occasional small negative energies due LPC round off I guess */

		final float x[] = new float[2];
		x[0] = (float)Math.log10( (double)((model.Wo / Jdefines.PI) * 4000.0f / 50.0f) ) / (float)Math.log10( 2. );
		x[1] = 10.0f * (float)Math.log10( (double)(1e-4f + e) );

		final float w[] = new float[2];
		compute_weights2( x, xq, w );

		final float err[] = new float[2];
		for( int i = 0; i < ndim; i++ ) {
			err[i] = x[i] - ge_coeff[i] * xq[i];
		}
		final int n1 = find_nearest_weighted( codebook1, codebook1.length, err, w, ndim );

		for( int i = 0, j = n1 * ndim; i < ndim; i++, j++ )
		{
			xq[i] = ge_coeff[i] * xq[i] + codebook1[j];
			err[i] -= codebook1[j];
		}

		//printf("enc: %f %f (%f)(%f) \n", xq[0], xq[1], e, 10.0*log10(1e-4 + e));
		return n1;
	}


	/**

	  FUNCTION....: decode_WoE()
	  AUTHOR......: Jean-Marc Valin & David Rowe
	  DATE CREATED: 11 May 2012

	  Joint Wo and LPC energy vector quantiser developed my Jean-Marc
	  Valin.  Given index and states xq[], returns Wo & E, and updates
	  states xq[].

	  @return java: e
	*/
	static final float decode_WoE(final JMODEL model,/* final float[] e,*/ final float xq[], final int n1)
	{
		final float[] codebook1 = Jcodebookge.ge_cb[0].cb;
		final int ndim = Jcodebookge.ge_cb[0].k;

		for( int i = 0, j = ndim * n1; i < ndim; i++, j++ )
		{
			xq[i] = ge_coeff[i] * xq[i] + codebook1[j];
		}

		//printf("dec: %f %f\n", xq[0], xq[1]);
		model.Wo = ((float)Math.pow( 2.0, xq[0] )) * (Jdefines.PI * 50.0f) / 4000.0f;

		/* bit errors can make us go out of range leading to all sorts of
		 probs like seg faults */

		// final float Wo_max = Jdefines.TWO_PI / Jdefines.P_MIN;
		// final float Wo_min = Jdefines.TWO_PI / Jdefines.P_MAX;
		if( model.Wo > Jdefines.TWO_PI / Jdefines.P_MIN ) {// if( model.Wo > Wo_max ) {
			model.Wo = Jdefines.TWO_PI / Jdefines.P_MIN;// Wo_max;
		}
		if( model.Wo < Jdefines.TWO_PI / Jdefines.P_MAX ) {// if( model.Wo < Wo_min ) {
			model.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;// Wo_min;
		}

		model.L = (int)(Jdefines.PI / model.Wo); /* if we quantise Wo re-compute L */

		// *e = powf(10.0, xq[1]/10.0);
		return (float)Math.pow( 10.0, (double)(xq[1] / 10.0f) );
	}

}