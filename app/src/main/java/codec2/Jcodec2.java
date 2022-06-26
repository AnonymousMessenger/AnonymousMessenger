package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: codec2.c
  AUTHOR......: David Rowe
  DATE CREATED: 21/8/2010

  Codec2 fully quantised encoder and decoder functions.  If you want use
  codec2, the codec2_xxx functions are for you.

\*---------------------------------------------------------------------------*/

/*
  Copyright ( C ) 2010 David Rowe

  All rights reserved.

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License version 2.1, as
  published by the Free Software Foundation.  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, see  < http://www.gnu.org/licenses/>.
*/

// codec2.c

public final class Jcodec2 {
	public static final int CODEC2_MODE_3200 = 0;
	public static final int CODEC2_MODE_2400 = 1;
	public static final int CODEC2_MODE_1600 = 2;
	public static final int CODEC2_MODE_1400 = 3;
	public static final int CODEC2_MODE_1300 = 4;
	public static final int CODEC2_MODE_1200 = 5;
	public static final int CODEC2_MODE_700  = 6;
	public static final int CODEC2_MODE_700B = 7;

	static final int CODEC2_RAND_MAX = 32767;// java: moved from sine.h
	// codec2_internal.h
	// struct CODEC2 {
		private final int           mode;
		/** forward FFT config */
		private final Jkiss_fft_state  fft_fwd_cfg;
		/** time domain hamming window */
		private final float   w[] = new float[Jdefines.M];
		/** DFT of w[] */
		private final Jkiss_fft_cpx   W[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		/** trapezoidal synthesis window */
		private final float   Pn[] = new float[2 * Jdefines.N];
		/** buffer for band pass filter */
		private final float[]       bpf_buf;
		/** input speech */
		private final float   Sn[] = new float[Jdefines.M];
		/** high pass filter states */
		private final float   hpf_states[] = new float[2];
		/** pitch predictor states */
		private final Jnlp          nlp;
		/** non-zero for gray encoding */
		private boolean       gray;

		/** inverse FFT config */
		private final Jkiss_fft_state fft_inv_cfg;
		/** synthesised output speech */
		private final float   Sn_[] = new float[2 * Jdefines.N];
		/** excitation model phase track */
		private float         ex_phase;
		/** background noise estimate for post filter */
		private float         bg_est;
		/** previous frame's pitch estimate */
		private float         prev_Wo_enc;
		/** previous frame's model parameters */
		// private final JMODEL  prev_model_dec = new JMODEL();
		private JMODEL        prev_model_dec = new JMODEL();// TODO check using pointer instead data holder
		/** previous frame's LSPs */
		private final float   prev_lsps_dec[] = new float[Jdefines.LPC_ORD];
		/** previous frame's LPC energy */
		private float         prev_e_dec;

		/** LPC post filter on */
		private boolean       lpc_pf;
		/** LPC post filter bass boost */
		private boolean       bass_boost;
		/** LPC post filter parameters */
		private float         beta;
		private float         gamma;

		/** joint pitch and energy VQ states */
		private final float   xq_enc[] = new float[2];
		private final float   xq_dec[] = new float[2];

		/** enable smoothing for channels with errors */
		// private boolean           smoothing;// FIXME never uses
		/** optional soft decn bits from demod */
		// private float[]       softdec;// java: never uses in the production code
	// };

/*---------------------------------------------------------------------------*\

								FUNCTIONS

\*---------------------------------------------------------------------------*/
	// public static final Jcodec2 codec2_create( final int mode )
	/**

	  FUNCTION....: codec2_create
	  AUTHOR......: David Rowe
	  DATE CREATED: 21/8/2010

	  Create and initialise an instance of the codec.  Returns a pointer
	  to the codec states or NULL on failure.  One set of states is
	  sufficient for a full duuplex codec ( i.e. an encoder and decoder ).
	  You don't need separate states for encoders and decoders.  See
	  c2enc.c and c2dec.c for examples.

	*/
	public Jcodec2( final int mode )
	{
		/* assert(
			( mode == CODEC2_MODE_3200 ) ||
			( mode == CODEC2_MODE_2400 ) ||
			( mode == CODEC2_MODE_1600 ) ||
			( mode == CODEC2_MODE_1400 ) ||
			( mode == CODEC2_MODE_1300 ) ||
			( mode == CODEC2_MODE_1200 ) ||
			( mode == CODEC2_MODE_700 ) ||
			( mode == CODEC2_MODE_700B )
			 ); */
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			this.W[i] = new Jkiss_fft_cpx();
		}
		this.mode = mode;
		for( int i = 0; i < Jdefines.M; i++ ) {
			this.Sn[i] = 1.0f;
		}
		this.hpf_states[0] = this.hpf_states[1] = 0.0f;
		for( int i = 0; i < 2 * Jdefines.N; i++ ) {
			this.Sn_[i] = 0;
		}
		this.fft_fwd_cfg = Jkiss_fft_state.kiss_fft_alloc( Jdefines.FFT_ENC, false );//, null, null );
		make_analysis_window( this.fft_fwd_cfg, this.w, this.W );
		make_synthesis_window( this.Pn );
		this.fft_inv_cfg = Jkiss_fft_state.kiss_fft_alloc( Jdefines.FFT_DEC, true );// , null, null );
		Jquantise.quantise_init();
		this.prev_Wo_enc = 0.0f;
		this.bg_est = 0.0f;
		this.ex_phase = 0.0f;

		for( int l = 1; l <= Jdefines.MAX_AMP; l++ ) {
			this.prev_model_dec.A[l] = 0.0f;
		}
		this.prev_model_dec.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;
		this.prev_model_dec.L = (int)( Jdefines.PI / this.prev_model_dec.Wo );
		this.prev_model_dec.voiced = false;

		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
		  this.prev_lsps_dec[i] = i * Jdefines.PI / ( Jdefines.LPC_ORD + 1 );
		}
		this.prev_e_dec = 1;

		this.nlp = new Jnlp( Jdefines.M );

		this.gray = mode != CODEC2_MODE_700B;// natural binary better for trellis decoding ( hopefully added later )

		this.lpc_pf = true; this.bass_boost = true; this.beta = Jquantise.LPCPF_BETA; this.gamma = Jquantise.LPCPF_GAMMA;

		this.xq_enc[0] = this.xq_enc[1] = 0.0f;
		this.xq_dec[0] = this.xq_dec[1] = 0.0f;

		// this.smoothing = false;// java: not uses in the production code

		this.bpf_buf = new float[ Jbpf.BPF_N + 4 * Jdefines.N ];
		// assert( this.bpf_buf != null );
		for( int i = 0; i < Jbpf.BPF_N + 4 * Jdefines.N; i++ ) {
			this.bpf_buf[i] = 0.0f;
		}

		// this.softdec = null;// java: not uses in the production code

		// return c2;
	}

	/**

	  FUNCTION....: codec2_destroy
	  AUTHOR......: David Rowe
	  DATE CREATED: 21/8/2010

	  Destroy an instance of the codec.

	*/
	/* public static final void codec2_destroy( final Jcodec2 c2 )
	{
		// assert( c2 != NULL );
		c2.bpf_buf = null;
		c2.nlp = null;
		c2.fft_fwd_cfg = null;
		c2.fft_inv_cfg = null;
		// c2 = null;
	}*/

	// start sine.c
	// private static final float HPF_BETA = 0.125f;
	/**

	  FUNCTION....: hpf
	  AUTHOR......: David Rowe
	  DATE CREATED: 16 Nov 2010

	  High pass filter with a -3dB point of about 160Hz.

	    y( n ) = -HPF_BETA*y( n-1 ) + x( n ) - x( n-1 )

	*/
	/* private static final float hpf( final float x, final float states[] )// FIXME never uses
	{
		final float s = -HPF_BETA * states[0] + x - states[1];
		states[0] = s;
		states[1] = x;

		return s;
	}*/
	/**

	  FUNCTION....: make_analysis_window
	  AUTHOR......: David Rowe
	  DATE CREATED: 11/5/94

	  Init function that generates the time domain analysis window and it's DFT.

	*/
	private static final void make_analysis_window( final Jkiss_fft_state fft_fwd_cfg, final float w[], final Jkiss_fft_cpx W[] )
	{
		/*
		 Generate Hamming window centered on M-sample pitch analysis window

		0            M/2           M-1
		|-------------|-------------|
			|-------|-------|
				NW samples

		 All our analysis/synthsis is centred on the M/2 sample.
		*/

		float m = 0.0f;
		for( int i = 0; i < Jdefines.M / 2 - Jdefines.NW / 2; i++ ) {
			w[i] = 0.0f;
		}
		for( int i = Jdefines.M / 2 - Jdefines.NW / 2, j = 0; i < Jdefines.M / 2 + Jdefines.NW / 2; i++, j++ ) {
			final float w_i = 0.5f - 0.5f * (float)Math.cos( (double)(Jdefines.TWO_PI * j / (Jdefines.NW - 1)) );
			w[i] = w_i;
			m += w_i * w_i;
		}
		for( int i = Jdefines.M / 2 + Jdefines.NW / 2; i < Jdefines.M; i++ ) {
			w[i] = 0.0f;
		}

		/* Normalise - makes freq domain amplitude estimation straight
		 forward */

		m = (float)(1.0 / Math.sqrt( (double)(m * Jdefines.FFT_ENC) ));
		for( int i = 0; i < Jdefines.M; i++ ) {
			w[i] *= m;
		}

		/*
		 Generate DFT of analysis window, used for later processing.  Note
		 we modulo FFT_ENC shift the time domain window w[], this makes the
		 imaginary part of the DFT W[] equal to zero as the shifted w[] is
		 even about the n=0 time axis if NW is odd.  Having the imag part
		 of the DFT W[] makes computation easier.

		 0                      FFT_ENC-1
		 |-------------------------|

		  ----\               /----
			   \             /
				\           /          <- shifted version of window w[n]
				 \         /
				  \       /
				   -------

		 |---------|     |---------|
		   NW/2              NW/2
		*/

		final Jkiss_fft_cpx wshift[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			wshift[i] = new Jkiss_fft_cpx();
			// wshift[i].real = 0.0f;
			// wshift[i].imag = 0.0f;
		}
		for( int i = 0, j = Jdefines.M / 2; i < Jdefines.NW / 2; i++, j++ ) {
			wshift[i].r = w[j];
		}
		for( int i = Jdefines.FFT_ENC - Jdefines.NW / 2, j = Jdefines.M / 2 - Jdefines.NW / 2; i < Jdefines.FFT_ENC; i++, j++ ) {
			wshift[i].r = w[j];
		}

		fft_fwd_cfg.kiss_fft( wshift, W );

		/*
		  Re-arrange W[] to be symmetrical about FFT_ENC/2.  Makes later
		  analysis convenient.

		Before:


		 0                 FFT_ENC-1
		 |----------|---------|
		 __                   _
		   \                 /
			\_______________/

		After:

		 0                 FFT_ENC-1
		 |----------|---------|
				   ___
				  /   \
		 ________/     \_______

		*/

		//final Jkiss_fft_cpx temp = new Jkiss_fft_cpx();
		for( int i = 0, j = Jdefines.FFT_ENC / 2; i < Jdefines.FFT_ENC / 2; i++, j++ ) {
			final float temp_r = W[i].r;
			final float temp_i = W[i].i;
			W[i].r = W[j].r;
			W[i].i = W[j].i;
			W[j].r = temp_r;
			W[j].i = temp_i;
		}

	}

	/**

	  FUNCTION....: make_synthesis_window
	  AUTHOR......: David Rowe
	  DATE CREATED: 11/5/94

	  Init function that generates the trapezoidal ( Parzen ) sythesis window.

	*/
	private static final void make_synthesis_window( final float Pn[] )
	{
		/* Generate Parzen window in time domain */

		float win = 0.0f;
		for( int i = 0; i < Jdefines.N / 2 - Jdefines.TW; i++ ) {
			Pn[i] = 0.0f;
		}
		win = 0.0f;
		for( int i = Jdefines.N / 2 - Jdefines.TW; i < Jdefines.N / 2 + Jdefines.TW; win += 1.0f / (2 * Jdefines.TW), i++  ) {
			Pn[i] = win;
		}
		for( int i = Jdefines.N / 2 + Jdefines.TW; i < 3 * Jdefines.N / 2 - Jdefines.TW; i++ ) {
			Pn[i] = 1.0f;
		}
		win = 1.0f;
		for( int i = 3 * Jdefines.N / 2 - Jdefines.TW; i < 3 * Jdefines.N / 2 + Jdefines.TW; win -= 1.0f / (2 * Jdefines.TW), i++ ) {
			Pn[i] = win;
		}
		for( int i = 3 * Jdefines.N / 2 + Jdefines.TW; i < 2 * Jdefines.N; i++ ) {
			Pn[i] = 0.0f;
		}
	}

	/**

	  FUNCTION....: dft_speech
	  AUTHOR......: David Rowe
	  DATE CREATED: 27/5/94

	  Finds the DFT of the current speech input speech frame.

	*/
	private static final void dft_speech( final Jkiss_fft_state fft_fwd_cfg, final Jkiss_fft_cpx Sw[], final float Sn[], final float w[] )
	{
		final Jkiss_fft_cpx sw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];

		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			sw[i] = new Jkiss_fft_cpx();// already zeroed
			// sw[i].real = 0.0;
			// sw[i].imag = 0.0;
		}

		/* Centre analysis window on time axis, we need to arrange input
		 to FFT this way to make FFT phases correct */

		/* move 2nd half to start of FFT input vector */

		for( int i = 0, j = Jdefines.M / 2; i < Jdefines.NW / 2; i++, j++ ) {
			sw[i].r = Sn[j] * w[j];
		}

		/* move 1st half to end of FFT input vector */

		for( int i = Jdefines.FFT_ENC - Jdefines.NW / 2, j = Jdefines.M / 2 - Jdefines.NW / 2; i < Jdefines.FFT_ENC; i++, j++ ) {
			sw[i].r = Sn[j] * w[j];
		}

		fft_fwd_cfg.kiss_fft( sw, Sw );
	}

	/**

	  FUNCTION....: synthesise
	  AUTHOR......: David Rowe
	  DATE CREATED: 20/2/95

	  Synthesise a speech signal in the frequency domain from the
	  sinusodal model parameters.  Uses overlap-add with a trapezoidal
	  window to smoothly interpolate betwen frames.

	  @param fft_inv_cfg
	  @param Sn_ time domain synthesised signal
	  @param model ptr to model parameters for this frame
	  @param Pn time domain Parzen window
	  @param shift flag used to handle transition frames

	*/
	static final void synthesise( final Jkiss_fft_state fft_inv_cfg, final float Sn_[], final JMODEL model, final float Pn[], final boolean shift)
	{
		final Jkiss_fft_cpx Sw_[] = new Jkiss_fft_cpx[Jdefines.FFT_DEC];	/* DFT of synthesised signal */
		final Jkiss_fft_cpx sw_[] = new Jkiss_fft_cpx[Jdefines.FFT_DEC];	/* synthesised signal */

		if( shift ) {
			/* Update memories */
			for( int i = 0, j = Jdefines.N; i < Jdefines.N - 1; i++, j++ ) {
				Sn_[i] = Sn_[j];
			}
			Sn_[Jdefines.N - 1] = 0.0f;
		}

		for( int i = 0; i < Jdefines.FFT_DEC; i++ ) {
			Sw_[i] = new Jkiss_fft_cpx();
			sw_[i] = new Jkiss_fft_cpx();
			// Sw_[i].real = 0.0f;// java: already zeroed
			// Sw_[i].imag = 0.0f;
		}

		/*
		  Nov 2010 - found that synthesis using time domain cos() functions
		  gives better results for synthesis frames greater than 10ms.  Inverse
		  FFT synthesis using a 512 pt FFT works well for 10ms window.  I think
		  (but am not sure) that the problem is related to the quantisation of
		  the harmonic frequencies to the FFT bin size, e.g. there is a
		  8000/512 Hz step between FFT bins.  For some reason this makes
		  the speech from longer frame > 10ms sound poor.  The effect can also
		  be seen when synthesising test signals like single sine waves, some
		  sort of amplitude modulation at the frame rate.

		  Another possibility is using a larger FFT size (1024 or 2048).
		*/

//# define FFT_SYNTHESIS
//# ifdef FFT_SYNTHESIS
		/* Now set up frequency domain synthesised speech */
		final float w = model.Wo * Jdefines.FFT_DEC / Jdefines.TWO_PI;// java
		final float[] ma = model.A;// java
		final float[] mphi = model.phi;// java
		for( int l = 1, count = model.L; l <= count; l++ ) {
			//for( l=model.L/2; l<=model.L; l++ ) {
			//for( l=1; l<=model.L/4; l++ ) {
			int b = (int)( (float)l * w + 0.5f );
			if( b > (Jdefines.FFT_DEC / 2 - 1) ) {
				b = Jdefines.FFT_DEC / 2 - 1;
			}
			final Jkiss_fft_cpx s = Sw_[b];// java
			final float a = ma[l];// java
			final double p = (double)mphi[l];// java
			s.r = a * (float)Math.cos( p );
			s.i = a * (float)Math.sin( p );
			Sw_[Jdefines.FFT_DEC - b].r = s.r;
			Sw_[Jdefines.FFT_DEC - b].i = -s.i;
		}

		/* Perform inverse DFT */

		fft_inv_cfg.kiss_fft( Sw_, sw_ );
//#else
		/*
		   Direct time domain synthesis using the cos() function.  Works
		   well at 10ms and 20ms frames rates.  Note synthesis window is
		   still used to handle overlap-add between adjacent frames.  This
		   could be simplified as we don't need to synthesise where Pn[]
		   is zero.
		*/
		/* for( int l = 1, count = model.L; l <= count; l++ ) {
			for( int i = 0, j = -Jdefines.N + 1; i < Jdefines.N - 1; i++, j++ ) {
				Sw_[Jdefines.FFT_DEC - Jdefines.N + 1 + i].r += 2.0f * model.A[l] * Math.cos( j * model.Wo * l + model.phi[l] );
			}
			for( int i = Jdefines.N - 1, j = 0; i < 2 * Jdefines.N; i++, j++ ) {
				Sw_[j].r += 2.0f * model.A[l] * Math.cos( j * model.Wo * l + model.phi[l] );
			}
		}*/
//#endif

		/* Overlap add to previous samples */

		for( int i = 0, j = Jdefines.FFT_DEC - Jdefines.N + 1; i < Jdefines.N - 1; i++, j++ ) {
			Sn_[i] += sw_[j].r * Pn[i];
		}

		if( shift ) {
			for( int i = Jdefines.N - 1, j = 0; i < 2 * Jdefines.N; i++, j++ ) {
				Sn_[i] = sw_[j].r * Pn[i];
			}
		} else {
			for( int i = Jdefines.N - 1, j = 0; i < 2 * Jdefines.N; i++, j++ ) {
				Sn_[i] += sw_[j].r * Pn[i];
			}
		}
	}
	// end sine.c

	/**

	  FUNCTION....: codec2_bits_per_frame
	  AUTHOR......: David Rowe
	  DATE CREATED: Nov 14 2011

	  Returns the number of bits per frame.

	*/
	public final int codec2_bits_per_frame() {
		if( this.mode == CODEC2_MODE_3200 ) {
			return 64;
		}
		if( this.mode == CODEC2_MODE_2400 ) {
			return 48;
		}
		if( this.mode == CODEC2_MODE_1600 ) {
			return 64;
		}
		if( this.mode == CODEC2_MODE_1400 ) {
			return 56;
		}
		if( this.mode == CODEC2_MODE_1300 ) {
			return 52;
		}
		if( this.mode == CODEC2_MODE_1200 ) {
			return 48;
		}
		if( this.mode == CODEC2_MODE_700 ) {
			return 28;
		}
		if( this.mode == CODEC2_MODE_700B ) {
			return 28;
		}

		return 0; /* shouldn't get here */
	}


	/**

	  FUNCTION....: codec2_samples_per_frame
	  AUTHOR......: David Rowe
	  DATE CREATED: Nov 14 2011

	  Returns the number of speech samples per frame.

	*/
	public final int codec2_samples_per_frame() {
		if( this.mode == CODEC2_MODE_3200 ) {
			return 160;
		}
		if( this.mode == CODEC2_MODE_2400 ) {
			return 160;
		}
		if( this.mode == CODEC2_MODE_1600 ) {
			return 320;
		}
		if( this.mode == CODEC2_MODE_1400 ) {
			return 320;
		}
		if( this.mode == CODEC2_MODE_1300 ) {
			return 320;
		}
		if( this.mode == CODEC2_MODE_1200 ) {
			return 320;
		}
		if( this.mode == CODEC2_MODE_700 ) {
			return 320;
		}
		if( this.mode == CODEC2_MODE_700B ) {
			return 320;
		}

		return 0; /* shouldnt get here */
	}

	public final void codec2_encode(final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );
		/* assert(
			( c2.mode == CODEC2_MODE_3200 ) ||
			( c2.mode == CODEC2_MODE_2400 ) ||
			( c2.mode == CODEC2_MODE_1600 ) ||
			( c2.mode == CODEC2_MODE_1400 ) ||
			( c2.mode == CODEC2_MODE_1300 ) ||
			( c2.mode == CODEC2_MODE_1200 ) ||
			( c2.mode == CODEC2_MODE_700 )  ||
			( c2.mode == CODEC2_MODE_700B )
			 ); */

		if( this.mode == CODEC2_MODE_3200 ) {
			codec2_encode_3200( bits, speech );
		}
		if( this.mode == CODEC2_MODE_2400 ) {
			codec2_encode_2400( bits, speech );
		}
		if( this.mode == CODEC2_MODE_1600 ) {
			codec2_encode_1600( bits, speech );
		}
		if( this.mode == CODEC2_MODE_1400 ) {
			codec2_encode_1400( bits, speech );
		}
		if( this.mode == CODEC2_MODE_1300 ) {
			codec2_encode_1300( bits, speech );
		}
		if( this.mode == CODEC2_MODE_1200 ) {
			codec2_encode_1200( bits, speech );
		}
// #ifndef CORTEX_M4
		if( this.mode == CODEC2_MODE_700 ) {
			codec2_encode_700( bits, speech );
		}
		if( this.mode == CODEC2_MODE_700B ) {
			codec2_encode_700b( bits, speech );
// #endif
		}
	}

	public final void codec2_decode( final short speech[], final byte[] bits )
	{
		codec2_decode_ber( speech, bits, 0.0f );
	}

	public final void codec2_decode_ber( final short speech[], final byte[] bits, final float ber_est )
	{
		/* assert( c2 != NULL );
		assert(
			( c2.mode == CODEC2_MODE_3200 ) ||
			( c2.mode == CODEC2_MODE_2400 ) ||
			( c2.mode == CODEC2_MODE_1600 ) ||
			( c2.mode == CODEC2_MODE_1400 ) ||
			( c2.mode == CODEC2_MODE_1300 ) ||
			( c2.mode == CODEC2_MODE_1200 ) ||
			( c2.mode == CODEC2_MODE_700 ) ||
			( c2.mode == CODEC2_MODE_700B )
			 ); */

		if( this.mode == CODEC2_MODE_3200 ) {
			codec2_decode_3200( speech, bits );
		}
		if( this.mode == CODEC2_MODE_2400 ) {
			codec2_decode_2400( speech, bits );
		}
		if( this.mode == CODEC2_MODE_1600 ) {
			codec2_decode_1600( speech, bits );
		}
		if( this.mode == CODEC2_MODE_1400 ) {
			codec2_decode_1400( speech, bits );
		}
		if( this.mode == CODEC2_MODE_1300 ) {
			codec2_decode_1300( speech, bits, ber_est );
		}
		if( this.mode == CODEC2_MODE_1200 ) {
			codec2_decode_1200( speech, bits );
		}
// #ifndef CORTEX_M4
		if( this.mode == CODEC2_MODE_700 ) {
			codec2_decode_700( speech, bits );
		}
		if( this.mode == CODEC2_MODE_700B ) {
			codec2_decode_700b( speech, bits );
// #endif
		}
	}


	/**

	  FUNCTION....: codec2_encode_3200
	  AUTHOR......: David Rowe
	  DATE CREATED: 13 Sep 2012

	  Encodes 160 speech samples (20ms of speech) into 64 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm twice.  On the
	  first frame we just send the voicing bits.  On the second frame we
	  send all model parameters.  Compared to 2400 we use a larger number
	  of bits for the LSPs and non-VQ pitch and energy.

	  The bit allocation is:

	    Parameter                      bits/frame
	    --------------------------------------
	    Harmonic magnitudes (LSPs)     50
	    Pitch (Wo)                      7
	    Energy                          5
	    Voicing (10ms update)           2
	    TOTAL                          64

	*/
	public final void codec2_encode_3200( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* first 10ms analysis frame - we just want voicing */

		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* second 10ms analysis frame */

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );
		final int Wo_index = Jquantise.encode_Wo( model.Wo, Jquantise.WO_BITS );
		nbit = Jpack.pack( bits, nbit, Wo_index, Jquantise.WO_BITS );

		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		final int e_index = Jquantise.encode_energy( e, Jquantise.E_BITS );
		nbit = Jpack.pack( bits, nbit, e_index, Jquantise.E_BITS );

		final int lspd_indexes[] = new int[Jdefines.LPC_ORD];
		Jquantise.encode_lspds_scalar( lspd_indexes, lsps, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSPD_SCALAR_INDEXES; i++ ) {
			nbit = Jpack.pack( bits, nbit, lspd_indexes[i], Jquantise.lspd_bits( i ) );
		}
		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_3200
	  AUTHOR......: David Rowe
	  DATE CREATED: 13 Sep 2012

	  Decodes a frame of 64 bits into 160 samples (20ms) of speech.

	*/
	public final void codec2_decode_3200( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[2];
		for( int i = 0; i < 2; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 2 x 10ms
		   frames */

		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		model[1].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		final int Wo_index = Jpack.unpack( bits, nbit, Jquantise.WO_BITS );
		model[1].Wo = Jquantise.decode_Wo( Wo_index, Jquantise.WO_BITS );
		model[1].L  = (int)(Jdefines.PI / model[1].Wo);

		final float e[] = new float[2];
		final int e_index = Jpack.unpack( bits, nbit, Jquantise.E_BITS );
		e[1] = Jquantise.decode_energy( e_index, Jquantise.E_BITS );

		final int lspd_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSPD_SCALAR_INDEXES; i++ ) {
			lspd_indexes[i] = Jpack.unpack( bits, nbit, Jquantise.lspd_bits( i ) );
		}

		final float lsps[][] = new float[2][Jdefines.LPC_ORD];
		final float[] lsps_1 = lsps[1];// java
		Jquantise.decode_lspds_scalar( lsps_1, lspd_indexes, Jdefines.LPC_ORD );

		/* interpolate ------------------------------------------------*/

		/* Wo and energy are sampled every 20ms, so we interpolate just 1
		   10ms frame between 20ms samples */

		Jinterp.interp_Wo( model[0], this.prev_model_dec, model[1] );
		e[0] = Jinterp.interp_energy( this.prev_e_dec, e[1] );

		/* LSPs are sampled every 20ms so we interpolate the frame in
		   between, then recover spectral amplitudes */

		Jinterp.interpolate_lsp_ver2( lsps[0], this.prev_lsps_dec, lsps_1, 0.5f, Jdefines.LPC_ORD );

		final float ak[][] = new float[2][Jdefines.LPC_ORD + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 2; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
					/*snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[1];
		this.prev_e_dec = e[1];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_1[i];
		}
	}


	/**

	  FUNCTION....: codec2_encode_2400
	  AUTHOR......: David Rowe
	  DATE CREATED: 21/8/2010

	  Encodes 160 speech samples (20ms of speech) into 48 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm twice.  On the
	  first frame we just send the voicing bit.  On the second frame we
	  send all model parameters.

	  The bit allocation is:

	    Parameter                      bits/frame
	    --------------------------------------
	    Harmonic magnitudes (LSPs)     36
	    Joint VQ of Energy and Wo       8
	    Voicing (10ms update)           2
	    Spare                           2
	    TOTAL                          48

	*/
	public final void codec2_encode_2400( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* first 10ms analysis frame - we just want voicing */

		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* second 10ms analysis frame */

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		final int WoE_index = Jquantise.encode_WoE( model, e, this.xq_enc );
		nbit = Jpack.pack( bits, nbit, WoE_index, Jquantise.WO_E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		Jquantise.encode_lsps_scalar( lsp_indexes, lsps, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			nbit = Jpack.pack( bits, nbit, lsp_indexes[i], Jquantise.lsp_bits( i ) );
		}
		final int spare = 0;
		nbit = Jpack.pack( bits, nbit, spare, 2 );

		// assert( nbit == (unsigned)codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_2400
	  AUTHOR......: David Rowe
	  DATE CREATED: 21/8/2010

	  Decodes frames of 48 bits into 160 samples (20ms) of speech.

	*/
	public final void codec2_decode_2400( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */

		final JMODEL model[] = new JMODEL[2];
		for( int i = 0; i < 2; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 2 x 10ms
		   frames */
		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[1].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		final int WoE_index = Jpack.unpack( bits, nbit, Jquantise.WO_E_BITS );
		final float e[] = new float[2];
		e[1] = Jquantise.decode_WoE( model[1],/* &e[1],*/ this.xq_dec, WoE_index );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			lsp_indexes[i] = Jpack.unpack( bits, nbit, Jquantise.lsp_bits( i ) );
		}
		final float lsps[][] = new float[2][Jdefines.LPC_ORD];
		final float[] lsps_1 = lsps[1];// java
		Jquantise.decode_lsps_scalar( lsps_1, lsp_indexes, Jdefines.LPC_ORD );
		Jquantise.check_lsp_order( lsps_1, Jdefines.LPC_ORD );
		Jquantise.bw_expand_lsps( lsps_1, Jdefines.LPC_ORD, 50.0f, 100.0f );

		/* interpolate ------------------------------------------------*/

		/* Wo and energy are sampled every 20ms, so we interpolate just 1
		   10ms frame between 20ms samples */

		Jinterp.interp_Wo( model[0], this.prev_model_dec, model[1] );
		e[0] = Jinterp.interp_energy( this.prev_e_dec, e[1] );

		/* LSPs are sampled every 20ms so we interpolate the frame in
		   between, then recover spectral amplitudes */

		Jinterp.interpolate_lsp_ver2( lsps[0], this.prev_lsps_dec, lsps_1, 0.5f, Jdefines.LPC_ORD );
		final float ak[][] = new float[2][Jdefines.LPC_ORD + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 2; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
							/*snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[1];
		this.prev_e_dec = e[1];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_1[i];
		}
	}


	/**

	  FUNCTION....: codec2_encode_1600
	  AUTHOR......: David Rowe
	  DATE CREATED: Feb 28 2013

	  Encodes 320 speech samples (40ms of speech) into 64 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm 4 times:

	  frame 0: voicing bit
	  frame 1: voicing bit, Wo and E
	  frame 2: voicing bit
	  frame 3: voicing bit, Wo and E, scalar LSPs

	  The bit allocation is:

	    Parameter                      frame 2  frame 4   Total
	    -------------------------------------------------------
	    Harmonic magnitudes (LSPs)      0       36        36
	    Pitch (Wo)                      7        7        14
	    Energy                          5        5        10
	    Voicing (10ms update)           2        2         4
	    TOTAL                          14       50        64

	*/
	public final void codec2_encode_1600( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* frame 1: - voicing ---------------------------------------------*/

		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 2: - voicing, scalar Wo & E -------------------------------*/

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		int Wo_index = Jquantise.encode_Wo( model.Wo, Jquantise.WO_BITS );
		nbit = Jpack.pack( bits, nbit, Wo_index, Jquantise.WO_BITS );

		/* need to run this just to get LPC energy */
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		int e_index = Jquantise.encode_energy( e, Jquantise.E_BITS );
		nbit = Jpack.pack( bits, nbit, e_index, Jquantise.E_BITS );

		/* frame 3: - voicing ---------------------------------------------*/

		analyse_one_frame( model, speech, 2 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 4: - voicing, scalar Wo & E, scalar LSPs ------------------*/

		analyse_one_frame( model, speech, 3 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		Wo_index = Jquantise.encode_Wo( model.Wo, Jquantise.WO_BITS );
		nbit = Jpack.pack( bits, nbit, Wo_index, Jquantise.WO_BITS );

		e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		e_index = Jquantise.encode_energy( e, Jquantise.E_BITS );
		nbit = Jpack.pack( bits, nbit, e_index, Jquantise.E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		Jquantise.encode_lsps_scalar( lsp_indexes, lsps, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			nbit = Jpack.pack( bits, nbit, lsp_indexes[i], Jquantise.lsp_bits( i ) );
		}

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_1600
	  AUTHOR......: David Rowe
	  DATE CREATED: 11 May 2012

	  Decodes frames of 64 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_1600( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 4 x 10ms
		   frames */
		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[1].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		int Wo_index = Jpack.unpack( bits, nbit, Jquantise.WO_BITS );
		model[1].Wo = Jquantise.decode_Wo( Wo_index, Jquantise.WO_BITS );
		model[1].L  = (int)(Jdefines.PI / model[1].Wo);

		int e_index = Jpack.unpack( bits, nbit, Jquantise.E_BITS );
		final float   e[] = new float[4];
		e[1] = Jquantise.decode_energy( e_index, Jquantise.E_BITS );

		model[2].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[3].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		Wo_index = Jpack.unpack( bits, nbit, Jquantise.WO_BITS );
		model[3].Wo = Jquantise.decode_Wo( Wo_index, Jquantise.WO_BITS );
		model[3].L  = (int)(Jdefines.PI / model[3].Wo);

		e_index = Jpack.unpack( bits, nbit, Jquantise.E_BITS );
		e[3] = Jquantise.decode_energy( e_index, Jquantise.E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			lsp_indexes[i] = Jpack.unpack( bits, nbit, Jquantise.lsp_bits( i ) );
		}
		final float lsps[][] = new float[4][Jdefines.LPC_ORD];
		final float[] lsps_3 = lsps[3];// java
		Jquantise.decode_lsps_scalar( lsps_3, lsp_indexes, Jdefines.LPC_ORD );
		Jquantise.check_lsp_order( lsps_3, Jdefines.LPC_ORD );
		Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD, 50.0f, 100.0f );

		/* interpolate ------------------------------------------------*/

		/* Wo and energy are sampled every 20ms, so we interpolate just 1
		   10ms frame between 20ms samples */

		Jinterp.interp_Wo( model[0], this.prev_model_dec, model[1] );
		e[0] = Jinterp.interp_energy( this.prev_e_dec, e[1] );
		Jinterp.interp_Wo( model[2], model[1], model[3] );
		e[2] = Jinterp.interp_energy( e[1], e[3] );

		/* LSPs are sampled every 40ms so we interpolate the 3 frames in
		   between, then recover spectral amplitudes */
		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
			Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD );
		}
		final float ak[][] = new float[4][Jdefines.LPC_ORD + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/*float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
					/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_3[i];
		}
	}

	/**

	  FUNCTION....: codec2_encode_1400
	  AUTHOR......: David Rowe
	  DATE CREATED: May 11 2012

	  Encodes 320 speech samples (40ms of speech) into 56 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm 4 times:

	  frame 0: voicing bit
	  frame 1: voicing bit, joint VQ of Wo and E
	  frame 2: voicing bit
	  frame 3: voicing bit, joint VQ of Wo and E, scalar LSPs

	  The bit allocation is:

	    Parameter                      frame 2  frame 4   Total
	    -------------------------------------------------------
	    Harmonic magnitudes (LSPs)      0       36        36
	    Energy+Wo                       8        8        16
	    Voicing (10ms update)           2        2         4
	    TOTAL                          10       46        56

	*/

	public final void codec2_encode_1400( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* frame 1: - voicing ---------------------------------------------*/

		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 2: - voicing, joint Wo & E -------------------------------*/

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* need to run this just to get LPC energy */
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );

		int WoE_index = Jquantise.encode_WoE( model, e, this.xq_enc );
		nbit = Jpack.pack( bits, nbit, WoE_index, Jquantise.WO_E_BITS );

		/* frame 3: - voicing ---------------------------------------------*/

		analyse_one_frame( model, speech, 2 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 4: - voicing, joint Wo & E, scalar LSPs ------------------*/

		analyse_one_frame( model, speech, 3 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		WoE_index = Jquantise.encode_WoE( model, e, this.xq_enc );
		nbit = Jpack.pack( bits, nbit, WoE_index, Jquantise.WO_E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		Jquantise.encode_lsps_scalar( lsp_indexes, lsps, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			nbit = Jpack.pack( bits, nbit, lsp_indexes[i], Jquantise.lsp_bits( i ) );
		}

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_1400
	  AUTHOR......: David Rowe
	  DATE CREATED: 11 May 2012

	  Decodes frames of 56 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_1400( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 4 x 10ms
		   frames */
		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[1].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		int WoE_index = Jpack.unpack( bits, nbit, Jquantise.WO_E_BITS );
		final float e[] = new float[4];
		e[1] = Jquantise.decode_WoE( model[1],/* &e[1],*/ this.xq_dec, WoE_index );

		model[2].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[3].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		WoE_index = Jpack.unpack( bits, nbit, Jquantise.WO_E_BITS );
		e[3] = Jquantise.decode_WoE( model[3],/* &e[3],*/ this.xq_dec, WoE_index );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			lsp_indexes[i] = Jpack.unpack( bits, nbit, Jquantise.lsp_bits( i ) );
		}
		final float lsps[][] = new float[4][Jdefines.LPC_ORD];
		final float[] lsps_3 = lsps[3];// java
		Jquantise.decode_lsps_scalar( lsps_3, lsp_indexes, Jdefines.LPC_ORD );
		Jquantise.check_lsp_order( lsps_3, Jdefines.LPC_ORD );
		Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD, 50.0f, 100.0f );

		/* interpolate ------------------------------------------------*/

		/* Wo and energy are sampled every 20ms, so we interpolate just 1
		   10ms frame between 20ms samples */

		Jinterp.interp_Wo( model[0], this.prev_model_dec, model[1] );
		e[0] = Jinterp.interp_energy( this.prev_e_dec, e[1] );
		Jinterp.interp_Wo( model[2], model[1], model[3] );
		e[2] = Jinterp.interp_energy( e[1], e[3] );

		/* LSPs are sampled every 40ms so we interpolate the 3 frames in
		   between, then recover spectral amplitudes */
		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
			Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD );
		}
		final float ak[][] = new float[4][Jdefines.LPC_ORD + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
							/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_3[i];
		}
	}

	/**

	  FUNCTION....: codec2_encode_1300
	  AUTHOR......: David Rowe
	  DATE CREATED: March 14 2013

	  Encodes 320 speech samples (40ms of speech) into 52 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm 4 times:

	  frame 0: voicing bit
	  frame 1: voicing bit,
	  frame 2: voicing bit
	  frame 3: voicing bit, Wo and E, scalar LSPs

	  The bit allocation is:

	    Parameter                      frame 2  frame 4   Total
	    -------------------------------------------------------
	    Harmonic magnitudes (LSPs)      0       36        36
	    Pitch (Wo)                      0        7         7
	    Energy                          0        5         5
	    Voicing (10ms update)           2        2         4
	    TOTAL                           2       50        52

	*/
	public final void codec2_encode_1300( final byte[] bits, final short speech[] )
	{
/* #ifdef PROFILE
		unsigned final int quant_start;
#endif */

		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* frame 1: - voicing ---------------------------------------------*/

		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack_natural_or_gray( bits, nbit, model.voiced ? 1 : 0, 1, this.gray );

		/* frame 2: - voicing ---------------------------------------------*/

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, model.voiced ? 1 : 0, 1, this.gray );

		/* frame 3: - voicing ---------------------------------------------*/

		analyse_one_frame( model, speech, 2 * Jdefines.N );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, model.voiced ? 1 : 0, 1, this.gray );

		/* frame 4: - voicing, scalar Wo & E, scalar LSPs ------------------*/

		analyse_one_frame( model, speech, 3 * Jdefines.N );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, model.voiced ? 1 : 0, 1, this.gray );

		final int Wo_index = Jquantise.encode_Wo( model.Wo, Jquantise.WO_BITS );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, Wo_index, Jquantise.WO_BITS, this.gray );

/* #ifdef PROFILE
		quant_start = machdep_profile_sample();
#endif */
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		final float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		final int e_index = Jquantise.encode_energy( e, Jquantise.E_BITS );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, e_index, Jquantise.E_BITS, this.gray );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		Jquantise.encode_lsps_scalar( lsp_indexes, lsps, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			nbit = Jpack.pack_natural_or_gray( bits, nbit, lsp_indexes[i], Jquantise.lsp_bits( i ), this.gray );
		}
/* #ifdef PROFILE
		machdep_profile_sample_and_log( quant_start, "	quant/packing" );
#endif */

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}

	// private static int frames;// FIXME never uses
	/**

	  FUNCTION....: codec2_decode_1300
	  AUTHOR......: David Rowe
	  DATE CREATED: 11 May 2012

	  Decodes frames of 52 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_1300( final short speech[], final byte[] bits, final float ber_est )
	{
		// PROFILE_VAR( recover_start );

		// assert( c2 != NULL );
		// frames += 4;
		/* only need to zero these out due to ( unused ) snr calculation */

		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 4 x 10ms
		   frames */
		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack_natural_or_gray( bits, nbit, 1, this.gray ) != 0;
		model[1].voiced = Jpack.unpack_natural_or_gray( bits, nbit, 1, this.gray ) != 0;
		model[2].voiced = Jpack.unpack_natural_or_gray( bits, nbit, 1, this.gray ) != 0;
		model[3].voiced = Jpack.unpack_natural_or_gray( bits, nbit, 1, this.gray ) != 0;

		final int Wo_index = Jpack.unpack_natural_or_gray( bits, nbit, Jquantise.WO_BITS, this.gray );
		model[3].Wo = Jquantise.decode_Wo( Wo_index, Jquantise.WO_BITS );
		model[3].L  = (int)(Jdefines.PI / model[3].Wo);

		final int e_index = Jpack.unpack_natural_or_gray( bits, nbit, Jquantise.E_BITS, this.gray );
		final float e[] = new float[4];
		e[3] = Jquantise.decode_energy( e_index, Jquantise.E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSP_SCALAR_INDEXES; i++ ) {
			lsp_indexes[i] = Jpack.unpack_natural_or_gray( bits, nbit, Jquantise.lsp_bits( i ), this.gray );
		}
		final float lsps[][] = new float[4][Jdefines.LPC_ORD];
		final float[] lsps_3 = lsps[3];// java
		Jquantise.decode_lsps_scalar( lsps_3, lsp_indexes, Jdefines.LPC_ORD );
		Jquantise.check_lsp_order( lsps_3, Jdefines.LPC_ORD );
		Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD, 50.0f, 100.0f );

		if( ber_est > 0.15f ) {
			model[0].voiced =  model[1].voiced = model[2].voiced = model[3].voiced = false;
			e[3] = Jquantise.decode_energy( 10, Jquantise.E_BITS );
			Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD, 200.0f, 200.0f );
			// System.err.print("soft mute\n");
		}

		/* interpolate ------------------------------------------------*/

		/* Wo, energy, and LSPs are sampled every 40ms so we interpolate
		   the 3 frames in between */

		// PROFILE_SAMPLE( recover_start );
		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
			Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD );
			Jinterp.interp_Wo2( model[i], this.prev_model_dec, model[3], weight );
			e[i] = Jinterp.interp_energy2( this.prev_e_dec, e[3], weight );
		}

		/* then recover spectral amplitudes */
		final float ak[][] = new float[4][Jdefines.LPC_ORD+1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
						/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}
		/*
		for( i = 0; i < 4; i++ ) {
			printf( "%d Wo: %f L: %d v: %d\n", frames, model[i].Wo, model[i].L, model[i].voiced );
		}
		if( frames == 4*50 )
			exit( 0 );
		*/
		// PROFILE_SAMPLE_AND_LOG2( recover_start, "	recover" );
/* #ifdef DUMP
		dump_lsp_( &lsps[3][0] );
		dump_ak_( &ak[3][0], Jdefines.LPC_ORD );
#endif */

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_3[i];
		}
	}


	/**

	  FUNCTION....: codec2_encode_1200
	  AUTHOR......: David Rowe
	  DATE CREATED: Nov 14 2011

	  Encodes 320 speech samples (40ms of speech) into 48 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm four times:

	  frame 0: voicing bit
	  frame 1: voicing bit, joint VQ of Wo and E
	  frame 2: voicing bit
	  frame 3: voicing bit, joint VQ of Wo and E, VQ LSPs

	  The bit allocation is:

	    Parameter                      frame 2  frame 4   Total
	    -------------------------------------------------------
	    Harmonic magnitudes (LSPs)      0       27        27
	    Energy+Wo                       8        8        16
	    Voicing (10ms update)           2        2         4
	    Spare                           0        1         1
	    TOTAL                          10       38        48

	*/
	public final void codec2_encode_1200( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* frame 1: - voicing ---------------------------------------------*/
		final JMODEL model = new JMODEL();
		analyse_one_frame( model, speech, 0 );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 2: - voicing, joint Wo & E -------------------------------*/

		analyse_one_frame( model, speech, Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* need to run this just to get LPC energy */
		final float lsps[] = new float[Jdefines.LPC_ORD];
		final float ak[] = new float[Jdefines.LPC_ORD + 1];
		float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );

		int WoE_index = Jquantise.encode_WoE( model, e, this.xq_enc );
		nbit = Jpack.pack( bits, nbit, WoE_index, Jquantise.WO_E_BITS );

		/* frame 3: - voicing ---------------------------------------------*/

		analyse_one_frame( model, speech, 2 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		/* frame 4: - voicing, joint Wo & E, scalar LSPs ------------------*/

		analyse_one_frame( model, speech, 3 * Jdefines.N );
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );

		e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD );
		WoE_index = Jquantise.encode_WoE( model, e, this.xq_enc );
		nbit = Jpack.pack( bits, nbit, WoE_index, Jquantise.WO_E_BITS );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		final float lsps_[] = new float[Jdefines.LPC_ORD];
		Jquantise.encode_lsps_vq( lsp_indexes, lsps, lsps_, Jdefines.LPC_ORD );
		for( int i = 0; i < Jquantise.LSP_PRED_VQ_INDEXES; i++ ) {
			nbit = Jpack.pack( bits, nbit, lsp_indexes[i], Jquantise.lsp_pred_vq_bits( i ) );
		}
		final int spare = 0;
		nbit = Jpack.pack( bits, nbit, spare, 1 );

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_1200
	  AUTHOR......: David Rowe
	  DATE CREATED: 14 Feb 2012

	  Decodes frames of 48 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_1200( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/

		/* this will partially fill the model params for the 4 x 10ms
		   frames */
		final int[] nbit = { 0 };
		model[0].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[1].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		int WoE_index = Jpack.unpack( bits, nbit, Jquantise.WO_E_BITS );
		final float e[] = new float[4];
		e[1] = Jquantise.decode_WoE( model[1], /* &e[1],*/ this.xq_dec, WoE_index );

		model[2].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;

		model[3].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		WoE_index = Jpack.unpack( bits, nbit, Jquantise.WO_E_BITS );
		e[3] = Jquantise.decode_WoE( model[3], /* &e[3],*/ this.xq_dec, WoE_index );

		final int lsp_indexes[] = new int[Jdefines.LPC_ORD];
		for( int i = 0; i < Jquantise.LSP_PRED_VQ_INDEXES; i++ ) {
			lsp_indexes[i] = Jpack.unpack( bits, nbit, Jquantise.lsp_pred_vq_bits( i ) );
		}
		final float lsps[][] = new float[4][Jdefines.LPC_ORD];
		final float[] lsps_3 = lsps[3];// java
		Jquantise.decode_lsps_vq( lsp_indexes, lsps_3, Jdefines.LPC_ORD, 0 );
		Jquantise.check_lsp_order( lsps_3, Jdefines.LPC_ORD );
		Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD, 50.0f, 100.0f );

		/* interpolate ------------------------------------------------*/

		/* Wo and energy are sampled every 20ms, so we interpolate just 1
		   10ms frame between 20ms samples */

		Jinterp.interp_Wo( model[0], this.prev_model_dec, model[1] );
		e[0] = Jinterp.interp_energy( this.prev_e_dec, e[1] );
		Jinterp.interp_Wo( model[2], model[1], model[3] );
		e[2] = Jinterp.interp_energy( e[1], e[3] );

		/* LSPs are sampled every 40ms so we interpolate the 3 frames in
		   between, then recover spectral amplitudes */

		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
			Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD );
		}
		final float ak[][] = new float[4][Jdefines.LPC_ORD+1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD, model[i], e[i],
						/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD; i++ ) {
			dec[i] = lsps_3[i];
		}
	}

// #ifndef CORTEX_M4
	/**

	  FUNCTION....: codec2_encode_700
	  AUTHOR......: David Rowe
	  DATE CREATED: April 2015

	  Encodes 320 speech samples (40ms of speech) into 28 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm four times:

	  frame 0: nothing
	  frame 1: nothing
	  frame 2: nothing
	  frame 3: voicing bit, scalar Wo and E, 17 bit LSP MEL scalar, 2 spare

	  The bit allocation is:

	    Parameter                      frames 1-3   frame 4   Total
	    -----------------------------------------------------------
	    Harmonic magnitudes (LSPs)          0         17        17
	    Energy                              0          3         3
	    log Wo                              0          5         5
	    Voicing                             0          1         1
	    spare                               0          2         2
	    TOTAL                               0         28        28

	*/
	public final void codec2_encode_700( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* band pass filter */
		final float[] buf = this.bpf_buf;// java
		for( int i = 0, j = 4 * Jdefines.N; i < Jbpf.BPF_N; i++, j++ ) {
			buf[i] = buf[j];
		}
		for( int i = 0, j = Jbpf.BPF_N; i < 4 * Jdefines.N; i++, j++ ) {
			buf[j] = speech[i];
		}
		final float bpf_out[] = new float[4 * Jdefines.N];
		Jlpc.inverse_filter( this.bpf_buf, Jbpf.BPF_N, Jbpf.bpf, 4 * Jdefines.N, bpf_out, Jbpf.BPF_N );
		final short bpf_speech[] = new short[4 * Jdefines.N];
		for( int i = 0; i < 4 * Jdefines.N; i++ ) {
			bpf_speech[i] = (short)bpf_out[i];
		}

		/* frame 1 --------------------------------------------------------*/
		final JMODEL model = new JMODEL();
		analyse_one_frame( model, bpf_speech, 0 );

		/* frame 2 --------------------------------------------------------*/

		analyse_one_frame( model, bpf_speech, Jdefines.N );

		/* frame 3 --------------------------------------------------------*/

		analyse_one_frame( model, bpf_speech, 2 * Jdefines.N );

		/* frame 4: - voicing, scalar Wo & E, scalar LSPs -----------------*/

		analyse_one_frame( model, bpf_speech, 3 * Jdefines.N );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );
		final int Wo_index = Jquantise.encode_log_Wo( model.Wo, 5 );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, Wo_index, 5, this.gray );

		final float lsps[] = new float[Jdefines.LPC_ORD_LOW];
		final float ak[] = new float[Jdefines.LPC_ORD_LOW + 1];
		final float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD_LOW );
		final int e_index = Jquantise.encode_energy( e, 3 );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, e_index, 3, this.gray );

		final float mel[] = new float[Jdefines.LPC_ORD_LOW];
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			final float f = ( 4000.0f / Jdefines.PI ) * lsps[i];
			mel[i] = (float)Math.floor( 2595.0 * Math.log10( 1.0 + (double)f / 700.0 ) + 0.5 );
		}
		final int indexes[] = new int[Jdefines.LPC_ORD_LOW];
		Jquantise.encode_mels_scalar( indexes, mel, Jdefines.LPC_ORD_LOW );

		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			nbit = Jpack.pack_natural_or_gray( bits, nbit, indexes[i], Jquantise.mel_bits( i ), this.gray );
		}

		final int spare = 0;
		nbit = Jpack.pack_natural_or_gray( bits, nbit, spare, 2, this.gray );

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}


	/**

	  FUNCTION....: codec2_decode_700
	  AUTHOR......: David Rowe
	  DATE CREATED: April 2015

	  Decodes frames of 28 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_700( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/
		final int[] nbit = { 0 };
		model[3].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		model[0].voiced = model[1].voiced = model[2].voiced = model[3].voiced;

		final int Wo_index = Jpack.unpack_natural_or_gray( bits, nbit, 5, this.gray );
		model[3].Wo = Jquantise.decode_log_Wo( Wo_index, 5 );
		model[3].L  = (int)(Jdefines.PI / model[3].Wo);

		final int e_index = Jpack.unpack_natural_or_gray( bits, nbit, 3, this.gray );
		final float e[] = new float[4];
		e[3] = Jquantise.decode_energy( e_index, 3 );

		final int indexes[] = new int[Jdefines.LPC_ORD_LOW];
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			indexes[i] = Jpack.unpack_natural_or_gray( bits, nbit, Jquantise.mel_bits( i ), this.gray );
		}

		final float mel[] = new float[Jdefines.LPC_ORD_LOW];
		Jquantise.decode_mels_scalar( mel, indexes, Jdefines.LPC_ORD_LOW );
		final float lsps[][] = new float[4][Jdefines.LPC_ORD_LOW];
		final float[] lsps_3 = lsps[3];// java
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			final float f_ = 700.0f * ((float)Math.pow( 10.0, (double)(mel[i] / 2595.0f) ) - 1.0f);
			lsps_3[i] = f_ * ( Jdefines.PI / 4000.0f );
			//printf( "lsps[3][%d]  %f\n", i, lsps[3][i] );
		}

		Jquantise.check_lsp_order( lsps_3, Jdefines.LPC_ORD_LOW );
		Jquantise.bw_expand_lsps( lsps_3, Jdefines.LPC_ORD_LOW, 50.0f, 100.0f );

/* # ifdef MASK_NOT_FOR_NOW
		// first pass at soft decn error masking, needs further work
		// If soft dec info available expand further for low power frames

		if( c2.softdec != null ) {
			float ee = 0.0f;
			for( int i = 9; i < 9+17f; i++ ) {
				ee += c2.softdec[i] * c2.softdec[i];
			}
			ee /= 6.0f;
			//fprintf( stderr, "e: %f\n", e );
			//if( e < 0.3 )
			//	  bw_expand_lsps( &lsps[3][0], LPC_ORD_LOW, 150.0, 300.0 );
		}
#endif */

		/* interpolate ------------------------------------------------*/

		/* LSPs, Wo, and energy are sampled every 40ms so we interpolate
		   the 3 frames in between, then recover spectral amplitudes */

		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
		Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD_LOW );
			Jinterp.interp_Wo2( model[i], this.prev_model_dec, model[3], weight );
			e[i] = Jinterp.interp_energy2( this.prev_e_dec, e[3], weight );
		}
		final float ak[][] = new float[4][Jdefines.LPC_ORD_LOW + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD_LOW );
			/* float snr =*/ Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD_LOW, model[i], e[i],
						/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

/* #ifdef DUMP
		dump_lsp_( &lsps[3][0] );
		dump_ak_( &ak[3][0], LPC_ORD_LOW );
		dump_model( &model[3] );
		if( c2.softdec )
			dump_softdec( c2.softdec, nbit );
#endif */

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			dec[i] = lsps_3[i];
		}
	}

	/**

	  FUNCTION....: codec2_encode_700b
	  AUTHOR......: David Rowe
	  DATE CREATED: August 2015

	  Version b of 700 bit/s codec.  After some experiments over the air I
	  wanted was unhappy with the rate 700 codec so spent a few weeks
	  trying to improve the speech quality. This version uses a wider BPF
	  and vector quantised mel-lsps.

	  Encodes 320 speech samples (40ms of speech) into 28 bits.

	  The codec2 algorithm actually operates internally on 10ms (80
	  sample) frames, so we run the encoding algorithm four times:

	  frame 0: nothing
	  frame 1: nothing
	  frame 2: nothing
	  frame 3: voicing bit, 5 bit scalar Wo and 3 bit E, 18 bit LSP MEL VQ,
	           1 spare

	  The bit allocation is:

	    Parameter                      frames 1-3   frame 4   Total
	    -----------------------------------------------------------
	    Harmonic magnitudes (LSPs)          0         18        18
	    Energy                              0          3         3
	    log Wo                              0          5         5
	    Voicing                             0          1         1
	    spare                               0          1         1
	    TOTAL                               0         28        28

	*/
	public final void codec2_encode_700b( final byte[] bits, final short speech[] )
	{
		// assert( c2 != NULL );

		for( int i = 0, ie = ((codec2_bits_per_frame() + 7) >>> 3); i < ie; i++ ) {
			bits[i] = 0;
		}

		/* band pass filter */

		for( int i = 0, j = 4 * Jdefines.N; i < Jbpf.BPF_N; i++, j++ ) {
			this.bpf_buf[i] = this.bpf_buf[j];
		}
		for( int i = 0, j = Jbpf.BPF_N; i < 4 * Jdefines.N; i++, j++ ) {
			this.bpf_buf[j] = speech[i];
		}
		final float bpf_out[] = new float[4 * Jdefines.N];
		Jlpc.inverse_filter( this.bpf_buf, Jbpf.BPF_N, Jbpfb.bpfb, 4 * Jdefines.N, bpf_out, Jbpf.BPF_N );
		final short bpf_speech[] = new short[4 * Jdefines.N];
		for( int i = 0; i < 4 * Jdefines.N; i++ ) {
			bpf_speech[i] = (short)bpf_out[i];
		}

		/* frame 1 --------------------------------------------------------*/
		final JMODEL model = new JMODEL();
		analyse_one_frame( model, bpf_speech, 0 );

		/* frame 2 --------------------------------------------------------*/

		analyse_one_frame( model, bpf_speech, Jdefines.N );

		/* frame 3 --------------------------------------------------------*/

		analyse_one_frame( model, bpf_speech, 2 * Jdefines.N );

		/* frame 4: - voicing, scalar Wo & E, VQ mel LSPs -----------------*/

		analyse_one_frame( model, bpf_speech, 3 * Jdefines.N );
		int nbit = 0;
		nbit = Jpack.pack( bits, nbit, model.voiced ? 1 : 0, 1 );
		final int Wo_index = Jquantise.encode_log_Wo( model.Wo, 5 );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, Wo_index, 5, this.gray );

		final float lsps[] = new float[Jdefines.LPC_ORD_LOW];
		final float ak[] = new float[Jdefines.LPC_ORD_LOW + 1];
		final float e = Jquantise.speech_to_uq_lsps( lsps, ak, this.Sn, this.w, Jdefines.LPC_ORD_LOW );
		final int e_index = Jquantise.encode_energy( e, 3 );
		nbit = Jpack.pack_natural_or_gray( bits, nbit, e_index, 3, this.gray );

		final float mel[] = new float[Jdefines.LPC_ORD_LOW];
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			final float f = (4000.0f / Jdefines.PI) * lsps[i];
			mel[i] = (float)Math.floor( 2595.0 * Math.log10( (double)(1.0f + f / 700.0f) ) + 0.5 );
		}
		final float mel_[] = new float[Jdefines.LPC_ORD_LOW];
		final int indexes[] = new int[3];
		Jquantise.lspmelvq_mbest_encode( indexes, mel, mel_, Jdefines.LPC_ORD_LOW, 5 );

		for( int i = 0; i < 3; i++ ) {
			nbit = Jpack.pack_natural_or_gray( bits, nbit, indexes[i], Jquantise.lspmelvq_cb_bits( i ), this.gray );
		}

		final int spare = 0;
		nbit = Jpack.pack_natural_or_gray( bits, nbit, spare, 1, this.gray );

		// assert( nbit == ( unsigned )codec2_bits_per_frame( c2 ) );
	}

	private static final int MEL_ROUND = 10;
	/**

	  FUNCTION....: codec2_decode_700b
	  AUTHOR......: David Rowe
	  DATE CREATED: August 2015

	  Decodes frames of 28 bits into 320 samples (40ms) of speech.

	*/
	public final void codec2_decode_700b( final short speech[], final byte[] bits )
	{
		// assert( c2 != NULL );

		/* only need to zero these out due to ( unused ) snr calculation */
		final JMODEL model[] = new JMODEL[4];
		for( int i = 0; i < 4; i++ ) {
			model[i] = new JMODEL();// java
			/* for( int j = 1; j <= Jdefines.MAX_AMP; j++ ) {// java: already zeroed
				model[i].A[j] = 0.0f;
			}*/
		}

		/* unpack bits from channel ------------------------------------*/
		final int[] nbit = { 0 };
		model[3].voiced = Jpack.unpack( bits, nbit, 1 ) != 0;
		model[0].voiced = model[1].voiced = model[2].voiced = model[3].voiced;

		final int Wo_index = Jpack.unpack_natural_or_gray( bits, nbit, 5, this.gray );
		model[3].Wo = Jquantise.decode_log_Wo( Wo_index, 5 );
		model[3].L  = (int)(Jdefines.PI / model[3].Wo);

		final int e_index = Jpack.unpack_natural_or_gray( bits, nbit, 3, this.gray );
		final float e[] = new float[4];
		e[3] = Jquantise.decode_energy( e_index, 3 );

		final int indexes[] = new int[3];
		for( int i = 0; i < 3; i++ ) {
			indexes[i] = Jpack.unpack_natural_or_gray( bits, nbit, Jquantise.lspmelvq_cb_bits( i ), this.gray );
		}

		final float mel[] = new float[Jdefines.LPC_ORD_LOW];
		Jquantise.lspmelvq_decode( indexes, mel, Jdefines.LPC_ORD_LOW );

		for( int i = 1; i < Jdefines.LPC_ORD_LOW; i++ ) {
			if( mel[i] <= mel[i - 1] + MEL_ROUND ) {
				mel[i] += MEL_ROUND / 2;
				mel[i - 1] -= MEL_ROUND / 2;
				i = 1;
			}
		}

		final float lsps[][] = new float[4][Jdefines.LPC_ORD_LOW];
		final float[] lsps_3 = lsps[3];// java
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			final float f_ = 700.0f * ((float)Math.pow( 10.0, (double)(mel[i] / 2595.0f) ) - 1.0f);
			lsps_3[i] = f_ * (Jdefines.PI / 4000.0f);
			//printf( "lsps[3][%d]  %f\n", i, lsps[3][i] );
		}

		/* interpolate ------------------------------------------------*/

		/* LSPs, Wo, and energy are sampled every 40ms so we interpolate
		   the 3 frames in between, then recover spectral amplitudes */

		float weight = 0.25f;
		for( int i = 0; i < 3; i++, weight += 0.25f ) {
			Jinterp.interpolate_lsp_ver2( lsps[i], this.prev_lsps_dec, lsps_3, weight, Jdefines.LPC_ORD_LOW );
			Jinterp.interp_Wo2( model[i], this.prev_model_dec, model[3], weight );
			e[i] = Jinterp.interp_energy2( this.prev_e_dec, e[3],weight );
		}
		final float ak[][] = new float[4][Jdefines.LPC_ORD_LOW + 1];
		final Jkiss_fft_cpx	Aw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Aw[i] = new Jkiss_fft_cpx();
		}
		for( int i = 0, j = 0; i < 4; i++, j += Jdefines.N ) {
			Jlsp.lsp_to_lpc( lsps[i], ak[i], Jdefines.LPC_ORD_LOW );
			Jquantise.aks_to_M2( this.fft_fwd_cfg, ak[i], Jdefines.LPC_ORD_LOW, model[i], e[i],
						/* &snr, false,*/ false, this.lpc_pf, this.bass_boost, this.beta, this.gamma, Aw );
			model[i].apply_lpc_correction();
			synthesise_one_frame( speech, j, model[i], Aw );
		}

/* #ifdef DUMP
		dump_lsp_( &lsps[3][0] );
		dump_ak_( &ak[3][0], LPC_ORD_LOW );
		dump_model( &model[3] );
		if( c2.softdec ) {
			dump_softdec( c2.softdec, nbit );
		}
#endif */

		/* update memories for next frame ----------------------------*/

		this.prev_model_dec = model[3];
		this.prev_e_dec = e[3];
		final float[] dec = this.prev_lsps_dec;// java
		for( int i = 0; i < Jdefines.LPC_ORD_LOW; i++ ) {
			dec[i] = lsps_3[i];
		}
	}
// #endif// CORTEX_M4

	/**

	  FUNCTION....: synthesise_one_frame()
	  AUTHOR......: David Rowe
	  DATE CREATED: 23/8/2010

	  Synthesise 80 speech samples (10ms) from model parameters.

	*/
	private final void synthesise_one_frame( final short speech[], int offset, final JMODEL model, final Jkiss_fft_cpx Aw[] )
	{
		// PROFILE_VAR( phase_start, pf_start, synth_start );

/* #ifdef DUMP
		dump_quantised_model( model );
#endif */

		// PROFILE_SAMPLE( phase_start );

		this.ex_phase = Jphase.phase_synth_zero_order( this.fft_fwd_cfg, model, this.ex_phase, Aw );

		// PROFILE_SAMPLE_AND_LOG( pf_start, phase_start, "	phase_synth" );

		this.bg_est = model.postfilter( this.bg_est );

		// PROFILE_SAMPLE_AND_LOG( synth_start, pf_start, "	postfilter" );

		synthesise( this.fft_inv_cfg, this.Sn_, model, this.Pn, true );

		// PROFILE_SAMPLE_AND_LOG2( synth_start, "	synth" );

		ear_protection( this.Sn_, Jdefines.N );

		final float[] snd = this.Sn_;// java
		for( int i = 0; i < Jdefines.N; i++, offset++ ) {
			if( snd[i] > 32767.0f ) {
				speech[offset] = 32767;
			} else if( snd[i] < -32767.0f ) {
				speech[offset] = -32767;
			} else {
				speech[offset] = (short)snd[i];
			}
		}

	}

	/**

	  FUNCTION....: analyse_one_frame()
	  AUTHOR......: David Rowe
	  DATE CREATED: 23/8/2010

	  Extract sinusoidal model parameters from 80 speech samples (10ms of
	  speech).

	*/
	private final void analyse_one_frame( final JMODEL model, final short speech[], int offset )
	{
		// PROFILE_VAR( dft_start, nlp_start, model_start, two_stage, estamps );

		/* Read input speech */
		final float[] snd = this.Sn;// java
		for( int i = 0, j = Jdefines.N; i < Jdefines.M - Jdefines.N; i++, j++ ) {
			snd[i] = snd[j];
		}
		for( int i = Jdefines.M - Jdefines.N, ie = i + Jdefines.N; i < ie; i++ ) {
			snd[i] = speech[offset++];
		}

		// PROFILE_SAMPLE( dft_start );
		final Jkiss_fft_cpx	Sw[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Sw[i] = new Jkiss_fft_cpx();
		}
		dft_speech( this.fft_fwd_cfg, Sw, snd, this.w );
		// PROFILE_SAMPLE_AND_LOG( nlp_start, dft_start, "	dft_speech" );

		/* Estimate pitch */
		// final float[] pitch = new float[1];
		final float f0 = this.nlp.nlp( snd, Jdefines.N, Jdefines.P_MIN, Jdefines.P_MAX,/* pitch,*/ Sw, this.W, this.prev_Wo_enc );
		// PROFILE_SAMPLE_AND_LOG( model_start, nlp_start, "	nlp" );

		// model.Wo = Jdefines.TWO_PI / pitch[0];
		model.Wo = Jdefines.TWO_PI / (Jnlp.SAMPLE_RATE / f0);// java changed
		model.L = (int)(Jdefines.PI / model.Wo);

		/* estimate model parameters */

		model.two_stage_pitch_refinement( Sw );
		// PROFILE_SAMPLE_AND_LOG( two_stage, model_start, "	two_stage" );
		model.estimate_amplitudes( Sw, this.W, false );
		// PROFILE_SAMPLE_AND_LOG( estamps, two_stage, "	est_amps" );
		final Jkiss_fft_cpx	Sw_[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		final Jkiss_fft_cpx	Ew[] = new Jkiss_fft_cpx[Jdefines.FFT_ENC];
		model.est_voicing_mbe( Sw, this.W, Sw_, Ew );
		this.prev_Wo_enc = model.Wo;
		// PROFILE_SAMPLE_AND_LOG2( estamps, "	est_voicing" );
/* #ifdef DUMP
		dump_model( model );
#endif */
	}

	/**

	  FUNCTION....: ear_protection()
	  AUTHOR......: David Rowe
	  DATE CREATED: Nov 7 2012

	  Limits output level to protect ears when there are bit errors or the input
	  is overdriven.  This doesn't correct or mask bit errors, just reduces the
	  worst of their damage.

	*/
	private static final void ear_protection( final float in_out[], final int n ) {

		/* find maximum sample in frame */

		float max_sample = 0.0f;
		for( int i = 0; i < n; i++ ) {
			if( in_out[i] > max_sample ) {
				max_sample = in_out[i];
			}
		}

		/* determine how far above set point */

		final float over = max_sample / 30000.0f;

		/* If we are x dB over set point we reduce level by 2x dB, this
		   attenuates major excursions in amplitude ( likely to be caused
		   by bit errors ) more than smaller ones */

		if( over > 1.0f ) {
			final float gain = 1.0f / ( over * over );
			//fprintf( stderr, "gain: %f\n", gain );
			for( int i = 0; i < n; i++ ) {
				in_out[i] *= gain;
			}
		}
	}

	public final void codec2_set_lpc_post_filter( final boolean enable, final boolean is_bass_boost, final float lpc_beta, final float lpc_gamma )
	{
		// assert( ( beta >= 0.0 ) && ( beta <= 1.0 ) );
		// assert( ( gamma >= 0.0 ) && ( gamma <= 1.0 ) );
		this.lpc_pf = enable;
		this.bass_boost = is_bass_boost;
		this.beta = lpc_beta;
		this.gamma = lpc_gamma;
	}

	/**
	   Allows optional stealing of one of the voicing bits for use as a
	   spare bit, only 1300 & 1400 & 1600 bit/s supported for now.
	   Experimental method of sending voice/data frames for FreeDV.
	*/
	/* static final int codec2_get_spare_bit_index( final Jcodec2 c2 )// java: uses only for freedv
	{
		// assert( c2 != NULL );

		switch( c2.mode ) {
		case CODEC2_MODE_1300:
			return 2; // bit 2 ( 3th bit ) is v2 ( third voicing bit )
			// break;
		case CODEC2_MODE_1400:
			return 10; // bit 10 ( 11th bit ) is v2 ( third voicing bit )
			// break;
		case CODEC2_MODE_1600:
			return 15; // bit 15 ( 16th bit ) is v2 ( third voicing bit )
			// break;
		case CODEC2_MODE_700:
			return 26; // bits 26 and 27 are spare
			// break;
		case CODEC2_MODE_700B:
			return 27; // bit 27 is spare
			// break;
		}

		return -1;
	}*/

	/**
	   Reconstructs the spare voicing bit.  Note works on unpacked bits
	   for convenience.
	*/
	/* private static final int codec2_rebuild_spare_bit( final Jcodec2 c2, final int unpacked_bits[] )// java: uses only for freedv
	{
		// assert( c2 != NULL );

		final int v1 = unpacked_bits[1];

		switch( c2.mode ) {
		case CODEC2_MODE_1300:

			int v3 = unpacked_bits[1 + 1 + 1];

			// if either adjacent frame is voiced, make this one voiced

			unpacked_bits[2] = (v1 != 0 || v3 != 0) ? 1 : 0;

			return 0;

			//break;

		case CODEC2_MODE_1400:

			v3 = unpacked_bits[1 + 1 + 8 + 1];

			// if either adjacent frame is voiced, make this one voiced

			unpacked_bits[10] = (v1 != 0 || v3 != 0) ? 1 : 0;

			return 0;

			// break;

		case CODEC2_MODE_1600:
			v3 = unpacked_bits[1 + 1 + 8 + 5 + 1];

			// if either adjacent frame is voiced, make this one voiced

			unpacked_bits[15] = (v1 != 0 || v3 != 0) ? 1 : 0;

			return 0;

			// break;
		}

		return -1;
	}*/

	public final void codec2_set_natural_or_gray( final boolean is_gray )
	{
		// assert( c2 != NULL );
		this.gray = is_gray;
	}

	// java: softdec not uses in the production code
	/* public static final void codec2_set_softdec( final Jcodec2 c2, final float[] softdec )
	{
		//assert( c2 != NULL );
		c2.softdec = softdec;
	}*/

	// java: from sine.c
	private static int next = 1;// uint32

	public static final int codec2_rand() {
		next = next * 1103515245 + 12345;
		return( ( next >>> 16 ) & 32767 );
	}
}