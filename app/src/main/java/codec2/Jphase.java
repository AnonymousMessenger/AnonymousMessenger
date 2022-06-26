package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: phase.c
  AUTHOR......: David Rowe
  DATE CREATED: 1/2/09

  Functions for modelling and synthesising phase.

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
  along with this program; if not,see <http://www.gnu.org/licenses/>.
*/

// phase.c

final class Jphase {

	/**

	   phase_synth_zero_order()

	   Synthesises phases based on SNR and a rule based approach.  No phase
	   parameters are required apart from the SNR (which can be reduced to a
	   1 bit V/UV decision per frame).

	   The phase of each harmonic is modelled as the phase of a LPC
	   synthesis filter excited by an impulse.  Unlike the first order
	   model the position of the impulse is not transmitted, so we create
	   an excitation pulse train using a rule based approach.

	   Consider a pulse train with a pulse starting time n=0, with pulses
	   repeated at a rate of Wo, the fundamental frequency.  A pulse train
	   in the time domain is equivalent to harmonics in the frequency
	   domain.  We can make an excitation pulse train using a sum of
	   sinsusoids:

	     for(m=1; m<=L; m++)
	       ex[n] = cos(m*Wo*n)

	   Note: the Octave script ../octave/phase.m is an example of this if
	   you would like to try making a pulse train.

	   The phase of each excitation harmonic is:

	     arg(E[m]) = mWo

	   where E[m] are the complex excitation (freq domain) samples,
	   arg(x), just returns the phase of a complex sample x.

	   As we don't transmit the pulse position for this model, we need to
	   synthesise it.  Now the excitation pulses occur at a rate of Wo.
	   This means the phase of the first harmonic advances by N samples
	   over a synthesis frame of N samples.  For example if Wo is pi/20
	   (200 Hz), then over a 10ms frame (N=80 samples), the phase of the
	   first harmonic would advance (pi/20)*80 = 4*pi or two complete
	   cycles.

	   We generate the excitation phase of the fundamental (first
	   harmonic):

	     arg[E[1]] = Wo*N;

	   We then relate the phase of the m-th excitation harmonic to the
	   phase of the fundamental as:

	     arg(E[m]) = m*arg(E[1])

	   This E[m] then gets passed through the LPC synthesis filter to
	   determine the final harmonic phase.

	   Comparing to speech synthesised using original phases:

	   - Through headphones speech synthesised with this model is not as
	     good. Through a loudspeaker it is very close to original phases.

	   - If there are voicing errors, the speech can sound clicky or
	     staticy.  If V speech is mistakenly declared UV, this model tends to
	     synthesise impulses or clicks, as there is usually very little shift or
	     dispersion through the LPC filter.

	   - When combined with LPC amplitude modelling there is an additional
	     drop in quality.  I am not sure why, theory is interformant energy
	     is raised making any phase errors more obvious.

	   NOTES:

	     1/ This synthesis model is effectively the same as a simple LPC-10
	     vocoders, and yet sounds much better.  Why? Conventional wisdom
	     (AMBE, MELP) says mixed voicing is required for high quality
	     speech.

	     2/ I am pretty sure the Lincoln Lab sinusoidal coding guys (like xMBE
	     also from MIT) first described this zero phase model, I need to look
	     up the paper.

	     3/ Note that this approach could cause some discontinuities in
	     the phase at the edge of synthesis frames, as no attempt is made
	     to make sure that the phase tracks are continuous (the excitation
	     phases are continuous, but not the final phases after filtering
	     by the LPC spectra).  Technically this is a bad thing.  However
	     this may actually be a good thing, disturbing the phase tracks a
	     bit.  More research needed, e.g. test a synthesis model that adds
	     a small delta-W to make phase tracks line up for voiced
	     harmonics.

	     @param fft_fwd_cfg
	     @param model
	     @param ex_phase excitation phase of fundamental
	     @param A
	     @return java: new value of the ex_phase

	*/
	static final float phase_synth_zero_order(final Jkiss_fft_state fft_fwd_cfg,
		final JMODEL model, float ex_phase, final Jkiss_fft_cpx A[])
	{
		final float r = Jdefines.TWO_PI / (Jdefines.FFT_ENC);
		final int n = model.L;// java
		/* Sample phase at harmonics */
		/* final Jkiss_fft_cpx H[] = new Jkiss_fft_cpx[Jdefines.MAX_AMP + 1]; // LPC freq domain samples
		for( int m = 1; m <= n; m++ ) {// moved down // FIXME why need array and 2 loops?
			final int b = (int)(m * model.Wo / r + 0.5);
			final double phi_ = -Math.atan2( (double)A[b].i, (double)A[b].r );
			H[m].r = (float)Math.cos( phi_ );
			H[m].i = (float)Math.sin( phi_ );
		}*/

		/*
		   Update excitation fundamental phase track, this sets the position
		   of each pitch pulse during voiced speech.  After much experiment
		   I found that using just this frame's Wo improved quality for UV
		   sounds compared to interpolating two frames Wo like this:

		   ex_phase[0] += (*prev_Wo+model.Wo)*N/2;
		*/

		ex_phase += (model.Wo) * Jdefines.N;
		ex_phase -= Jdefines.TWO_PI * (float)Math.floor( (double)(ex_phase / Jdefines.TWO_PI + 0.5f) );

		// final Jkiss_fft_cpx Ex[] = new Jkiss_fft_cpx[Jdefines.MAX_AMP + 1];	/* excitation samples */// FIXME why need array?
		// final Jkiss_fft_cpx A_[] = new Jkiss_fft_cpx[Jdefines.MAX_AMP + 1]; /* synthesised harmonic samples */// FIXME why need array?
		final float[] model_phi = model.phi;// java
		for( int m = 1; m <= n; m++ ) {
			final int b = (int)(m * model.Wo / r + 0.5);
			final double phi_ = -Math.atan2( (double)A[b].i, (double)A[b].r );
			final float h_m_r = (float)Math.cos( phi_ );
			final float h_m_i = (float)Math.sin( phi_ );

			/* generate excitation */
			float ex_m_r;
			float ex_m_i;
			if( model.voiced ) {

				final double phi = (double)(ex_phase * (float)m);// java
				ex_m_r = (float)Math.cos( phi );
				ex_m_i = (float)Math.sin( phi );
			}
			else {

				/* When a few samples were tested I found that LPC filter
				   phase is not needed in the unvoiced case, but no harm in
				   keeping it.
				*/
				final double phi = (double)(Jdefines.TWO_PI * (float)Jcodec2.codec2_rand() / Jcodec2.CODEC2_RAND_MAX);
				ex_m_r = (float)Math.cos( phi );
				ex_m_i = (float)Math.sin( phi );
			}

			/* filter using LPC filter */

			final float a_m_r = h_m_r * ex_m_r - h_m_i * ex_m_i;
			final float a_m_i = h_m_i * ex_m_r + h_m_r * ex_m_i;

			/* modify sinusoidal phase */

			final float new_phi = (float)Math.atan2( (double)a_m_i, (double)(a_m_r + 1E-12f) );
			model_phi[m] = new_phi;
		}

		return ex_phase;// java
	}

}