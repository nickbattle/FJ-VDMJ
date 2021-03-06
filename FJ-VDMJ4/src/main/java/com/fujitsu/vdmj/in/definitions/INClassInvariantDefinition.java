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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A VDM class invariant definition.
 */
public class INClassInvariantDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INExpression expression;

	public INClassInvariantDefinition(TCNameToken name, INExpression expression)
	{
		super(name.getLocation(), null, name);
		this.expression = expression;
	}

	@Override
	public INDefinition findName(TCNameToken sought)
	{
		return null;		// We can never find inv_C().
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		return expression.findExpression(lineno);
	}

	@Override
	public TCType getType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public String toString()
	{
		return "inv " + expression;
	}
}
