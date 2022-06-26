package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: lsp.c
  AUTHOR......: David Rowe
  DATE CREATED: 24/2/93


  This file contains functions for LPC to LSP conversion and LSP to
  LPC conversion. Note that the LSP coefficients are not in radians
  format but in the x domain of the unit circle.

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

// lsp.c

final class Jlsp {

/*---------------------------------------------------------------------------*\

  Introduction to Line Spectrum Pairs (LSPs)
  ------------------------------------------

  LSPs are used to encode the LPC filter coefficients {ak} for
  transmission over the channel.  LSPs have several properties (like
  less sensitivity to quantisation noise) that make them superior to
  direct quantisation of {ak}.

  A(z) is a polynomial of order lpcrdr with {ak} as the coefficients.

  A(z) is transformed to P(z) and Q(z) (using a substitution and some
  algebra), to obtain something like:

    A(z) = 0.5[P(z)(z+z^-1) + Q(z)(z-z^-1)]  (1)

  As you can imagine A(z) has complex zeros all over the z-plane. P(z)
  and Q(z) have the very neat property of only having zeros _on_ the
  unit circle.  So to find them we take a test point z=exp(jw) and
  evaluate P (exp(jw)) and Q(exp(jw)) using a grid of points between 0
  and pi.

  The zeros (roots) of P(z) also happen to alternate, which is why we
  swap coefficients as we find roots.  So the process of finding the
  LSP frequencies is basically finding the roots of 5th order
  polynomials.

  The root so P(z) and Q(z) occur in symmetrical pairs at +/-w, hence
  the name Line Spectrum Pairs (LSPs).

  To convert back to ak we just evaluate (1), "clocking" an impulse
  thru it lpcrdr times gives us the impulse response of A(z) which is
  {ak}.

\*---------------------------------------------------------------------------*/

	/**

	  FUNCTION....: cheb_poly_eva()
	  AUTHOR......: David Rowe
	  DATE CREATED: 24/2/93

	  This function evalutes a series of chebyshev polynomials

	  FIXME: performing memory allocation at run time is very inefficient,
	  replace with stack variables of MAX_P size.

	  @param coef coefficients of the polynomial to be evaluated
	  @param x the point where polynomial is to be evaluated
	  @param order order of the polynomial

	*/
	private static final float cheb_poly_eva(final float[] coef, float x, int order)
	{
		order >>= 1;
		final float T[] = new float[order + 1];

		/* Initialise pointers */

		int t = 0;                          	/* T[i-2] 			*/
		T[t] = 1.0f;
		int u = 1;                        	/* T[i-1] 			*/
		T[u] = x;
		int v = 2;                        	/* T[i] 			*/

		/* Evaluate chebyshev series formulation using iterative approach 	*/
		x *= 2f;// java
		while( v <= order ) {
			T[v++] = x * T[u++] - T[t++];  	/* T[i] = 2*x*T[i-1] - T[i-2]	*/
		}

		float sum = 0.0f;                        	/* initialise sum to zero 	*/
		t = 0;                          	/* reset pointer 		*/

		/* Evaluate polynomial and return value also free memory space */

		for( int i = order; t <= order; ) {
			sum += coef[i--] * T[t++];
		}

		return sum;
	}


	/**

	  FUNCTION....: lpc_to_lsp()
	  AUTHOR......: David Rowe
	  DATE CREATED: 24/2/93

	  This function converts LPC coefficients to LSP coefficients.

	  @param a lpc coefficients
	  @param order order of LPC coefficients (10)
	  @param freq LSP frequencies in radians
	  @param nb number of sub-intervals (4)
	  @param delta grid spacing interval (0.02)

	*/
	static final int lpc_to_lsp(final float[] a, final int order, final float[] freq, final int nb, final float delta)
	{
		final float Q[] = new float[order + 1];
		final float P[] = new float[order + 1];

		final int m = order >> 1;            	/* order of P'(z) & Q'(z) polynimials 	*/

		/* Allocate memory space for polynomials */

		/* determine P'(z)'s and Q'(z)'s coefficients where
		P'(z) = P(z)/(1 + z^(-1)) and Q'(z) = Q(z)/(1-z^(-1)) */

		P[0] = 1.0f;
		Q[0] = 1.0f;
		for( int i = 1, j = order; i <= m; i++, j-- ) {
			int i1 = i;
			P[i] = a[i] + a[j] - P[--i1];
			Q[i] = a[i] - a[j] + Q[i1];
		}
		for( int i = 0; i < m; i++ ) {
			P[i] *= 2f;
			Q[i] *= 2f;
		}

		/* Search for a zero in P'(z) polynomial first and then alternate to Q'(z).
		Keep alternating between the two polynomials as each zero is found 	*/

		float xr = 0f;             	/* initialise xr to zero 		*/
		float xl = 1.0f;               	/* start at point xl = 1 		*/

		int roots = 0;              	/* number of roots found 	        */
		float xm = 0;
		float[] pt;/* ptr used for cheb_poly_eval() whether P' or Q' */
		for( int j = 0; j < order; j++ ) {
			if( (j & 1) != 0 ) {
				pt = Q;
			} else {
				pt = P;
			}

			float psuml = cheb_poly_eva( pt, xl, order );	/* evals poly. at xl 	*/
			boolean flag = true;
			while( flag && (xr >= -1.0f) ) {
				xr = xl - delta;                  	/* interval spacing 	*/
				float psumr = cheb_poly_eva( pt, xr, order );/* poly(xl-delta_x) 	*/
				final float temp_psumr = psumr;
				final float temp_xr = xr;

				/* if no sign change increment xr and re-evaluate
				   poly(xr). Repeat til sign change.  if a sign change has
				   occurred the interval is bisected and then checked again
				   for a sign change which determines in which interval the
				   zero lies in.  If there is no sign change between poly(xm)
				   and poly(xl) set interval between xm and xr else set
				   interval between xl and xr and repeat till root is located
				   within the specified limits  */

				if( ((psumr * psuml) < 0.0f) || (psumr == 0.0f) ) {
					roots++;

					float psumm = psuml;
					for( int k = 0; k <= nb; k++ ) {
						xm = (xl + xr) / 2f;        	/* bisect the interval 	*/
						psumm = cheb_poly_eva( pt, xm, order );
						if( psumm * psuml > 0.f ) {
							psuml = psumm;
							xl = xm;
						}
						else {
							psumr = psumm;
							xr = xm;
						}
					}

					/* once zero is found, reset initial interval to xr 	*/
					freq[j] = xm;
					xl = xm;
					flag = false;       		/* reset flag for next search 	*/
				}
				else {
					psuml = temp_psumr;
					xl = temp_xr;
				}
			}
		}

		/* convert from x domain to radians */

		for( int i = 0; i < order; i++ ) {
			freq[i] = (float)Math.acos( (double)freq[i] );
		}

		return roots;
	}

	/**

	  FUNCTION....: lsp_to_lpc()
	  AUTHOR......: David Rowe
	  DATE CREATED: 24/2/93

	  This function converts LSP coefficients to LPC coefficients.  In the
	  Speex code we worked out a way to simplify this significantly.

	  @param lsp array of LSP frequencies in radians
	  @param ak array of LPC coefficients
	  @param order order of LPC coefficients

	*/
	static final void lsp_to_lpc(final float[] lsp, final float[] ak, final int order)
	{
		final float freq[] = new float[order];

		/* convert from radians to the x=cos(w) domain */

		for( int i = 0; i < order; i++ ) {
			freq[i] = (float)Math.cos( (double)lsp[i] );
		}

		// pw = 0;// Wp[pw]

		final int order2 = order >> 1;// java
		/* initialise contents of array */
		final float Wp[] = new float[(order << 2) + 2];
		for( int i = 0, ie = ((order2) << 2) + 1; i <= ie; i++ ) {       	/* set contents of buffer to 0 */
			Wp[i] = 0.0f;
		}

		/* Set pointers up */

		// final int pw = 0;// Wp[pw]
		float xin1 = 1.0f;
		float xin2 = 1.0f;

		/* reconstruct P(z) and Q(z) by cascading second order polynomials
		  in form 1 - 2xz(-1) +z(-2), where x is the LSP coefficient */
		int n4 = Integer.MIN_VALUE;// java variant for null
		for( int j = 0; j <= order; j++ ) {
			for( int i = 0; i < order2; i++ ) {
				final int n1 = (i << 2);
				final int n2 = n1 + 1;
				final int n3 = n2 + 1;
				n4 = n3 + 1;
				int i2 = i << 1;// java
				final float xout1 = xin1 - 2f * (freq[i2++]) * Wp[n1] + Wp[n2];
				final float xout2 = xin2 - 2f * (freq[i2  ]) * Wp[n3] + Wp[n4];
				Wp[n2] = Wp[n1];
				Wp[n4] = Wp[n3];
				Wp[n1] = xin1;
				Wp[n3] = xin2;
				xin1 = xout1;
				xin2 = xout2;
			}
			final float xout1 = xin1 + Wp[++n4];// + 1
			final float xout2 = xin2 - Wp[++n4];// + 2
			ak[j] = (xout1 + xout2) * 0.5f;
			Wp[n4--] = xin2;// + 2
			Wp[n4] = xin1;// + 1

			xin1 = 0.0f;
			xin2 = 0.0f;
		}
	}

}