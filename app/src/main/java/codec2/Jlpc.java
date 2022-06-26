package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: lpc.c
  AUTHOR......: David Rowe
  DATE CREATED: 30 Sep 1990 (!)

  Linear Prediction functions written in C.

\*---------------------------------------------------------------------------*/

/*
  Copyright (C) 2009-2012 David Rowe

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

// lpc.c

final class Jlpc {
	/** maximum no. of samples in frame */
	// private static final int LPC_MAX_N = 512;
	/** mathematical constant */
	// private static final float PI    = 3.141592654f;

	// private static final float ALPHA = 1.0f;
	// private static final float BETA  = 0.94f;

	/**

	  pre_emp()

	  Pre-emphasise (high pass filter with zero close to 0 Hz) a frame of
	  speech samples.  Helps reduce dynamic range of LPC spectrum, giving
	  greater weight and hense a better match to low energy formants.

	  Should be balanced by de-emphasis of the output speech.

	  @param Sn_pre output frame of speech samples
	  @param Sn input frame of speech samples
	  @param mem Sn[-1] single sample memory
	  @param Nsam number of speech samples to use

	*/
	/* private static final void pre_emp( final float Sn_pre[], final float Sn[], final float[] mem, final int Nsam )
	{// java: never uses
		for( int i = 0; i < Nsam; i++ ) {
			final float s = Sn[i];// java
			Sn_pre[i] = s - ALPHA * mem[0];
			mem[0] = s;
		}
	}*/


	/**

	  de_emp()

	  De-emphasis filter (low pass filter with a pole close to 0 Hz).

	  @param Sn_de output frame of speech samples
	  @param Sn input frame of speech samples
	  @param mem Sn[-1] single sample memory
	  @param Nsam number of speech samples to use

	*/
	/* private static final void de_emp( final float Sn_de[], final float Sn[], final float[] mem, final int Nsam)
	{// java: never uses
		for( int i = 0; i < Nsam; i++) {
			final float s = Sn[i] + BETA * mem[0];
			Sn_de[i] = s;
			mem[0] = s;
		}

	}*/

	// java: never uses
	/**

	  hanning_window()

	  Hanning windows a frame of speech samples.

	  @param Sn input frame of speech samples
	  @param Wn output frame of windowed samples
	  @param Nsam number of samples

	*/
	/* private static final void hanning_window( final float Sn[], final float Wn[], int Nsam )
	{
		Nsam--;// java
		final float k = 2f * PI / (float)Nsam;// java
		for( int i = 0; i <= Nsam; i++ ) {
			Wn[i] = Sn[i] * (0.5f - 0.5f * (float)Math.cos( (double)(k * (float)i) ));
		}
	}*/

	/**

	  autocorrelate()

	  Finds the first P autocorrelation values of an array of windowed speech
	  samples Sn[].

	  @param Sn frame of Nsam windowed speech samples
	  @param Rn array of P+1 autocorrelation coefficients
	  @param Nsam number of windowed samples to use
	  @param order order of LPC analysis

	*/
	static final void autocorrelate( final float Sn[], final float Rn[], final int Nsam, final int order )
	{
		for( int j = 0; j <= order; j++ ) {
			float r = 0.0f;
			for( int i = 0, ie = Nsam - j, k = j; i < ie; ) {
				r += Sn[i++] * Sn[k++];
			}
			Rn[j] = r;
		}
	}

	/**

	  levinson_durbin()

	  Given P+1 autocorrelation coefficients, finds P Linear Prediction Coeff.
	  (LPCs) where P is the order of the LPC all-pole model. The Levinson-Durbin
	  algorithm is used, and is described in:

		J. Makhoul
		"Linear prediction, a tutorial review"
		Proceedings of the IEEE
		Vol-63, No. 4, April 1975

		@param R order+1 autocorrelation coeff
		@param lpcs order+1 LPC's
		@param order order of the LPC analysis

	*/
	static final void levinson_durbin( final float R[], final float lpcs[], final int order )
	{
		final float a[][] = new float[order + 1][order + 1];

		float e = R[0];				/* Equation 38a, Makhoul */

		for( int i = 1; i <= order; i++ ) {
			final float[] ai1 = a[i - 1];// java
			float sum = 0.0f;
			for( int j = 1, m = i - 1; j < i; j++ ) {
				sum += ai1[j] * R[m--];
			}
			float k = -1.0f * (R[i] + sum) / e;		/* Equation 38b, Makhoul */
			if( k > 1.0f || k < -1.0f ) {
				k = 0.0f;
			}

			final float[] ai = a[i];// java
			ai[i] = k;

			for( int j = 1, m = i - 1; j < i; j++ ) {
				ai[j] = ai1[j] + k * ai1[m--];
			}	/* Equation 38c, Makhoul */

			e *= (1f - k * k);				/* Equation 38d, Makhoul */
		}

		final float[] a_order = a[order];// java
		for( int i = 1; i <= order; i++ ) {
			lpcs[i] = a_order[i];
		}
		lpcs[0] = 1.0f;
	}

	/**

	  inverse_filter()

	  Inverse Filter, A(z).  Produces an array of residual samples from an array
	  of input samples and linear prediction coefficients.

	  The filter memory is stored in the first order samples of the input array.

	  @param Sn Nsam input samples
	  @param a LPCs for this frame of samples
	  @param Nsam number of samples
	  @param res Nsam residual samples
	  @param order order of LPC

	*/
	static final void inverse_filter(final float Sn[], final int soffset, final float a[], final int Nsam, final float res[], final int order)
	{
		for( int i = 0; i < Nsam; i++ ) {
			float r = 0.0f;
			for( int j = 0, k = soffset + i; j <= order; j++, k-- ) {
				r += Sn[k] * a[j];
			}
			res[i] = r;
		}
	}

	// java: never uses
	/**

	 synthesis_filter()

	 C version of the Speech Synthesis Filter, 1/A(z).  Given an array of
	 residual or excitation samples, and the the LP filter coefficients, this
	 function will produce an array of speech samples.  This filter structure is
	 IIR.

	 The synthesis filter has memory as well, this is treated in the same way
	 as the memory for the inverse filter (see inverse_filter() notes above).
	 The difference is that the memory for the synthesis filter is stored in
	 the output array, wheras the memory of the inverse filter is stored in the
	 input array.

	 Note: the calling function must update the filter memory.

	 @param res Nsam input residual (excitation) samples
	 @param a LPCs for this frame of speech samples
	 @param Nsam number of speech samples
	 @param order LPC order
	 @param Sn_ Nsam output synthesised speech samples

	*/
	/* private static final void synthesis_filter( final float res[], final float a[], final int Nsam, final int order, final float Sn_[])
	{
		// Filter Nsam samples
		final float a0 = a[0];// java
		for( int i = 0; i < Nsam; i++ ) {
			float s = res[i] * a0;
			for( int j = 1, m = i - 1; j <= order; j++ ) {
				s -= Sn_[m--] * a[j];
			}
			Sn_[i] = s;
		}
	}*/

	// java: never uses
	/**

	  find_aks()

	  This function takes a frame of samples, and determines the linear
	  prediction coefficients for that frame of samples.

	  @param Sn Nsam samples with order sample memory
	  @param a order+1 LPCs with first coeff 1.0
	  @param Nsam number of input speech samples
	  @param order order of the LPC analysis
	  @return java: E residual energy

	*/
	/* private static final float find_aks(final float Sn[], final float a[], final int Nsam, final int order)
	{
		final float Wn[] = new float[LPC_MAX_N]; // windowed frame of Nsam speech samples
		final float R[] = new float[order + 1]; // order+1 autocorrelation values of Sn[]

		// assert(Nsam < LPC_MAX_N);

		hanning_window( Sn, Wn, Nsam );
		autocorrelate( Wn, R, Nsam, order );
		levinson_durbin( R, a, order);

		float E = 0.0f;
		for( int i = 0; i <= order; i++ ) {
			E += a[i] * R[i];
		}
		if( E < 0.0f ) {
			E = 1E-12f;
		}
		return E;
	}*/

	// java: never uses
	/**

	  weight()

	  Weights a vector of LPCs.

	  @param ak vector of order+1 LPCs
	  @param gamma weighting factor
	  @param order num LPCs (excluding leading 1.0)
	  @param akw weighted vector of order+1 LPCs

	*/
	/* private static final void weight(final float ak[], final float gamma, final int order, final float akw[])
	{
		for( int i = 1; i <= order; i++ ) {
			akw[i] = ak[i] * (float)Math.pow( (double)gamma, (double)i );
		}
	}*/

}