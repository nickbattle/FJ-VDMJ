/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INFuncInstantiationExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression function;
	public final TCTypeList actualTypes;
	public final INExplicitFunctionDefinition expdef;
	public final INImplicitFunctionDefinition impdef;

	public INFuncInstantiationExpression(INExpression function, TCTypeList actualTypes,
		INExplicitFunctionDefinition expdef, INImplicitFunctionDefinition impdef)
	{
		super(function);
		this.function = function;
		this.actualTypes = actualTypes;
		this.expdef = expdef;
		this.impdef = impdef;
	}

	@Override
	public String toString()
	{
		return "(" + function + ")[" + Utils.listToString(actualTypes) + "]";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		FunctionValue fv = function.eval(ctxt).functionValue(ctxt);

    		if (!fv.uninstantiated)
    		{
    			abort(3034, "Function is already instantiated: " + fv.name, ctxt);
    		}

    		TCTypeList fixed = new TCTypeList();

    		for (TCType ptype: actualTypes)
    		{
    			if (ptype instanceof TCParameterType)
    			{
    				TCParameterType pname = (TCParameterType)ptype;
    				Value t = ctxt.lookup(pname.name);

    				if (t == null)
    				{
    					abort(4008, "No such type parameter @" + pname + " in scope", ctxt);
    				}
    				else if (t instanceof ParameterValue)
    				{
    					ParameterValue tv = (ParameterValue)t;
    					fixed.add(tv.type);
    				}
    				else
    				{
    					abort(4009, "INType parameter/local variable name clash, @" + pname, ctxt);
    				}
    			}
    			else
    			{
    				fixed.add(ptype);
    			}
    		}
    		
    		FunctionValue rv = null;
    		
    		if (expdef == null)
			{
				rv = impdef.getPolymorphicValue(fixed);
			}
			else
			{
				rv = expdef.getPolymorphicValue(fixed);
			}

    		rv.setSelf(fv.self);
			rv.uninstantiated = false;
			return rv;
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return function.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return function.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return function.getOldNames();
	}
}
