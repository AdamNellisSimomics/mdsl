package com.simomics.leishsim.model.equations;

import com.simomics.leishsim.simulation.Membrane;

/**
 * An operation in an equation that performs a unary operation on one expression.
 */
public abstract class UnaryOperationExpression implements Equation {
	
	protected final Equation subexpression;

	public UnaryOperationExpression(Equation subexpression) {
		this.subexpression = subexpression;
	}
	
	public Equation getSubexpression() {
		return subexpression;
	}
	
	@Override
	public final double evaluate(Membrane membrane) throws InvalidEquationEvaluationException {
		return evaluate(subexpression.evaluate(membrane));
	}
	
	/**
	 * Change the signature of the evaluate method for operations.
	 */
	public abstract double evaluate(double subexpressionValue) throws InvalidEquationEvaluationException;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		UnaryOperationExpression other = (UnaryOperationExpression) obj;
		return subexpression.equals(other.subexpression);
	}
}
