package codec2;

/*---------------------------------------------------------------------------*\

  FILE........: defines.h
  AUTHOR......: David Rowe
  DATE CREATED: 23/4/93

  Defines and structures used throughout the codec.

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

// defines.h

final class Jdefines {

/*---------------------------------------------------------------------------*\

				DEFINES

\*---------------------------------------------------------------------------*/

	/* General defines */

	/** number of samples per frame */
	static final int N           = 80;
	/** maximum number of harmonics */
	static final int MAX_AMP     = 80;
	/** mathematical constant */
	static final float PI        = 3.141592654f;
	/** mathematical constant */
	static final float TWO_PI    = 6.283185307f;
	/** sample rate in Hz */
	static final int FS          = 8000;
	/** maximum string size */
	static final int MAX_STR     = 256;

	/** analysis window size */
	static final int NW          = 279;
	/** size of FFT used for encoder */
	static final int FFT_ENC     = 512;
	/** size of FFT used in decoder */
	static final int FFT_DEC     = 512;
	/** Trapezoidal synthesis window overlap */
	static final int TW          = 40;
	/** voicing threshold in dB */
	static final float V_THRESH  = 6.0f;
	/** LPC order */
	static final int LPC_ORD     = 10;
	/** LPC order for lower rates */
	static final int LPC_ORD_LOW = 6;

	/* Pitch estimation defines */

	/** pitch analysis frame size */
	static final int M     = 320;
	/** minimum pitch */
	static final int P_MIN = 20;
	/** maximum pitch */
	static final int P_MAX = 160;

}
