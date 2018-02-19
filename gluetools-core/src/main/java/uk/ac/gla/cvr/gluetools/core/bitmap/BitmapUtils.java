/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.bitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BitmapUtils {
	public static byte intsToByteBitmap(int[] ints) {
		byte bitmap = 0;
		for(int integer : ints) {
			bitmap |= 1 << integer;
		}
		return bitmap;
	}
	
	public static List<Integer> byteBitmapToInts(byte bitmap) {
		List<Integer> results = new ArrayList<Integer>();
		for(int i = 0; i < 8; i++) {
			byte mask = (byte) (1 << i);
			if((bitmap & mask) != 0) {
				results.add(i);
			}
		}
		return results;
	}

	public static int intsToIntBitmap(int[] ints) {
		int bitmap = 0;
		for(int integer : ints) {
			bitmap |= 1 << integer;
		}
		return bitmap;
	}

	public static List<Integer> intBitmapToInts(int bitmap) {
		List<Integer> results = new ArrayList<Integer>();
		for(int i = 0; i < 32; i++) {
			int mask = (int) (1 << i);
			if((bitmap & mask) != 0) {
				results.add(i);
			}
		}
		return results;
	}

	public static long intsToLongBitmap(int[] ints) {
		long bitmap = 0L;
		for(int integer : ints) {
			long mask = 1L << integer;
			bitmap |= mask;
		}
		return bitmap;
	}

	public static List<Integer> longBitmapToInts(long bitmap) {
		List<Integer> results = new ArrayList<Integer>();
		for(int i = 0; i < 64; i++) {
			long mask = 1L << i;
			if((bitmap & mask) != 0L) {
				results.add((int) i);
			}
		}
		return results;
	}


	public static void main(String[] args) {
		byte byteBM1 = intsToByteBitmap(new int[]{1,6,7,2,3});
		System.out.println(byteBitmapToInts(byteBM1));
		int intBM1 = intsToIntBitmap(new int[]{1,6,7,24,9,8,2,3});
		System.out.println(intBitmapToInts(intBM1));
		long longBM1 = intsToLongBitmap(new int[]{1,4,62,63,2,9,32});
		System.out.println(longBitmapToInts(longBM1));
	}
	

}
