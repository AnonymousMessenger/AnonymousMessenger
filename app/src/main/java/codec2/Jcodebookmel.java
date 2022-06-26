package codec2;

/* THIS IS A GENERATED FILE. Edit generate_codebook.c and its input */

/*
 * This intermediary file and the files that used to create it are under
 * The LGPL. See the file COPYING.
 */

// codebookmel.c

final class Jcodebookmel {

	/* codebook/mel1.txt */
	private static final float codes0[] = {
			550,
			600,
			650,
			700,
			750,
			800,
			850,
			900
		};
	/* codebook/mel2.txt */
	private static final float codes1[] = {
			50,
			100,
			200,
			300
		};
	/* codebook/mel3.txt */
	private static final float codes2[] = {
			800,
			850,
			900,
			950,
			1000,
			1050,
			1100,
			1150,
			1200,
			1250,
			1300,
			1350,
			1400,
			1450,
			1500,
			1650
		};
	/*codebook/mel4.txt */
	private static final float codes3[] = {
			25,
			50,
			75,
			100,
			125,
			150,
			175,
			250
		};
	/* codebook/mel5.txt */
	private static final float codes4[] = {
			1350,
			1400,
			1450,
			1500,
			1550,
			1600,
			1650,
			1700
		};
	/* codebook/mel6.txt */
	private static final float codes5[] = {
			25,
			50,
			100,
			150
		};

	static final Jlsp_codebook mel_cb[] = {
		/* codebook/mel1.txt */
		new Jlsp_codebook( 1, 3,/* 8,*/ codes0 ),
		/* codebook/mel2.txt */
		new Jlsp_codebook( 1, 2,/* 4,*/ codes1 ),
		/* codebook/mel3.txt */
		new Jlsp_codebook( 1, 4,/* 16,*/ codes2 ),
		/* codebook/mel4.txt */
		new Jlsp_codebook( 1, 3,/* 8,*/ codes3 ),
		/* codebook/mel5.txt */
		new Jlsp_codebook( 1, 3,/* 8,*/ codes4 ),
		/* codebook/mel6.txt */
		new Jlsp_codebook( 1, 2,/* 4,*/ codes5 ),
		// new Jlsp_codebook( 0, 0, 0, null )
	};
}