package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: nlp.c
  AUTHOR......: David Rowe
  DATE CREATED: 23/3/93

  Non Linear Pitch (NLP) estimation functions.

\*---------------------------------------------------------------------------*/

/*
  Copyright (C) 2009 David Rowe

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

// nlp.c

final class Jnlp {
/*---------------------------------------------------------------------------*\

 				DEFINES

\*---------------------------------------------------------------------------*/

	/** maximum NLP analysis window size */
	private static final int PMAX_M      = 600;
	/** notch filter parameter */
	private static final float COEFF     = 0.95f;
	/** DFT size for pitch estimation */
	private static final int PE_FFT_SIZE = 512;
	/** decimation factor */
	private static final int DEC         = 5;
	static final int SAMPLE_RATE = 8000;
	/** mathematical constant */
	private static final float PI        = 3.141592654f;
	/** threshold for local minima candidate */
	// private static final float T         = 0.1f;
	// private static final int F0_MAX      = 500;
	/** post processor constant */
	private static final float CNLP      = 0.3f;
	/** Decimation LPF order */
	private static final int NLP_NTAP    = 48;

	//#undef DUMP

/*---------------------------------------------------------------------------*\

 				GLOBALS

\*---------------------------------------------------------------------------*/

	/** 48 tap 600Hz low pass FIR filter coefficients */
	private static final float nlp_fir[] = {
		-1.0818124e-03f,
		-1.1008344e-03f,
		-9.2768838e-04f,
		-4.2289438e-04f,
		 5.5034190e-04f,
		 2.0029849e-03f,
		 3.7058509e-03f,
		 5.1449415e-03f,
		 5.5924666e-03f,
		 4.3036754e-03f,
		 8.0284511e-04f,
		-4.8204610e-03f,
		-1.1705810e-02f,
		-1.8199275e-02f,
		-2.2065282e-02f,
		-2.0920610e-02f,
		-1.2808831e-02f,
		 3.2204775e-03f,
		 2.6683811e-02f,
		 5.5520624e-02f,
		 8.6305944e-02f,
		 1.1480192e-01f,
		 1.3674206e-01f,
		 1.4867556e-01f,
		 1.4867556e-01f,
		 1.3674206e-01f,
		 1.1480192e-01f,
		 8.6305944e-02f,
		 5.5520624e-02f,
		 2.6683811e-02f,
		 3.2204775e-03f,
		-1.2808831e-02f,
		-2.0920610e-02f,
		-2.2065282e-02f,
		-1.8199275e-02f,
		-1.1705810e-02f,
		-4.8204610e-03f,
		 8.0284511e-04f,
		 4.3036754e-03f,
		 5.5924666e-03f,
		 5.1449415e-03f,
		 3.7058509e-03f,
		 2.0029849e-03f,
		 5.5034190e-04f,
		-4.2289438e-04f,
		-9.2768838e-04f,
		-1.1008344e-03f,
		-1.0818124e-03f
	};

	// typedef struct {
		private final int           m;
		/** DFT window */
		private final float         w[] = new float[PMAX_M / DEC];
		/** squared speech samples */
		private final float         sq[] = new float[PMAX_M];
		/** memory for notch filter */
		private float         mem_x = 0f, mem_y = 0f;
		/** decimation FIR filter memory */
		private final float         mem_fir[] = new float[NLP_NTAP];
		/** kiss FFT config */
		private final Jkiss_fft_state fft_cfg;
	//} NLP;

	/**

	  nlp_create()

	  Initialisation function for NLP pitch estimator.

	  @param m analysis window size

	*/
	// static final Jnlp nlp_create( final int m )
	Jnlp( final int frame_size )
	{
		// assert(m <= PMAX_M);

		/* final Jnlp nlp = new Jnlp();
		if( nlp == null ) {
			return null;
		}*/

		this.m = frame_size;
		final float[] window = this.w;
		for( int i = 0, ie = frame_size / DEC; i < ie; i++ ) {
			window[i] = 0.5f - 0.5f * (float)Math.cos( (double)(2f * PI * i / (frame_size / DEC - 1)) );
		}

		/* for( int i = 0; i < PMAX_M; i++ ) {// java: already zeroed
			nlp.sq[i] = 0.0f;
		}*/
		// nlp.mem_x = 0.0f;// java: already zeroed
		// nlp.mem_y = 0.0f;// java: already zeroed
		/* for( int i = 0; i < NLP_NTAP; i++ ) {// java: already zeroed
			nlp.mem_fir[i] = 0.0f;
		}*/

		this.fft_cfg = Jkiss_fft_state.kiss_fft_alloc( PE_FFT_SIZE, false );//, null, null );
		// assert(nlp.fft_cfg != NULL);

		// return nlp;
	}

	/**

	  nlp_destroy()

	  Shut down function for NLP pitch estimator.

	*/
	/* static final void nlp_destroy(final Jnlp nlp_state)
	{
		NLP   *nlp;
		// assert(nlp_state != NULL);
		nlp = (NLP*)nlp_state;

		KISS_FFT_FREE(nlp.fft_cfg ;
		free(nlp_state);
	}*/

	/**

	  nlp()

	  Determines the pitch in samples using the Non Linear Pitch (NLP)
	  algorithm [1]. Returns the fundamental in Hz.  Note that the actual
	  pitch estimate is for the centre of the M sample Sn[] vector, not
	  the current N sample input vector.  This is (I think) a delay of 2.5
	  frames with N=80 samples.  You should align further analysis using
	  this pitch estimate to be centred on the middle of Sn[].

	  Two post processors have been tried, the MBE version (as discussed
	  in [1]), and a post processor that checks sub-multiples.  Both
	  suffer occasional gross pitch errors (i.e. neither are perfect).  In
	  the presence of background noise the sub-multiple algorithm tends
	  towards low F0 which leads to better sounding background noise than
	  the MBE post processor.

	  A good way to test and develop the NLP pitch estimator is using the
	  tnlp (codec2/unittest) and the codec2/octave/plnlp.m Octave script.

	  A pitch tracker searching a few frames forward and backward in time
	  would be a useful addition.

	  References:

		[1] http://www.itr.unisa.edu.au/~steven/thesis/dgr.pdf Chapter 4

		@param nlp_state
		@param Sn input speech vector
		@param n frames shift (no. new samples in Sn[])
		@param pmin minimum pitch value
		@param pmax maximum pitch value
		@param pitch estimated pitch period in samples
		@param Sw Freq domain version of Sn[]
		@param W Freq domain window
		@param prev_Wo
		@return f0. java: pitch = SAMPLE_RATE / best_f0;

	*/
	final float nlp( final float Sn[], final int n, final int pmin, final int pmax,// final float[] pitch,// java: pitch = SAMPLE_RATE / best_f0;
			final Jkiss_fft_cpx Sw[], final Jkiss_fft_cpx W[], final float prev_Wo)
	{
		// PROFILE_VAR(start, tnotch, filter, peakpick, window, fft, magsq, shiftmem);

		// assert(nlp_state != NULL);
		final int frame_size = this.m;

		// PROFILE_SAMPLE(start);

		/* Square, notch filter at DC, and LP filter vector */
		final float[] nlp_sq = this.sq;// java
		for( int i = frame_size - n; i < frame_size; i++ ) {
			final float s = Sn[i];// java
			nlp_sq[i] = s * s;
		}

		for( int i = frame_size - n; i < frame_size; i++ ) {	/* notch filter at DC */
			float notch = nlp_sq[i] - this.mem_x;/* current notch filter output    */
			notch += COEFF * this.mem_y;
			this.mem_x = nlp_sq[i];
			this.mem_y = notch;
			nlp_sq[i] = notch + 1.0f;  /* With 0 input vectors to codec,
							  kiss_fft() would take a long
							  time to execute when running in
							  real time.  Problem was traced
							  to kiss_fft function call in
							  this function. Adding this small
							  constant fixed problem.  Not
							  exactly sure why. */
		}

		// PROFILE_SAMPLE_AND_LOG(tnotch, start, "      square and notch");
		final float[] fir = this.mem_fir;// java
		for( int i = frame_size - n; i < frame_size; i++ ) {	/* FIR filter vector */

			for( int j = 0; j < NLP_NTAP - 1; j++ ) {
				fir[j] = fir[j + 1];
			}
			fir[NLP_NTAP - 1] = nlp_sq[i];

			float sfq = 0.0f;// java
			for( int j = 0; j < NLP_NTAP; j++ ) {
				sfq += fir[j] * nlp_fir[j];
			}
			nlp_sq[i] = sfq;
		}

		// PROFILE_SAMPLE_AND_LOG(filter, tnotch, "      filter");

		/* Decimate and DFT */
		final Jkiss_fft_cpx fw[] = new Jkiss_fft_cpx[PE_FFT_SIZE];	    /* DFT of squared signal (input)  */
		final Jkiss_fft_cpx Fw[] = new Jkiss_fft_cpx[PE_FFT_SIZE];	    /* DFT of squared signal (output) */
		/* for( i = 0; i < PE_FFT_SIZE; i++ ) {
			fw[i].real = 0.0;
			fw[i].imag = 0.0;
		}*/
		int i = 0;
		for( final int j = frame_size / DEC; i < j; i++ ) {
			// fw[i].real = nlp->sq[i*DEC]*nlp->w[i];
			fw[i] = new Jkiss_fft_cpx( nlp_sq[i * DEC] * this.w[i], 0.f );// java
			Fw[i] = new Jkiss_fft_cpx();// java
		}
		for( ; i < PE_FFT_SIZE; i++ ) {
			fw[i] = new Jkiss_fft_cpx();// java
			Fw[i] = new Jkiss_fft_cpx();// java
		}
		// PROFILE_SAMPLE_AND_LOG(window, filter, "      window");
/* #ifdef DUMP
		dump_dec(Fw);
#endif */

		this.fft_cfg.kiss_fft( fw, Fw );
		// PROFILE_SAMPLE_AND_LOG(fft, window, "      fft");

		for( i = 0; i < PE_FFT_SIZE; i++ ) {
			Fw[i].r = Fw[i].r * Fw[i].r + Fw[i].i * Fw[i].i;
		}

		// PROFILE_SAMPLE_AND_LOG(magsq, fft, "      mag sq");
/* #ifdef DUMP
		dump_sq(nlp.sq);
		dump_Fw(Fw);
#endif */

		/* find global peak */

		float gmax = 0.0f;
		int gmax_bin = PE_FFT_SIZE * DEC / pmax;
		i = PE_FFT_SIZE * DEC / pmax;
		for( final int ie = PE_FFT_SIZE * DEC / pmin; i <= ie; i++ ) {
			if( Fw[i].r > gmax ) {
				gmax = Fw[i].r;
				gmax_bin = i;
			}
		}

		// PROFILE_SAMPLE_AND_LOG(peakpick, magsq, "      peak pick");

		// #define POST_PROCESS_MBE
//#ifdef POST_PROCESS_MBE
		// best_f0 = post_process_mbe( Fw, pmin, pmax, gmax, Sw, W, prev_Wo );
//#else
		final float best_f0 = post_process_sub_multiples( Fw, pmin, pmax, gmax, gmax_bin, prev_Wo );
//#endif

		// PROFILE_SAMPLE_AND_LOG(shiftmem, peakpick,  "      post process");

		/* Shift samples in buffer to make room for new samples */

		for( int j = 0, je = frame_size - n, k = n; j < je; j++, k++ ) {
			nlp_sq[j] = nlp_sq[k];
		}

		/* return pitch and F0 estimate */

		// pitch[0] = (float)SAMPLE_RATE / best_f0;

		// PROFILE_SAMPLE_AND_LOG2(shiftmem,  "      shift mem");

		// PROFILE_SAMPLE_AND_LOG2(start,  "      nlp int");

		return best_f0;
	}

	/**

	  post_process_sub_multiples()

	  Given the global maximma of Fw[] we search integer submultiples for
	  local maxima.  If local maxima exist and they are above an
	  experimentally derived threshold (OK a magic number I pulled out of
	  the air) we choose the submultiple as the F0 estimate.

	  The rational for this is that the lowest frequency peak of Fw[]
	  should be F0, as Fw[] can be considered the autocorrelation function
	  of Sw[] (the speech spectrum).  However sometimes due to phase
	  effects the lowest frequency maxima may not be the global maxima.

	  This works OK in practice and favours low F0 values in the presence
	  of background noise which means the sinusoidal codec does an OK job
	  of synthesising the background noise.  High F0 in background noise
	  tends to sound more periodic introducing annoying artifacts.

	*/
	private static final float post_process_sub_multiples(final Jkiss_fft_cpx Fw[],
		final int pmin, final int pmax, final float gmax, final int gmax_bin,
		final float prev_Wo)
	{
		/* post process estimate by searching submultiples */

		int mult = 2;
		final int min_bin = PE_FFT_SIZE * DEC / pmax;
		int cmax_bin = gmax_bin;
		final int prev_f0_bin = (int)(prev_Wo * (4000.0f / PI) * (PE_FFT_SIZE * DEC) / SAMPLE_RATE);
		final float treshold = CNLP * gmax;// java
		final float treshold2 = treshold * 0.5f;

		int b;
		while( (b = gmax_bin / mult) >= min_bin ) {

			// int b = gmax_bin / mult;			/* determine search interval */
			int bmin = (int)(0.8f * (float)b);
			final int bmax = (int)(1.2f * (float)b);
			if( bmin < min_bin ) {
				bmin = min_bin;
			}

			/* lower threshold to favour previous frames pitch estimate,
				this is a form of pitch tracking */
			float thresh;
			if( (prev_f0_bin > bmin) && (prev_f0_bin < bmax) ) {
				thresh = treshold2;
			} else {
				thresh = treshold;
			}

			float lmax = 0f;
			int lmax_bin = bmin;
			for( b = bmin; b <= bmax; b++ ) {
				if( Fw[b].r > lmax ) {
					lmax = Fw[b].r;
					lmax_bin = b;
				}
			}

			if( lmax > thresh ) {
				if( (lmax > Fw[lmax_bin - 1].r) && (lmax > Fw[lmax_bin + 1].r) ) {
					cmax_bin = lmax_bin;
				}
			}

			mult++;
		}

		final float best_f0 = (float)cmax_bin * SAMPLE_RATE / (PE_FFT_SIZE * DEC);

		return best_f0;
	}

	// java: never uses
	/**

	  post_process_mbe()

	  Use the MBE pitch estimation algorithm to evaluate pitch candidates.  This
	  works OK but the accuracy at low F0 is affected by NW, the analysis window
	  size used for the DFT of the input speech Sw[].  Also favours high F0 in
	  the presence of background noise which causes periodic artifacts in the
	  synthesised speech.

	*/
	/* private static final float post_process_mbe(final Jkiss_fft_cpx Fw[], final int pmin, final int pmax, final float gmax,
			final Jkiss_fft_cpx Sw[], final Jkiss_fft_cpx W[], final float[] prev_Wo)
	{
		float candidate_f0;
		float f0,best_f0;// fundamental frequency
		float e,e_min;// MBE cost function
		int   i;
// #ifdef DUMP
//		float e_hz[F0_MAX];
// #endif
// #if !defined(NDEBUG) || defined(DUMP)
//		int   bin;
// #endif
		float f0_min, f0_max;
		float f0_start, f0_end;

		f0_min = (float)SAMPLE_RATE / pmax;
		f0_max = (float)SAMPLE_RATE / pmin;

		// Now look for local maxima.  Each local maxima is a candidate
		// that we test using the MBE pitch estimation algotithm

// #ifdef DUMP
//		for( i = 0; i < F0_MAX; i++)
//			e_hz[i] = -1;
// #endif
		e_min = 1E32f;
		best_f0 = 50;
		for( i = PE_FFT_SIZE * DEC / pmax; i <= PE_FFT_SIZE * DEC / pmin; i++ ) {
			if( (Fw[i].r > Fw[i - 1].r) && (Fw[i].r > Fw[i + 1].r) ) {

				// local maxima found, lets test if it's big enough

				if( Fw[i].r > T * gmax ) {

					// OK, sample MBE cost function over +/- 10Hz range in 2.5Hz steps

					candidate_f0 = (float)i * SAMPLE_RATE / (PE_FFT_SIZE * DEC);
					f0_start = candidate_f0 - 20f;
					f0_end = candidate_f0 + 20f;
					if( f0_start < f0_min ) {
						f0_start = f0_min;
					}
					if( f0_end > f0_max ) {
						f0_end = f0_max;
					}

					for( f0 = f0_start; f0 <= f0_end; f0 += 2.5f ) {
						e = test_candidate_mbe( Sw, W, f0 );
// #if !defined(NDEBUG) || defined(DUMP)
//		bin = floorf( f0 );// assert((bin > 0) && (bin < F0_MAX));
// #endif
// #ifdef DUMP
//		e_hz[bin] = e;
// #endif
						if( e < e_min ) {
							e_min = e;
							best_f0 = f0;
						}
					}
				}
			}
		}

		// finally sample MBE cost function around previous pitch estimate
		// (form of pitch tracking)

		candidate_f0 = prev_Wo[0] * SAMPLE_RATE / Jdefines.TWO_PI;
		f0_start = candidate_f0 - 20f;
		f0_end = candidate_f0 + 20f;
		if( f0_start < f0_min ) {
			f0_start = f0_min;
		}
		if( f0_end > f0_max ) {
			f0_end = f0_max;
		}

		for( f0 = f0_start; f0 <= f0_end; f0 += 2.5f ) {
			e = test_candidate_mbe( Sw, W, f0 );
// #if !defined(NDEBUG) || defined(DUMP)
//		bin = floorf( f0 );// assert((bin > 0) && (bin < F0_MAX));
// #endif
// #ifdef DUMP
//		e_hz[bin] = e;
// #endif
			if( e < e_min ) {
				e_min = e;
				best_f0 = f0;
			}
		}

// #ifdef DUMP
//		dump_e(e_hz);
// #endif

		return best_f0;
	} */

	// java: never uses
	/**

	  test_candidate_mbe()

	  Returns the error of the MBE cost function for the input f0.

	  Note: I think a lot of the operations below can be simplified as
	  W[].imag = 0 and has been normalised such that den always equals 1.

	*/
	/* private static final float test_candidate_mbe(final Jkiss_fft_cpx Sw[], final Jkiss_fft_cpx W[], final float f0)
	{
		final Jkiss_fft_cpx  Sw_[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];// DFT of all voiced synthesised signal
		// final COMP  Am;

		final int L4 = ((int)(Math.floor( (SAMPLE_RATE / 2.0f) / f0 ))) >> 2;
		final float Wo = f0 * (2 * PI / SAMPLE_RATE) * Jdefines.FFT_ENC / Jdefines.TWO_PI;// current "test" fundamental freq.

		float error = 0.0f;// accumulated error between originl and synthesised

		// Just test across the harmonics in the first 1000 Hz (L/4)

		for( int l = 1; l < L4; l++ ) {
			float am_real = 0.0f;// amplitude sample for this band
			float am_imag = 0.0f;
			float den = 0.0f;// denominator of Am expression
			final int al = (int)(Math.ceil( (l - 0.5f) * Wo ));
			final int bl = (int)(Math.ceil( (l + 0.5f) * Wo ));

			// Estimate amplitude of harmonic assuming harmonic is totally voiced

			for( int m = al; m < bl; m++ ) {
				final int offset = (int)(Jdefines.FFT_ENC / 2 + m - l * Wo + 0.5f);// centers Hw[] about current harmonic
				final Jkiss_fft_cpx w = W[offset];// java
				final float w_real = w.r;
				final float w_imag = w.i;
				final Jkiss_fft_cpx s = Sw[m];// java
				final float s_real = s.r;
				final float s_imag = s.i;
				am_real += s_real * w_real + s_imag * w_imag;
				am_imag += s_imag * w_real - s_real * w_imag;
				den += w_real * w_real + w_imag * w_imag;
			}

			am_real /= den;
			am_imag /= den;

			// Determine error between estimated harmonic and original

			for( int m = al; m < bl; m++ ) {
				final int offset = (int)(Jdefines.FFT_ENC / 2 + m - l * Wo + 0.5f);
				final Jkiss_fft_cpx w = W[offset];// java
				final float w_real = w.r;
				final float w_imag = w.i;
				float t = am_real * w_real - am_imag * w_imag;
				Sw_[m].r = t;
				final Jkiss_fft_cpx s = Sw[m];// java
				t = s.r - t;
				error += t * t;
				t = am_real * w_imag + am_imag * w_real;
				Sw_[m].i = t;
				t = s.i - t;
				error += t * t;
			}
		}

		return error;
	} */

}