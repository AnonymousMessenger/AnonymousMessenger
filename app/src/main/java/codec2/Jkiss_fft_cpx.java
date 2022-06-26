package codec2;


final class Jkiss_fft_cpx {
	float r;
	float i;
	//
	Jkiss_fft_cpx() {
		r = 0f;
		i = 0f;
	}
	Jkiss_fft_cpx(final float real, final float image) {
		r = real;
		i = image;
	}
	/**
	 * Set values
	 * @param cpx source
	 */
	final void copyFrom(final Jkiss_fft_cpx cpx) {
		r = cpx.r;
		i = cpx.i;
	}
}
