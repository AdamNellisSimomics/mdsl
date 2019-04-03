package com.simomics.leishsim.model.equations;

import com.simomics.leishsim.simulation.Membrane;

/**
 * An expression in an equation that performs an operation on two sub-expressions.
 */
public abstract class OperationExpression implements Equation {
	
	/** The left hand side of the operation */
	protected Equation left;
	
	/** The right hand side of the operation */
	protected Equation right;
	
	public OperationExpression(Equation left, Equation right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public final double evaluate(Membrane membrane) throws InvalidEquationEvaluationException {
		return evaluate(left.evaluate(membrane), right.evaluate(membrane));
	}
	
	/**
	 * Change the signature of the evaluate method for operations.
	 * @param leftValue
	 * @param rightValue
	 * @return
	 * @throws InvalidEquationEvaluationException 
	 */
	public abstract double evaluate(double leftValue, double rightValue) throws InvalidEquationEvaluationException;
	
	public Equation getLeft() {
		return left;
	}
	
	public Equation getRight() {
		return right;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj.getClass() != getClass()) { return false; }
		OperationExpression other = (OperationExpression) obj;
		return (left.equals(other.left)) && (right.equals(other.right));
	}
}
