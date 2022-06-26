package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: interp.c
  AUTHOR......: David Rowe
  DATE CREATED: 9/10/09

  Interpolation of 20ms frames to 10ms frames.

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

// interp.c

final class Jinterp {

	/**

	  FUNCTION....: interp()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22/8/10

	  Given two frames decribed by model parameters 20ms apart, determines
	  the model parameters of the 10ms frame between them.  Assumes
	  voicing is available for middle (interpolated) frame.  Outputs are
	  amplitudes and Wo for the interpolated frame.

	  This version can interpolate the amplitudes between two frames of
	  different Wo and L.

	  This version works by log linear interpolation, but listening tests
	  showed it creates problems in background noise, e.g. hts2a and mmt1.
	  When this function is used (--dec mode) bg noise appears to be
	  amplitude modulated, and gets louder.  The interp_lsp() function
	  below seems to do a better job.

	  @param interp interpolated model params
	  @param prev previous frames model params
	  @param next next frames model params
	*/
	/* private static final void interpolate( final JMODEL interp, final JMODEL prev, final JMODEL next )
	{// java: never uses
		// Wo depends on voicing of this and adjacent frames

		if( interp.voiced ) {
			if( prev.voiced && next.voiced ) {
				interp.Wo = (prev.Wo + next.Wo) / 2.0f;
			}
			if( ! prev.voiced && next.voiced ) {
				interp.Wo = next.Wo;
			}
			if( prev.voiced && ! next.voiced ) {
				interp.Wo = prev.Wo;
			}
		}
		else {
			interp.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;
		}
		interp.L = (int)(Jdefines.PI / interp.Wo);

		// Interpolate amplitudes using linear interpolation in log domain

		final float[] a = interp.A;// java
		for( int l = 1; l <= interp.L; l++ ) {
			final float w = l * interp.Wo;
			final float log_amp = (prev.sample_log_amp( w ) + next.sample_log_amp( w )) / 2.0f;
			a[l] = (float)Math.pow( 10.0, (double)log_amp );
		}
	}*/

	/**

	  FUNCTION....: interp_Wo()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22 May 2012

	  Interpolates centre 10ms sample of Wo and L samples given two
	  samples 20ms apart. Assumes voicing is available for centre
	  (interpolated) frame.

	  @param interp interpolated model params
	  @param prev previous frames model params
	  @param next next frames model params
	*/
	static final void interp_Wo(final JMODEL interp, final JMODEL prev, final JMODEL next)
	{
		interp_Wo2( interp, prev, next, 0.5f );
	}

	/**

	  FUNCTION....: interp_Wo2()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22 May 2012

	  Weighted interpolation of two Wo samples.

	  @param interp interpolated model params
	  @param prev previous frames model params
	  @param next next frames model params
	*/
	static final void interp_Wo2( final JMODEL interp, final JMODEL prev, final JMODEL next, final float weight )
	{
		/* trap corner case where voicing est is probably wrong */

		if( interp.voiced && ! prev.voiced && ! next.voiced ) {
			interp.voiced = false;
		}

		/* Wo depends on voicing of this and adjacent frames */

		if( interp.voiced ) {
			if( prev.voiced && next.voiced ) {
				interp.Wo = (1.0f - weight) * prev.Wo + weight * next.Wo;
			}
			if( ! prev.voiced && next.voiced ) {
				interp.Wo = next.Wo;
			}
			if( prev.voiced && ! next.voiced ) {
				interp.Wo = prev.Wo;
			}
		}
		else {
			interp.Wo = Jdefines.TWO_PI / Jdefines.P_MAX;
		}
		interp.L = (int)(Jdefines.PI / interp.Wo);
	}

	/**

	  FUNCTION....: interp_energy()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22 May 2012

	  Interpolates centre 10ms sample of energy given two samples 20ms
	  apart.

	*/
	static final float interp_energy(final float prev_e, final float next_e)
	{
		return (float)Math.pow( 10.0, (Math.log10( prev_e ) + Math.log10( next_e )) / 2.0 );
	}


	/**

	  FUNCTION....: interp_energy2()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22 May 2012

	  Interpolates centre 10ms sample of energy given two samples 20ms
	  apart.

	*/
	static final float interp_energy2(final float prev_e, final float next_e, final float weight)
	{
		return (float)(Math.pow( 10.0, (1.0 - (double)weight) * Math.log10( (double)prev_e ) + (double)weight * Math.log10( (double)next_e ) ));
	}


	/**

	  FUNCTION....: interpolate_lsp_ver2()
	  AUTHOR......: David Rowe
	  DATE CREATED: 22 May 2012

	  Weighted interpolation of LSPs.

	*/
	static final void interpolate_lsp_ver2(final float interp[], final float prev[],  final float next[], final float weight, final int order)
	{
		final float weight1 = 1.0f - weight;// java
		for( int i = 0; i < order; i++ ) {
			interp[i] = weight1 * prev[i] + weight * next[i];
		}
	}

}