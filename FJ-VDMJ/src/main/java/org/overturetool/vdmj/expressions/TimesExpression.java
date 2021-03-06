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

package org.overturetool.vdmj.expressions;

import org.overturetool.vdmj.lex.LexToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.IntegerType;
import org.overturetool.vdmj.types.NaturalOneType;
import org.overturetool.vdmj.types.NaturalType;
import org.overturetool.vdmj.types.NumericType;
import org.overturetool.vdmj.types.RealType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.NumericValue;
import org.overturetool.vdmj.values.Value;

public class TimesExpression extends NumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TimesExpression(Expression left, LexToken op, Expression right)
	{
		super(left, op, right);
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		checkNumeric(env, scope);

		NumericType ln = ltype.getNumeric();
		NumericType rn = rtype.getNumeric();

		if (ln instanceof RealType)
		{
			return ln;
		}
		else if (rn instanceof RealType)
		{
			return rn;
		}
		else if (ln instanceof IntegerType)
		{
			return ln;
		}
		else if (rn instanceof IntegerType)
		{
			return rn;
		}
		else if (ln instanceof NaturalType)
		{
			return ln;
		}
		else if (rn instanceof NaturalType)
		{
			return rn;
		}
		else
		{
			return new NaturalOneType(ln.location);
		}
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
    		Value l = left.eval(ctxt);
    		Value r = right.eval(ctxt);

			if (NumericValue.areIntegers(l, r))
			{
				long lv = l.intValue(ctxt);
				long rv = r.intValue(ctxt);
				long mult = multiplyExact(lv, rv, ctxt);
				return NumericValue.valueOf(mult, ctxt);
			}
			else
			{
				double lv = l.realValue(ctxt);
				double rv = r.realValue(ctxt);
	    		return NumericValue.valueOf(lv * rv, ctxt);
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public String kind()
	{
		return "multiply";
	}
	
	// This is included in Java 8 Math.java
    private long multiplyExact(long x, long y, Context ctxt) throws ValueException
    {
    	long r = x * y;
    	long ax = Math.abs(x);
    	long ay = Math.abs(y);

    	if (((ax | ay) >>> 31 != 0))
    	{
    		if (((y != 0) && (r / y != x)) || (x == Long.MIN_VALUE && y == -1))
    		{
    			throw new ValueException(4169, "Arithmetic overflow", ctxt);
    		}
    	}

    	return r;
    }
}
