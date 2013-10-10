package edu.penn.rtg.schedulingapp;

import java.util.Iterator;
import java.util.Vector;

import edu.penn.rtg.common.GlobalVariable;
/**
 * Compute Least Common Multiplier and Greatest Common Divider.
 * <P>
 * @author      
 * @version     1.0
 * @since       1.0
 */
public final class LCM {

	/**
	 * Compute Least Common Multiplier
	 * @param  numbers vector of numbers
	 * @return      Least Common Multiplier
	 * @see         none
	 */
	public static double generateLCM(Vector numbers) {

		double lcm = 0, gcd = 0;
		double number1 = 0, number2 = 0;
		Iterator iter = numbers.iterator();
		if (iter.hasNext()) {
			number1 = ((Double) iter.next()).doubleValue();
			lcm = number1;
		}
		while (iter.hasNext()) {
			number2 = ((Double) iter.next()).doubleValue();
			System.out.println("lcm=" + lcm  + " number=" + number2);
			gcd = generateGCD(number1, number2);
			if(gcd < 0 || lcm < 0 || lcm > GlobalVariable.MAX_NUMBER){ //lcm is too large to compute, so just return the max value and mark it unschedulable
				System.err.println("lcm gets overflow or larger than MAX_NUMBER; Not computable, mark the taskset as unscheduable. lcm is:" + "gcd="+gcd + "\t lcm=" + lcm);
				return GlobalVariable.MAX_NUMBER;
			}
			lcm = number1 * (number2 / gcd);
			number1 = lcm;
		}
		return lcm;
	}

	/**
	 * Compute Greatest Common Divider between two number
	 * @param  number1 firtst number
	 * @param  number2 second number
	 * @return      Greatest Common Divider
	 * @see         none
	 */
	public static double generateGCD(double number1, double number2) {

		if (number1 >= number2) {
			if (number2 == 0)
				return number1;
			else {
				double remainder = number1 - Math.floor(number1 / number2)
						* number2;
				return generateGCD(number2, remainder);
			}
		} else {
			if (number1 == 0)
				return number2;
			else {
				double remainder = number2 - Math.floor(number2 / number1)
						* number1;
				return generateGCD(number1, remainder);
			}
		}
	}
}
