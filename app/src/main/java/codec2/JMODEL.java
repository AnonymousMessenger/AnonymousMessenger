package codec2;

/** Structure to hold model parameters for one frame */
final class JMODEL {
	/** fundamental frequency estimate in radians  */
	float Wo;
	/** number of harmonics */
	int   L;
	/** amplitiude of each harmonic */
	final float A[] = new float[Jdefines.MAX_AMP + 1];
	/** phase of each harmonic */
	final float phi[] = new float[Jdefines.MAX_AMP + 1];
	/** non-zero if this frame is voiced */
	boolean voiced;
	//
	// start postfilter.c
	/** only consider low levels signals for bg_est */
	private static final float BG_THRESH = 40.0f;
	/** averaging filter constant */
	private static final float BG_BETA   = 0.1f;
	/** harmonics this far above BG noise are
	randomised.  Helped make bg noise less
	spikey (impulsive) for mmt1, but speech was
	perhaps a little rougher. */
	private static final float BG_MARGIN = 6.0f;
	/**

	  postfilter()

	  The post filter is designed to help with speech corrupted by
	  background noise.  The zero phase model tends to make speech with
	  background noise sound "clicky".  With high levels of background
	  noise the low level inter-formant parts of the spectrum will contain
	  noise rather than speech harmonics, so modelling them as voiced
	  (i.e. a continuous, non-random phase track) is inaccurate.

	  Some codecs (like MBE) have a mixed voicing model that breaks the
	  spectrum into voiced and unvoiced regions.  Several bits/frame
	  (5-12) are required to transmit the frequency selective voicing
	  information.  Mixed excitation also requires accurate voicing
	  estimation (parameter estimators always break occasionally under
	  exceptional conditions).

	  In our case we use a post filter approach which requires no
	  additional bits to be transmitted.  The decoder measures the average
	  level of the background noise during unvoiced frames.  If a harmonic
	  is less than this level it is made unvoiced by randomising it's
	  phases.

	  This idea is rather experimental.  Some potential problems that may
	  happen:

	  1/ If someone says "aaaaaaaahhhhhhhhh" will background estimator track
		 up to speech level?  This would be a bad thing.

	  2/ If background noise suddenly dissapears from the source speech does
		 estimate drop quickly?  What is noise suddenly re-appears?

	  3/ Background noise with a non-flat sepctrum.  Current algorithm just
		 comsiders scpetrum as a whole, but this could be broken up into
		 bands, each with their own estimator.

	  4/ Males and females with the same level of background noise.  Check
		 performance the same.  Changing Wo affects width of each band, may
		 affect bg energy estimates.

	  5/ Not sure what happens during long periods of voiced speech
		 e.g. "sshhhhhhh"

	  @return java: new value of the bg_est

	*/
	final float postfilter(float bg_est)
	{
		/* determine average energy across spectrum */
		final int n = this.L;// java
		final float[] a = this.A;// java
		float e = 1E-12f;
		for( int m = 1; m <= n; m++ ) {
			final float v = a[m];// java
			e += v * v;
		}

		// assert(e > 0.0);
		e = 10.0f * (float)Math.log10( (double)(e / (float)n) );

		/* If beneath threhold, update bg estimate.  The idea
		 of the threshold is to prevent updating during high level
		 speech. */

		if( (e < BG_THRESH) && ! this.voiced ) {
			bg_est =  bg_est * (1.0f - BG_BETA) + e * BG_BETA;
		}

		/* now mess with phases during voiced frames to make any harmonics
		 less then our background estimate unvoiced.
		*/

		final float thresh = (float)Math.pow( 10.0, (double)((bg_est + BG_MARGIN) / 20.0f) );
		if( this.voiced ) {
			final float[] phase = this.phi;// java
			for( int m = 1; m <= n; m++ ) {
				if( a[m] < thresh ) {
					phase[m] = Jdefines.TWO_PI * (float)Jcodec2.codec2_rand() / (float)Jcodec2.CODEC2_RAND_MAX;
				}
			}
		}

/* #ifdef DUMP
		dump_bg( e, *bg_est, 100.0 * uv / model.L );
#endif */
		return bg_est;// java
	}
	// end postfilter.c

	// start interp.c
	/**

	FUNCTION....: sample_log_amp()
	AUTHOR......: David Rowe
	DATE CREATED: 22/8/10

	Samples the amplitude envelope at an arbitrary frequency w.  Uses
	linear interpolation in the log domain to sample between harmonic
	amplitudes.

	*/
	/* private final float sample_log_amp(final float w)
	{// java never uses
		// assert(w > 0.0); assert (w <= PI);

		final int m = (int)Math.floor( (double)(w / this.Wo + 0.5f) );
		final float f = (w - m * this.Wo) / w;
		// assert(f <= 1.0);

		final float log_amp;
		if( m < 1 ) {
			log_amp = f * (float)Math.log10( (double)(this.A[1] + 1E-6f) );
		}
		else if( (m + 1) > this.L ) {
			log_amp = (1.0f - f) * (float)Math.log10( (double)(this.A[this.L] + 1E-6f) );
		}
		else {
			log_amp = (1.0f - f) * (float)Math.log10( (double)(this.A[m] + 1E-6f) ) +
						f * (float)Math.log10( (double)(this.A[m + 1] + 1E-6f) );
		}

		return log_amp;
	}*/

	// end interp.c

	// start quantise.c
	/**

	  FUNCTION....: apply_lpc_correction()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/2010

	  Apply first harmonic LPC correction at decoder.  This helps improve
	  low pitch males after LPC modelling, like hts1a and morig.

	*/
	final void apply_lpc_correction()
	{
		if( this.Wo < (Jdefines.PI * 150.0f / 4000f) ) {
			this.A[1] *= 0.032f;
		}
	}
	// end quantise.c

	// start sine.c
	/**

	 FUNCTION....: hs_pitch_refinement
	 AUTHOR......: David Rowe
	 DATE CREATED: 27/5/94

	 Harmonic sum pitch refinement function.

	 pmin   pitch search range minimum
	 pmax	pitch search range maximum
	 step   pitch search step size
	 model	current pitch estimate in model.Wo

	 model 	refined pitch estimate in model.Wo

	*/
	final void hs_pitch_refinement( final Jkiss_fft_cpx Sw[], final float pmin, final float pmax, final float pstep )
	{
		/* Initialisation */

		final int n = (int)(Jdefines.PI / this.Wo);	/* use initial pitch est. for L */
		this.L = n;
		float Wom = this.Wo;/* Wo that maximises E */
		float Em = 0.0f;/* mamimum energy */
		final float r = Jdefines.TWO_PI / Jdefines.FFT_ENC;/* number of rads/bin */
		// final float one_on_r = 1.0f / r;

		/* Determine harmonic sum for a range of Wo values */

		for( float p = pmin; p <= pmax; p += pstep ) {/* current pitch */
			float E = 0.0f;/* energy for current pitch*/
			final float test_wo = Jdefines.TWO_PI / p;/* current "test" fundamental freq. */
			final float WoR = test_wo / r;// java

			/* Sum harmonic magnitudes */
			for( int m = 1; m <= n; m++ ) {
				final int b = (int)( m * WoR + 0.5f );/* bin for current harmonic centre */
				E += Sw[b].r * Sw[b].r + Sw[b].i * Sw[b].i;
			}
			/* Compare to see if this is a maximum */

			if( E > Em ) {
				Em = E;
				Wom = test_wo;
			}
		}

		this.Wo = Wom;
	}

	/**

	  FUNCTION....: two_stage_pitch_refinement
	  AUTHOR......: David Rowe
	  DATE CREATED: 27/5/94

	  Refines the current pitch estimate using the harmonic sum pitch
	  estimation technique.

	*/
	final void two_stage_pitch_refinement( final Jkiss_fft_cpx Sw[] )
	{
		/* Coarse refinement */
		/* pitch refinment minimum, maximum and step */
		float w = Jdefines.TWO_PI / this.Wo;// java
		float pmax = w + 5f;
		float pmin = w - 5f;
		float pstep = 1.0f;
		hs_pitch_refinement( Sw, pmin, pmax, pstep );

		/* Fine refinement */
		w = Jdefines.TWO_PI / this.Wo;// java
		pmax = w + 1f;
		pmin = w - 1f;
		pstep = 0.25f;
		hs_pitch_refinement( Sw, pmin, pmax, pstep );

		/* Limit range */

		if( this.Wo < Jdefines.TWO_PI / Jdefines.P_MAX ) {
			this.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;
		}
		if ( this.Wo > Jdefines.TWO_PI / Jdefines.P_MIN ) {
			this.Wo = Jdefines.TWO_PI / Jdefines.P_MIN;
		}

		this.L = (int)Math.floor( (double)(Jdefines.PI / this.Wo) );
	}
	/**

	  FUNCTION....: estimate_amplitudes
	  AUTHOR......: David Rowe
	  DATE CREATED: 27/5/94

	  Estimates the complex amplitudes of the harmonics.

	*/
	final void estimate_amplitudes( final Jkiss_fft_cpx Sw[], final Jkiss_fft_cpx W[], final boolean est_phase )
	{
		// final COMP  Am;// FIXME why need Am?

		// final float r = Jdefines.TWO_PI / Jdefines.FFT_ENC;/* number of rads/bin */
		// final float one_on_r = 1.0f / r;// FIXME why need?
		final float wo_r = this.Wo / Jdefines.TWO_PI * Jdefines.FFT_ENC;// java
		final float[] a = this.A;// java
		final float[] ph = this.phi;// java

		for( int m = 1, n = this.L; m <= n; m++ ) {
			// float den = 0.0f;
			final int am = (int)( ( m - 0.5f ) * wo_r + 0.5f );/* bound of current harmonic */
			final int bm = (int)( ( m + 0.5f ) * wo_r + 0.5f );/* bound of current harmonic */
			final int b = (int)( m * wo_r + 0.5f );/* DFT bin of centre of current harmonic */

			/* Estimate ampltude of harmonic */

			float den = 0.0f;/* denominator of amplitude expression */
			for( int i = am; i < bm; i++ ) {
				den += Sw[i].r * Sw[i].r + Sw[i].i * Sw[i].i;
				// Am.real += Sw[i].r * W[offset].r;
				// Am.imag += Sw[i].i * W[offset].r;
			}

			a[m] = (float)Math.sqrt( (double)den );

			if( est_phase ) {

				/* Estimate phase of harmonic, this is expensive in CPU for
				embedded devicesso we make it an option */

				ph[m] = (float)Math.atan2( (double)Sw[b].i, (double)Sw[b].r );
			}
		}
	}
	/**

	  est_voicing_mbe(  )

	  Returns the error of the MBE cost function for a fiven F0.

	  Note: I think a lot of the operations below can be simplified as
	  W[].imag = 0 and has been normalised such that den always equals 1.

	  @param model
	  @param Sw
	  @param W
	  @param Sw_ DFT of all voiced synthesised signal, useful for debugging/dump file
	  	java: elements of the Sw_ may be null
	  @param Ew DFT of error. java: elements of the Sw_ may be null

	*/
	final float est_voicing_mbe(final Jkiss_fft_cpx Sw[], final Jkiss_fft_cpx W[], final Jkiss_fft_cpx Sw_[], final Jkiss_fft_cpx Ew[])
	{
		// final COMP  Am;             /* amplitude sample for this band */

		final int n = this.L;
		final int count = n >> 2;// java
		final float[] a = this.A;// java
		float sig = 1E-4f;
		for( int i = 1; i <= count; i++ ) {
			final float v = a[i];// java
			sig += v * v;
		}
		for( int i = 0; i < Jdefines.FFT_ENC; i++ ) {
			Sw_[i] = new Jkiss_fft_cpx();
			// Sw_[i].r = 0.0f;
			// Sw_[i].i = 0.0f;
			Ew[i] = new Jkiss_fft_cpx();
			// Ew[i].r = 0.0f;
			// Ew[i].i = 0.0f;
		}

		final float wo = this.Wo * Jdefines.FFT_ENC / Jdefines.TWO_PI;// java changed
		float error = 1E-4f;/* accumulated error between original and synthesised */

		/* Just test across the harmonics in the first 1000 Hz ( L/4 ) */

		for( int i = 1; i <= count; i++ ) {
			float am_real = 0.0f;
			float am_imag = 0.0f;
			float den = 0.0f;/* denominator of Am expression */
			final int al = (int)Math.ceil( (double)(( i - 0.5f ) * wo) );
			final int bl = (int)Math.ceil( (double)(( i + 0.5f ) * wo) );

			/* Estimate amplitude of harmonic assuming harmonic is totally voiced */

			final int offset = (int)(Jdefines.FFT_ENC / 2 - (float)i * wo + 0.5f);/* centers Hw[] about current harmonic */
			for( int m = al, o = offset + m; m < bl; m++, o++ ) {
				final float wr = W[o].r;// java
				final Jkiss_fft_cpx sw = Sw[m];// java
				am_real += sw.r * wr;
				am_imag += sw.i * wr;
				den += wr * wr;
			}

			am_real /= den;
			am_imag /= den;

			/* Determine error between estimated harmonic and original */

			// offset = (int)(Jdefines.FFT_ENC / 2 - (float)l * Wo + 0.5f);
			for( int m = al, o = offset + m; m < bl; m++, o++ ) {
				float t = W[o].r;// java
				final Jkiss_fft_cpx sw_m = Sw_[m];// java
				final Jkiss_fft_cpx sw = Sw[m];// java
				final Jkiss_fft_cpx ew = Ew[m];// java
				sw_m.r = am_real * t;
				sw_m.i = am_imag * t;
				t = sw.r - sw_m.r;
				ew.r = t;
				error += t * t;
				t = sw.i - sw_m.i;
				ew.i = t;
				error += t * t;
			}
		}

		final float snr = 10.0f * (float)Math.log10( (double)(sig / error) );
		this.voiced = ( snr > Jdefines.V_THRESH );

		/* post processing, helps clean up some voicing errors ------------------*/

		/*
		   Determine the ratio of low freqency to high frequency energy,
		   voiced speech tends to be dominated by low frequency energy,
		   unvoiced by high frequency. This measure can be used to
		   determine if we have made any gross errors.
		*/

		float elow, ehigh;
		elow = ehigh = 1E-4f;
		for( int i = 1, ie = n >> 1; i <= ie; i++ ) {
			final float v = a[i];// java
			elow += v * v;
		}
		for( int i = n >> 1; i <= n; i++ ) {
			final float v = a[i];// java
			ehigh += v * v;
		}
		final float eratio = 10.0f * (float)Math.log10( (double)(elow / ehigh) );

		/* Look for Type 1 errors, strongly V speech that has been
		   accidentally declared UV */

		if( ! this.voiced ) {
			if( eratio > 10.0f ) {
				this.voiced = true;
			}
		}

		/* Look for Type 2 errors, strongly UV speech that has been
		   accidentally declared V */

		if( this.voiced ) {
			if ( eratio < -10.0f ) {
				this.voiced = false;
			}

			/* A common source of Type 2 errors is the pitch estimator
			   gives a low ( 50Hz ) estimate for UV speech, which gives a
			   good match with noise due to the close harmoonic spacing.
			   These errors are much more common than people with 50Hz3
			   pitch, so we have just a small eratio threshold. */

			final float sixty = 60.0f * Jdefines.TWO_PI / Jdefines.FS;
			if( ( eratio < -4.0f ) && ( this.Wo <= sixty ) ) {
				this.voiced = false;
			}
		}
		//printf( " v: %d snr: %f eratio: %3.2f %f\n",model.voiced,snr,eratio,dF0 );

		return snr;
	}
	// end sine.c
}
