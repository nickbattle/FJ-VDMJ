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

package org.overturetool.vdmj.patterns;

import java.util.List;
import java.util.Vector;

import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.expressions.BooleanLiteralExpression;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexBooleanToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.PatternMatchException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.Value;

public class BooleanPattern extends Pattern
{
	private static final long serialVersionUID = 1L;
	public final LexBooleanToken value;

	public BooleanPattern(LexBooleanToken token)
	{
		super(token.location);
		this.value = token;
	}

	@Override
	public String toString()
	{
		return value.toString();
	}

	@Override
	public DefinitionList getAllDefinitions(Type type, NameScope scope)
	{
		return new DefinitionList();
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt)
		throws PatternMatchException
	{
		List<NameValuePairList> result = new Vector<NameValuePairList>();

		try
		{
			if (expval.boolValue(ctxt) != value.value)
			{
				patternFail(4106, "Boolean pattern match failed");
			}
		}
		catch (ValueException e)
		{
			patternFail(e);
		}

		result.add(new NameValuePairList());
		return result;
	}

	@Override
	public Type getPossibleType()
	{
		return new BooleanType(location);
	}

	@Override
	public Expression getMatchingExpression()
	{
		return new BooleanLiteralExpression(value);
	}
}
