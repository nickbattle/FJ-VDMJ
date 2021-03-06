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

package org.overturetool.vdmj.modules;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.definitions.LocalDefinition;
import org.overturetool.vdmj.definitions.UntypedDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.util.Utils;

public class ExportedValue extends Export
{
	private static final long serialVersionUID = 1L;
	public final LexNameList nameList;
	public final Type type;

	public ExportedValue(LexLocation location, LexNameList nameList, Type type)
	{
		super(location);
		this.nameList = nameList;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return "export value " + Utils.listToString(nameList) + ":" + type;
	}

	@Override
	public DefinitionList getDefinition(DefinitionList actualDefs)
	{
		DefinitionList list = new DefinitionList();

		for (LexNameToken name: nameList)
		{
			Definition def = actualDefs.findName(name, NameScope.NAMES);

			if (def != null)
			{
    			if (def instanceof UntypedDefinition)
    			{
    				UntypedDefinition untyped = (UntypedDefinition)def;
    				list.add(new LocalDefinition(untyped.location, untyped.name, NameScope.GLOBAL, type));
    			}
    			else
    			{
    				list.add(def);
    			}
			}
		}

		return list;
	}

	@Override
	public DefinitionList getDefinition()
	{
		DefinitionList list = new DefinitionList();

		for (LexNameToken name: nameList)
		{
			list.add(new LocalDefinition(name.location, name, NameScope.GLOBAL, type));
		}

		return list;
	}

	@Override
	public void typeCheck(Environment env, DefinitionList actualDefs)
	{
		Type resolved = type.typeResolve(env, null);
		
		for (LexNameToken name: nameList)
		{
			Definition actual = actualDefs.findName(name, NameScope.GLOBAL);
			
			if (actual == null)
			{
				report(3188, "Exported value " + name + " not defined in module");
			}
			else
			{
    			Type actualType = actual.getType();
    			
				if (actualType != null && !TypeComparator.compatible(resolved, actualType))
				{
					report(3189, "Exported type does not match actual type");
					detail2("Exported", resolved, "Actual", actualType);
				}
			}
		}
	}
}
