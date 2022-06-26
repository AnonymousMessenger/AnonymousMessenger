package codec2;

/*
Copyright (c) 2003-2010, Mark Borgerding

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the author nor the names of any contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

final class Jkiss_fft_state {
	/*
	  Explanation of macros dealing with complex math:

	   C_MUL(m,a,b)         : m = a*b
	   C_FIXDIV( c , div )  : if a fixed point impl., c /= div. noop otherwise
	   C_SUB( res, a,b)     : res = a - b
	   C_SUBFROM( res , a)  : res -= a
	   C_ADDTO( res , a)    : res += a
	 */
	// private static final int SAMP_MAX = 32767;
	// private static final int SAMP_MIN = -SAMP_MAX;

	/* private static final void C_MUL(final Jkiss_fft_cpx m, final Jkiss_fft_cpx a, final Jkiss_fft_cpx b) {
		m.r = a.r * b.r - a.i * b.i;
		m.i = a.r * b.i + a.i * b.r;
	}*/
	/* private static final void C_MULBYSCALAR(final Jkiss_fft_cpx c, final float s) {
		c.r *= s;
		c.i *= s;
	}*/
	/* private static final void C_ADD(final Jkiss_fft_cpx res, final Jkiss_fft_cpx a, final Jkiss_fft_cpx b) {
		res.r = a.r + b.r; res.i = a.i + b.i;
    }*/
    /* private static final void C_SUB(final Jkiss_fft_cpx res, final Jkiss_fft_cpx a, final Jkiss_fft_cpx b) {
    	res.r = a.r - b.r; res.i = a.i - b.i;
    }*/
    /* private static final void C_ADDTO(final Jkiss_fft_cpx res, final Jkiss_fft_cpx a) {
		res.r += a.r; res.i += a.i;
    }*/
	/** e.g. an fft of length 128 has 4 factors
	 as far as kissfft is concerned
	 4*4*4*2
	 */
	private static final int MAXFACTORS = 32;
	//
	private int nfft;
	private boolean inverse;
	private final int factors[] = new int[2 * MAXFACTORS];
	private final Jkiss_fft_cpx twiddles[];
	//
	private Jkiss_fft_state(final int n) {
		twiddles = new Jkiss_fft_cpx[n];
		for( int i = 0 ; i < n; i++ ) {
			twiddles[i] = new Jkiss_fft_cpx();
		}
	}
	private final void kf_bfly2(final Jkiss_fft_cpx[] Fout, int outoffset, final int fstride, final int m)
	{
		final Jkiss_fft_cpx[] tw = this.twiddles;
		int tw1 = 0;
		final Jkiss_fft_cpx t = new Jkiss_fft_cpx();
		final int end = outoffset + m;// java
		int Fout2 = end;
		do {
			// C_FIXDIV( Fout[Fout0], 2 );
			// C_FIXDIV( Fout[Fout2], 2 );

			final Jkiss_fft_cpx tmp = Fout[outoffset];
			final Jkiss_fft_cpx tmp2 = Fout[Fout2];
			final Jkiss_fft_cpx t1 = tw[tw1];

			t.r = tmp2.r * t1.r - tmp2.i * t1.i;// C_MUL( t, Fout[Fout2], tw[tw1] );
			t.i = tmp2.r * t1.i + tmp2.i * t1.r;
			tw1 += fstride;
			tmp2.r = tmp.r - t.r; tmp2.i = tmp.i - t.i;// C_SUB( Fout[Fout2], Fout[outoffset], t );
			tmp.r += t.r; tmp.i += t.i;// C_ADDTO( Fout[outoffset], t );
			++Fout2;
		} while( ++outoffset < end );
	}

	private final void kf_bfly4(final Jkiss_fft_cpx[] Fout, int outoffset, final int fstride, int m)
	{
		final Jkiss_fft_cpx[] tw = this.twiddles;
		final Jkiss_fft_cpx scratch0 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch1 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch2 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch3 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch4 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch5 = new Jkiss_fft_cpx();

		final int k = outoffset + m;
		int m2 = (m << 1);
		int m3 = outoffset + m2 + m;
		m2 += outoffset;
		m = k;

		int tw1, tw2, tw3;
		tw3 = tw2 = tw1 = 0;

		final int fstride2 = fstride << 1;
		final int fstride3 = fstride2 + fstride;

		do {
			final Jkiss_fft_cpx tmp1 = Fout[m];
			final Jkiss_fft_cpx tmp2 = Fout[m2];
			final Jkiss_fft_cpx tmp3 = Fout[m3];
			Jkiss_fft_cpx t = tw[tw1];
			scratch0.r = tmp1.r * t.r - tmp1.i * t.i;
			scratch0.i = tmp1.r * t.i + tmp1.i * t.r;// C_MUL( scratch0, Fout[m], tw[tw1] );
			t = tw[tw2];
			scratch1.r = tmp2.r * t.r - tmp2.i * t.i;
			scratch1.i = tmp2.r * t.i + tmp2.i * t.r;// C_MUL( scratch1, Fout[m2], tw[tw2] );
			t = tw[tw3];
			scratch2.r = tmp3.r * t.r - tmp3.i * t.i;
			scratch2.i = tmp3.r * t.i + tmp3.i * t.r;// C_MUL( scratch2, Fout[m3], tw[tw3] );

			final Jkiss_fft_cpx tmp = Fout[outoffset];
			scratch5.r = tmp.r - scratch1.r; scratch5.i = tmp.i - scratch1.i;// C_SUB( scratch5, Fout[outoffset], scratch1 );
			tmp.r += scratch1.r; tmp.i += scratch1.i;// C_ADDTO( Fout[outoffset], scratch1 );
			scratch3.r = scratch0.r + scratch2.r; scratch3.i = scratch0.i + scratch2.i;// C_ADD( scratch3, scratch0, scratch2 );
			scratch4.r = scratch0.r - scratch2.r; scratch4.i = scratch0.i - scratch2.i;// C_SUB( scratch4, scratch0, scratch2 );
			tmp2.r = tmp.r - scratch3.r; tmp2.i = tmp.i - scratch3.i;// C_SUB( Fout[m2], Fout[outoffset], scratch3 );
			tw1 += fstride;
			tw2 += fstride2;
			tw3 += fstride3;
			tmp.r += scratch3.r; tmp.i += scratch3.i;// C_ADDTO( Fout[outoffset], scratch3 );

			if( this.inverse ) {
				tmp1.r = scratch5.r - scratch4.i;
				tmp1.i = scratch5.i + scratch4.r;
				tmp3.r = scratch5.r + scratch4.i;
				tmp3.i = scratch5.i - scratch4.r;
			} else {
				tmp1.r = scratch5.r + scratch4.i;
				tmp1.i = scratch5.i - scratch4.r;
				tmp3.r = scratch5.r - scratch4.i;
				tmp3.i = scratch5.i + scratch4.r;
			}
			m++;
			m2++;
			m3++;
		} while( ++outoffset < k );
	}

	private final void kf_bfly3(final Jkiss_fft_cpx[] Fout, int outoffset, final int fstride, int m)
	{
		final Jkiss_fft_cpx[] tw = this.twiddles;
		final int k = outoffset + m;
		int m2 = m << 1;
		m2 += outoffset;
		m = k;

		final Jkiss_fft_cpx scratch0 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch1 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch2 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch3 = new Jkiss_fft_cpx();

		final float epi3_i = tw[fstride * m].i;

		int tw1 = 0, tw2 = 0;
		final int fstride2 = fstride << 1;

		do {
			final Jkiss_fft_cpx tmp1 = Fout[m];
			final Jkiss_fft_cpx tmp2 = Fout[m2];
			Jkiss_fft_cpx t = tw[tw1];
			scratch1.r = tmp1.r * t.r - tmp1.i * t.i;
			scratch1.i = tmp1.r * t.i + tmp1.i * t.r;// C_MUL( scratch1, Fout[m], tw[tw1] );
			t = tw[tw2];
			scratch2.r = tmp2.r * t.r - tmp2.i * t.i;
			scratch2.i = tmp2.r * t.i + tmp2.i * t.r;// C_MUL( scratch2, Fout[m2], tw[tw2] );

			scratch3.r = scratch1.r + scratch2.r; scratch3.i = scratch1.i + scratch2.i;// C_ADD( scratch3, scratch1, scratch2 );
			scratch0.r = scratch1.r - scratch2.r; scratch0.i = scratch1.i - scratch2.i;// C_SUB( scratch0, scratch1, scratch2 );
			tw1 += fstride;
			tw2 += fstride2;

			final Jkiss_fft_cpx tmp = Fout[outoffset];
			tmp1.r = tmp.r - 0.5f * scratch3.r;
			tmp1.i = tmp.i - 0.5f * scratch3.i;

			scratch0.r *= epi3_i; scratch0.i *= epi3_i;// C_MULBYSCALAR( scratch0, epi3.i );

			tmp.r += scratch3.r; tmp.i += scratch3.i;// C_ADDTO( Fout[outoffset], scratch3 );

			tmp2.r = tmp1.r + scratch0.i;
			tmp2.i = tmp1.i - scratch0.r;

			tmp1.r -= scratch0.i;
			tmp1.i += scratch0.r;

			m++;
			m2++;
		} while( ++outoffset < k );
	}

	private final void kf_bfly5(final Jkiss_fft_cpx[] Fout, int outoffset, final int fstride, int m)
	{
		final Jkiss_fft_cpx scratch0 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch1 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch2 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch3 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch4 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch5 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch6 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch7 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch8 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch9 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch10 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch11 = new Jkiss_fft_cpx();
		final Jkiss_fft_cpx scratch12 = new Jkiss_fft_cpx();

		// int Fout0 = 0;
		int Fout1 = outoffset + m;
		int Fout2 = Fout1 + m;
		int Fout3 = Fout2 + m;
		int Fout4 = Fout3 + m;

		m *= fstride;

		final Jkiss_fft_cpx[] tw = this.twiddles;
		final Jkiss_fft_cpx ya = tw[m];
		final Jkiss_fft_cpx yb = tw[m << 1];

		for( int u = 0; u < m; u += fstride ) {
			scratch0.copyFrom( Fout[outoffset] );

			final Jkiss_fft_cpx tmp1 = Fout[Fout1];
			final Jkiss_fft_cpx tmp2 = Fout[Fout2];
			final Jkiss_fft_cpx tmp3 = Fout[Fout3];
			final Jkiss_fft_cpx tmp4 = Fout[Fout4];

			Jkiss_fft_cpx t = tw[u];
			scratch1.r = tmp1.r * t.r - tmp1.i * t.i;
			scratch1.i = tmp1.r * t.i + tmp1.i * t.r;// C_MUL( scratch1, Fout[Fout1], tw[u] );
			int u2 = u << 1;
			t = tw[u2];
			scratch2.r = tmp2.r * t.r - tmp2.i * t.i;
			scratch2.i = tmp2.r * t.i + tmp2.i * t.r;// C_MUL( scratch2, Fout[Fout2], tw[u2] );
			u2 += u;
			t = tw[u2];
			scratch3.r = tmp3.r * t.r - tmp3.i * t.i;
			scratch3.i = tmp3.r * t.i + tmp3.i * t.r;// C_MUL( scratch3, Fout[Fout3], tw[u2] );
			u2 += u;
			t = tw[u2];
			scratch4.r = tmp4.r * t.r - tmp4.i * t.i;
			scratch4.i = tmp4.r * t.i + tmp4.i * t.r;// C_MUL( scratch4, Fout[Fout4], tw[u2] );

			scratch7.r = scratch1.r + scratch4.r; scratch7.i = scratch1.i + scratch4.i;// C_ADD( scratch7, scratch1, scratch4 );
			scratch10.r = scratch1.r - scratch4.r; scratch10.i = scratch1.i - scratch4.i;// C_SUB( scratch10, scratch1, scratch4 );
			scratch8.r = scratch2.r + scratch3.r; scratch8.i = scratch2.i + scratch3.i;// C_ADD( scratch8, scratch2, scratch3 );
			scratch9.r = scratch2.r - scratch3.r; scratch9.i = scratch2.i - scratch3.i;// C_SUB( scratch9, scratch2, scratch3 );

			Fout[outoffset].r += scratch7.r + scratch8.r;
			Fout[outoffset].i += scratch7.i + scratch8.i;

			scratch5.r = scratch0.r + ( scratch7.r * ya.r ) + ( scratch8.r * yb.r );
			scratch5.i = scratch0.i + ( scratch7.i * ya.r ) + ( scratch8.i * yb.r );

			scratch6.r =  ( scratch10.i * ya.i ) + ( scratch9.i * yb.i );
			scratch6.i = -( scratch10.r * ya.i ) - ( scratch9.r * yb.i );

			tmp1.r = scratch5.r - scratch6.r; tmp1.i = scratch5.i - scratch6.i;// C_SUB( Fout[Fout1], scratch5, scratch6 );

			tmp4.r = scratch5.r + scratch6.r; tmp4.i = scratch5.i + scratch6.i;// C_ADD( Fout[Fout4], scratch5, scratch6 );

			scratch11.r = scratch0.r + ( scratch7.r * yb.r ) + ( scratch8.r * ya.r );
			scratch11.i = scratch0.i + ( scratch7.i * yb.r ) + ( scratch8.i * ya.r );
			scratch12.r = - ( scratch10.i * yb.i ) + ( scratch9.i * ya.i );
			scratch12.i = ( scratch10.r * yb.i ) - ( scratch9.r * ya.i );

			tmp2.r = scratch11.r + scratch12.r; tmp2.i = scratch11.i + scratch12.i;// C_ADD( Fout[Fout2], scratch11, scratch12 );
			tmp3.r = scratch11.r - scratch12.r; tmp3.i = scratch11.i - scratch12.i;// C_SUB( Fout[Fout3], scratch11, scratch12 );

			++outoffset; ++Fout1; ++Fout2; ++Fout3; ++Fout4;
		}
	}

	/** perform the butterfly for one stage of a mixed radix FFT */
	private final void kf_bfly_generic(final Jkiss_fft_cpx[] Fout, final int outoffset, final int fstride, final int m, final int p)
	{
		final Jkiss_fft_cpx[] tw = this.twiddles;
		final Jkiss_fft_cpx t = new Jkiss_fft_cpx();
		final int Norig = this.nfft;

		Jkiss_fft_cpx[] scratch = new Jkiss_fft_cpx[ p ];

		for( int u = 0; u < m; ++u ) {
			int k = u + outoffset;
			for( int q1 = 0; q1 < p; ++q1 ) {
				scratch[q1].copyFrom( Fout[ k ] );
				k += m;
			}

			k = u;
			int i = outoffset + k;
			for( int q1 = 0; q1 < p; ++q1 ) {
				int twidx = 0;
				Fout[ i ].copyFrom( scratch[0] );
				for( int q = 1; q < p; ++q ) {
					twidx += fstride * k;
					if( twidx >= Norig ) {
						twidx -= Norig;
					}
					final Jkiss_fft_cpx s = scratch[q];
					final Jkiss_fft_cpx twi = tw[ twidx ];
					t.r = s.r * twi.r - s.i * twi.i;
					t.i = s.r * twi.i + s.i * twi.r;// C_MUL( t, scratch[q], tw[ twidx ] );
					final Jkiss_fft_cpx res = Fout[ i ];
					res.r += t.r; res.i += t.i;// C_ADDTO( Fout[ i ], t );
				}
				k += m;
				i += m;
			}
		}
		scratch = null;
	}

	private final void kf_work(final Jkiss_fft_cpx[] Fout, int outoffset, final Jkiss_fft_cpx[] f, int foffset, final int fstride, final int in_stride, final int[] mults, int offset )
	{
		final int Fout_beg = outoffset;
		final int p = mults[offset++]; /* the radix  */
		final int m = mults[offset++]; /* stage's fft length/p */
		final int Fout_end = outoffset + p * m;

		if( m == 1 ) {
			do {
				Fout[outoffset].copyFrom( f[foffset] );
				foffset += fstride * in_stride;
			} while( ++outoffset != Fout_end );
		} else {
			do {
				// recursive call:
				// DFT of size m*p performed by doing
				// p instances of smaller DFTs of size m,
				// each one takes a decimated version of the input
				kf_work( Fout, outoffset, f, foffset, fstride * p, in_stride, mults, offset );
				foffset += fstride * in_stride;
			} while( (outoffset += m) != Fout_end );
		}

		// recombine the p smaller DFTs
		switch( p ) {
		case 2: kf_bfly2( Fout, Fout_beg, fstride, m ); break;
		case 3: kf_bfly3( Fout, Fout_beg, fstride, m ); break;
		case 4: kf_bfly4( Fout, Fout_beg, fstride, m ); break;
		case 5: kf_bfly5( Fout, Fout_beg, fstride, m ); break;
		default: kf_bfly_generic( Fout, Fout_beg, fstride, m, p ); break;
		}
	}

	private static final void kf_cexp( final Jkiss_fft_cpx x, final double phase)
	{
		x.r = (float)Math.cos( phase );
		x.i = (float)Math.sin( phase );
	}
	/** facbuf is populated by p1,m1,p2,m2, ...
		where
		p[i] * m[i] = m[i-1]
		m0 = n                  */
	private static final void kf_factor(int n, final int[] facbuf)
	{
		int offset = 0;// java
		int p = 4;
		final double floor_sqrt = Math.floor( Math.sqrt( (double)n ) );

		/*factor out powers of 4, powers of 2, then any remaining primes */
		do {
			while( (n % p) != 0 ) {
				switch( p ) {
				case 4: p = 2; break;
				case 2: p = 3; break;
				default: p += 2; break;
				}
				if( p > floor_sqrt ) {
					p = n;
				}          /* no more factors, skip to end */
			}
			n /= p;
			facbuf[offset++] = p;
			facbuf[offset++] = n;
		} while( n > 1 );
	}
	/**
	 *
	 * User-callable function to allocate all necessary storage space for the fft.
	 *
	 * The return value is a contiguous block of memory, allocated with malloc.  As such,
	 * It can be freed with free(), rather than a kiss_fft-specific function.
	 */
	static final Jkiss_fft_state kiss_fft_alloc(final int nfft, final boolean inverse_fft)// java: never uses , void * mem, size_t * lenmem )
	{
		Jkiss_fft_state st = null;
		// if( lenmem == NULL ) {
			st = new Jkiss_fft_state( nfft );
		/* } else {
			if (mem != NULL && *lenmem >= memneeded) {
				st = (kiss_fft_cfg)mem;
			}
			*lenmem = memneeded;
		}*/
		if( st != null ) {
			st.nfft = nfft;
			st.inverse = inverse_fft;

			for( int i = 0; i < nfft; ++i ) {
				final double pi = 3.141592653589793238462643383279502884197169399375105820974944;
				double phase = -2. * pi * i / nfft;
				if( st.inverse ) {
					phase *= -1;
				}
				kf_cexp( st.twiddles[i], phase );
			}

			kf_factor( nfft, st.factors );
		}
		return st;
	}

	private final void kiss_fft_stride(final Jkiss_fft_cpx[] fin, final Jkiss_fft_cpx[] fout, final int in_stride)
	{
		if( fin == fout ) {
			//NOTE: this is not really an in-place FFT algorithm.
			//It just performs an out-of-place FFT into a temp buffer
			Jkiss_fft_cpx[] tmpbuf = new Jkiss_fft_cpx[ this.nfft ];
			kf_work( tmpbuf, 0, fin, 0, 1, in_stride, this.factors, 0 );
			System.arraycopy( tmpbuf, 0, fout, 0, this.nfft );
			tmpbuf = null;
		} else {
			kf_work( fout, 0, fin, 0, 1, in_stride, this.factors, 0 );
		}
	}

	final void kiss_fft(final Jkiss_fft_cpx[] fin, final Jkiss_fft_cpx[] fout)
	{
		kiss_fft_stride( fin, fout, 1 );
	}


	/* private static final void kiss_fft_cleanup()
	{
		// nothing needed any more
	}*/

	/* private static final int kiss_fft_next_fast_size(int n)// java: never uses
	{
		while( true ) {
			int m = n;
			while( (m & 1) == 0 ) {// m % 2
				m >>= 1;
			}
			while( (m % 3) == 0 ) {
				m /= 3;
			}
			while( (m % 5) == 0 ) {
				m /= 5;
			}
			if( m <= 1 ) {
				break; // n is completely factorable by twos, threes, and fives
			}
			n++;
		}
		return n;
	}*/
}
