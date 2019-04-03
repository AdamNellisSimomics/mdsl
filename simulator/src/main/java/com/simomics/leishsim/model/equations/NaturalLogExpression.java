package com.simomics.leishsim.model.equations;

/**
 * An expression in an equation that calculates a natural log.
 */
public class NaturalLogExpression extends UnaryOperationExpression {

	public NaturalLogExpression(Equation subexpression) {
		super(subexpression);
	}

	@Override 
	public String toString() {
		return String.format("ln(%s)", subexpression);
	}
	
	@Override
	public double evaluate(double subexpressionValue) throws InvalidEquationEvaluationException {
		return Math.log(subexpressionValue);
	}

}
