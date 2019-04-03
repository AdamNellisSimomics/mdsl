package com.simomics.leishsim.model.equations;

/**
 * An expression in an equation that multiplies two sub-expressions.
 */
public class MultiplyExpression extends OperationExpression {

	public MultiplyExpression(Equation left, Equation right) {
		super(left, right);
	}
	
	@Override
	public String toString() {
		return String.format("(%s * %s)", left, right);
	}

	@Override
	public double evaluate(double leftValue, double rightValue) {
		return leftValue * rightValue;
	}
}