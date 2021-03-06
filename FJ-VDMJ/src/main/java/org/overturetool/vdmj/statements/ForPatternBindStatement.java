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

import java.util.ListIterator;

import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.patterns.PatternBind;
import org.overturetool.vdmj.patterns.SeqBind;
import org.overturetool.vdmj.patterns.SetBind;
import org.overturetool.vdmj.patterns.TypeBind;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.POForAllSequenceContext;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.pog.SeqMemberObligation;
import org.overturetool.vdmj.pog.SetMemberObligation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.PatternMatchException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatCheckedEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.SeqType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueSet;
import org.overturetool.vdmj.values.VoidValue;


public class ForPatternBindStatement extends Statement
{
	private static final long serialVersionUID = 1L;
	public final PatternBind patternBind;
	public final boolean reverse;
	public final Expression exp;
	public final Statement statement;

	private SeqType seqType;

	public ForPatternBindStatement(LexLocation location,
		PatternBind patternBind, boolean reverse, Expression exp, Statement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.exp = exp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "for " + patternBind + " in " +
			(reverse ? " reverse " : "") + exp + " do\n" + statement;
	}

	@Override
	public String kind()
	{
		return "for";
	}

	@Override
	public Type typeCheck(Environment base, NameScope scope, Type constraint)
	{
		Type stype = exp.typeCheck(base, null, scope, null);
		Environment local = base;

		if (stype.isSeq(location))
		{
			seqType = stype.getSeq();
			patternBind.typeCheck(base, scope, seqType.seqof);
			DefinitionList defs = patternBind.getDefinitions();
			defs.typeCheck(base, scope);
			local = new FlatCheckedEnvironment(defs, base, scope);
		}
		else
		{
			report(3223, "Expecting sequence type after 'in'");
		}

		Type rt = statement.typeCheck(local, scope, constraint);
		local.unusedCheck();
		return rt;
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
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			ValueList values = exp.eval(ctxt).seqValue(ctxt);

			if (reverse)
			{
				ListIterator<Value> li = values.listIterator(values.size());
				ValueList backwards = new ValueList();

				while (li.hasPrevious())
				{
					backwards.add(li.previous());
				}

				values = backwards;
			}

			if (patternBind.pattern != null)
			{
				for (Value val: values)
				{
					try
					{
						Context evalContext = new Context(location, "for pattern", ctxt);
						evalContext.putList(patternBind.pattern.getNamedValues(val, ctxt));
						Value rv = statement.eval(evalContext);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else if (patternBind.bind instanceof SetBind)
			{
				SetBind setbind = (SetBind)patternBind.bind;
				ValueSet set = setbind.set.eval(ctxt).setValue(ctxt);

				for (Value val: values)
				{
					try
					{
						if (!set.contains(val))
						{
							abort(4039, "Set bind does not contain value " + val, ctxt);
						}

						Context evalContext = new Context(location, "for set bind", ctxt);
						evalContext.putList(setbind.pattern.getNamedValues(val, ctxt));
						Value rv = statement.eval(evalContext);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else if (patternBind.bind instanceof SeqBind)
			{
				SeqBind seqbind = (SeqBind)patternBind.bind;
				ValueList seq = seqbind.sequence.eval(ctxt).seqValue(ctxt);

				for (Value val: values)
				{
					try
					{
						if (!seq.contains(val))
						{
							abort(4039, "Seq bind does not contain value " + val, ctxt);
						}

						Context evalContext = new Context(location, "for seq bind", ctxt);
						evalContext.putList(seqbind.pattern.getNamedValues(val, ctxt));
						Value rv = statement.eval(evalContext);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else
			{
				TypeBind typebind = (TypeBind)patternBind.bind;

				for (Value val: values)
				{
					try
					{
						Value converted = val.convertTo(typebind.type, ctxt);

						Context evalContext = new Context(location, "for type bind", ctxt);
						evalContext.putList(typebind.pattern.getNamedValues(converted, ctxt));
						Value rv = statement.eval(evalContext);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
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
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = exp.getProofObligations(ctxt);

		if (patternBind.pattern != null)
		{
			// Nothing to do
		}
		else if (patternBind.bind instanceof TypeBind)
		{
			// Nothing to do
		}
		else if (patternBind.bind instanceof SetBind)
		{
			SetBind bind = (SetBind)patternBind.bind;
			list.addAll(bind.set.getProofObligations(ctxt));
			
			ctxt.push(new POForAllSequenceContext(bind, exp));
			list.add(new SetMemberObligation(bind.pattern.getMatchingExpression(), bind.set, ctxt));
			ctxt.pop();
		}
		else if (patternBind.bind instanceof SeqBind)
		{
			SeqBind bind = (SeqBind)patternBind.bind;
			list.addAll(bind.sequence.getProofObligations(ctxt));
			
			ctxt.push(new POForAllSequenceContext(bind, exp));
			list.add(new SeqMemberObligation(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
			ctxt.pop();
		}

		list.addAll(statement.getProofObligations(ctxt));
		return list;
	}
}
