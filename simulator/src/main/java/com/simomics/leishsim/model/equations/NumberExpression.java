package com.simomics.leishsim.model.equations;

import com.simomics.leishsim.simulation.Membrane;

/**
 * A constant number in an equation.
 */
public class NumberExpression implements Equation {

	private double value;

	public NumberExpression(double value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		// Don't print any trailing zeros, and don't print the decimal point unless we have to
		if(value == (long) value) {
			return String.format("(%d)", (long)value);
		}
		else {
			return String.format("(%s)", value);
		}
	}

	@Override
	public double evaluate(Membrane membrane) {
		return value;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		NumberExpression other = (NumberExpression) obj;
		return value == other.value;
	}
}
