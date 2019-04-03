package com.simomics.leishsim.model.equations;

/**
 * An expression in an equation that adds two sub-expressions.
 */
public class AddExpression extends OperationExpression {

	public AddExpression(Equation left, Equation right) {
		super(left, right);
	}
	
	@Override
	public String toString() {
		return String.format("(%s + %s)", left, right);
	}

	@Override
	public double evaluate(double leftValue, double rightValue) {
		return leftValue + rightValue;
	}
}