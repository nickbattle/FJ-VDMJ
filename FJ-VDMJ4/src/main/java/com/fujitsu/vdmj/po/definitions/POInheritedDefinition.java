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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold an inherited definition in VDM++.
 */
public class POInheritedDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final PODefinition superdef;

	public POInheritedDefinition(TCNameToken localname, PODefinition def)
	{
		super(def.location, localname);
		this.superdef = def;
	}

	@Override
	public TCType getType()
	{
		return superdef.getType();
	}

	@Override
	public String toString()
	{
		return superdef.toString();
	}

	@Override
	public TCNameList getVariableNames()
	{
		TCNameList names = new TCNameList();

		for (TCNameToken vn: superdef.getVariableNames())
		{
			names.add(vn.getModifiedName(name.getModule()));
		}

		return names;
	}
}
