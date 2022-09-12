class Wide {
	long foo(long input, int add) {
		long ret;
		long copy = input + add;
		if (add > 0) {
			int value = (int) (copy / add);
			ret = value;
		} else if (add == 0) {
			ret = 0L;
		} else {
			ret = input + add;
		}
		return ret;
	}

	void bar(byte[] array1, byte[] array2) {
		int length = array1.length;
		for (int i = 0; i < length; i += 8) {
			long l = foo((long) array1[i], i);
			l = foo(l, 0);
			foobar(l, array2, i);
		}
	}

	void bar2(byte[] array1, byte[] array2) {
		for (int length = array1.length, i = 0; i < length; i += 8) {
			foobar(foobar(foo((long) array1[i], i), array1, i), array2, i);
		}
	}

	void bar3(byte[] array1, byte[] array2) {
		int length = array1.length;
		long l;
		for (int i = 0; i < length; i += 8) {
			l = foobar(array1, i);
			l = foo(l, i);
			foobar(l, array2, i);
		}
	}

	long foobar(byte[] array, int i) {
		return (((long) array[i]) << 56) |
				(((long) array[i + 1] & 0x0ffL) << 48) |
				(((long) array[i + 2] & 0x0ffL) << 40) |
				(((long) array[i + 3] & 0x0ffL) << 32) |
				(((long) array[i + 4] & 0x0ffL) << 24) |
				(((long) array[i + 5] & 0x0ffL) << 16) |
				(((long) array[i + 6] & 0x0ffL) << 8) |
				((long) array[i + 7] & 0x0ff);
	}

	long foobar(long l, byte[] array, int i) {
		array[i] = (byte)     (l >>> 56);
		array[i + 1] = (byte) (l >>> 48 & 0xFFL);
		array[i + 2] = (byte) (l >>> 40 & 0xFFL);
		array[i + 3] = (byte) (l >>> 32 & 0xFFL);
		array[i + 4] = (byte) (l >>> 24 & 0xFFL);
		array[i + 5] = (byte) (l >>> 16 & 0xFFL);
		array[i + 6] = (byte) (l >>> 8 & 0xFFL);
		array[i + 7] = (byte) l;
		return l;
	}
}