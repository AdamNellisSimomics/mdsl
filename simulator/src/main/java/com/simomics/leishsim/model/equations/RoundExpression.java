package com.simomics.leishsim.model.equations;

/**
 * An expression in an equation that rounds a double to an integer.
 */
public class RoundExpression extends UnaryOperationExpression {

	public RoundExpression(Equation subexpression) {
		super(subexpression);
	}

	@Override 
	public String toString() {
		return String.format("round(%s)", subexpression);
	}
	
	@Override
	public double evaluate(double subexpressionValue) throws InvalidEquationEvaluationException {
		return Math.round(subexpressionValue);
	}
}
