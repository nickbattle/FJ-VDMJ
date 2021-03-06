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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

abstract public class INSimpleBlockStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INStatementList statements;

	public INSimpleBlockStatement(LexLocation location, INStatementList statements)
	{
		super(location);
		location.executable(false);
		this.statements = statements;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (INStatement s: statements)
		{
			sb.append(sep);
			sb.append(s.toString());
			sep = ";\n";
		}

		sb.append("\n");
		return sb.toString();
	}

	@Override
	public INStatement findStatement(int lineno)
	{
		if (location.startLine == lineno) return this;
		INStatement found = null;

		for (INStatement stmt: statements)
		{
			found = stmt.findStatement(lineno);
			if (found != null) break;
		}

		return found;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = null;

		for (INStatement stmt: statements)
		{
			found = stmt.findExpression(lineno);
			if (found != null) break;
		}

		return found;
	}

	@Override
	abstract public Value eval(Context ctxt);

	protected Value evalBlock(Context ctxt)
	{
		// Note, no breakpoint check - designed to be called by eval

		for (INStatement s: statements)
		{
			Value rv = s.eval(ctxt);

			if (!rv.isVoid())
			{
				return rv;
			}
		}

		return new VoidValue();
	}
}
