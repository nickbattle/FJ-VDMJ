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

import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold an external state definition.
 */
public class INExternalDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INDefinition state;
	public final boolean readOnly;
	public final TCNameToken oldname;	// For "wr" only

	public INExternalDefinition(INDefinition state, boolean readOnly)
	{
		super(state.location, null, state.name);
		this.state = state;
		this.readOnly = readOnly;
		this.oldname = readOnly ? null : state.name.getOldName();
	}

	@Override
	public INDefinition findName(TCNameToken sought)
	{
		if (sought.isOld())
		{
			return (sought.equals(oldname)) ? this : null;
		}

		return (sought.equals(state.name)) ? this : null;
	}

	@Override
	public String toString()
	{
		return (readOnly ? "ext rd " : "ext wr ") + state.name;
	}

	@Override
	public TCType getType()
	{
		return state.getType();
	}

	@Override
	public boolean isUpdatable()
	{
		return true;
	}
}
