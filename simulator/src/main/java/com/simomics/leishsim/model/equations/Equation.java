package com.simomics.leishsim.model.equations;

import com.simomics.leishsim.simulation.Membrane;

/**
 * An equation involving parameters, species numbers and constants.
 */
public interface Equation {
	
	/**
	 * Compute the value of this equation relative to the given membrane.
	 * @param membrane
	 * @return
	 * @throws InvalidEquationEvaluationException If the equation could not be evaluated.
	 */
	public double evaluate(Membrane membrane) throws InvalidEquationEvaluationException;
	
	/** 
	 * Exception thrown when the equation could not be evaluated. 
	 */
	@SuppressWarnings("serial")
	public static class InvalidEquationEvaluationException extends Exception {
		public InvalidEquationEvaluationException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}