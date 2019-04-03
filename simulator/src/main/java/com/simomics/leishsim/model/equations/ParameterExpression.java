package com.simomics.leishsim.model.equations;

import com.simomics.leishsim.Logging;
import com.simomics.leishsim.Logging.LogType;
import com.simomics.leishsim.simulation.Membrane;

/**
 * A parameter value in an equation.
 */
public class ParameterExpression implements Equation {

	private String parameterName;
	
	public ParameterExpression(String parameterName) {
		this.parameterName = parameterName;
	}
	
	@Override
	public String toString() {
		return String.format("(%s)", parameterName);
	}

	/**
	 * This is just a stub implementation. Parameter equations should never be evaluated.
	 * Instead, they should be simplified before evaluation, which replaces them with their (number) value.
	 */
	@Override
	public double evaluate(Membrane membrane) {
		Logging.getLogger(LogType.ERROR).log("Tried to evaluate a parameter expression: " + parameterName);
		return Double.NEGATIVE_INFINITY;
	}
	
	public String getParameterName() {
		return parameterName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		ParameterExpression other = (ParameterExpression) obj;
		return parameterName.equals(other.parameterName);
	}
}
