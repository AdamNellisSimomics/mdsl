package com.simomics.leishsim.model.equations;

/**
 * An expression in an equation that divides two sub-expressions.
 */
public class DivideExpression extends OperationExpression {

	public DivideExpression(Equation left, Equation right) {
		super(left, right);
	}
	
	@Override
	public String toString() {
		return String.format("(%s / %s)", left, right);
	}

	@Override
	public double evaluate(double leftValue, double rightValue) throws InvalidEquationEvaluationException {
		if (rightValue == 0) {
			throw new InvalidEquationEvaluationException("Divide by zero", null);
		}
		return leftValue / rightValue;
	}
}
