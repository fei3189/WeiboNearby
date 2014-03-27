package info.caq9.util;

public class NumberParser {
	public static long parseLong(String num) {
		long unit = 1;
		if (num.endsWith("万")) {
			num = num.substring(0, num.length() - 1);
			unit = 10000;
		} else if (num.endsWith("亿")) {
			num = num.substring(0, num.length() - 1);
			unit = 100000000;
		}
		return new Double(new Double(num) * unit).longValue();
	}
}
