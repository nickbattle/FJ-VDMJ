/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.traces;

import java.util.Vector;

import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;

/**
 * A class representing a sequence of trace definitions.
 */

@SuppressWarnings("serial")
public class TraceDefinitionTerm extends Vector<TraceDefinition>
{
	public void typeCheck(Environment base, NameScope scope)
	{
		for (TraceDefinition def: this)
		{
			def.typeCheck(base, scope);
		}
	}

	public TraceIterator getIterator(Context ctxt)
	{
		TraceIteratorList list = new TraceIteratorList();

		for (TraceDefinition term: this)
		{
			list.add(term.getIterator(ctxt));
		}

		return list.getAlternatveIterator();
	}
}
