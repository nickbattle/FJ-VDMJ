/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package org.overturetool.vdmj.statements;

import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.expressions.BooleanLiteralExpression;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.pog.WhileLoopObligation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.UnionType;
import org.overturetool.vdmj.types.VoidType;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.VoidValue;

public class WhileStatement extends Statement
{
	private static final long serialVersionUID = 1L;
	public final Expression exp;
	public final Statement statement;

	public WhileStatement(LexLocation location, Expression exp, Statement body)
	{
		super(location);
		this.exp = exp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "while " + exp + " do " + statement;
	}

	@Override
	public String kind()
	{
		return "while";
	}

	@Override
	public Type typeCheck(Environment env, NameScope scope, Type constraint)
	{
		if (!exp.typeCheck(env, null, scope, null).isType(BooleanType.class, location))
		{
			exp.report(3218, "Expression is not boolean");
		}

		DefinitionList qualified = exp.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		Type stype = statement.typeCheck(qenv, scope, constraint);
		
		if (exp instanceof BooleanLiteralExpression && stype instanceof UnionType)
		{
			BooleanLiteralExpression ble = (BooleanLiteralExpression)exp;
			
			if (ble.value.value)	// while true do...
			{
				TypeSet edited = new TypeSet();
				UnionType original = (UnionType)stype;
				
				for (Type t: original.types)
				{
					if (!(t instanceof VoidType))
					{
						edited.add(t);
					}
				}
				
				stype = new UnionType(stype.location, edited);
			}
		}
		
		return stype;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			while (exp.eval(ctxt).boolValue(ctxt))
			{
				Value rv = statement.eval(ctxt);

				if (!rv.isVoid())
				{
					return rv;
				}
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}

		return new VoidValue();
	}

	@Override
	public TypeSet exitCheck()
	{
		return statement.exitCheck();
	}

	@Override
	public Statement findStatement(int lineno)
	{
		Statement found = super.findStatement(lineno);
		if (found != null) return found;
		return statement.findStatement(lineno);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		Expression found = exp.findExpression(lineno);
		if (found != null) return found;
		return statement.findExpression(lineno);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.add(new WhileLoopObligation(this, ctxt));
		obligations.addAll(exp.getProofObligations(ctxt));
		obligations.addAll(statement.getProofObligations(ctxt));
		return obligations;
	}
}
