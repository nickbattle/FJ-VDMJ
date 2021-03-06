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

package org.overturetool.vdmj.definitions;

import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.values.NameValuePair;
import org.overturetool.vdmj.values.NameValuePairList;

/**
 * A class to hold an inherited definition in VDM++.
 */

public class InheritedDefinition extends Definition
{
	private static final long serialVersionUID = 1L;
	public Definition superdef;
	private final LexNameToken oldname;

	public InheritedDefinition(LexNameToken localname, Definition def)
	{
		super(def.pass, def.location, localname, def.nameScope);

		this.superdef = def;
		this.oldname = localname.getOldName();

		setAccessSpecifier(def.accessSpecifier);
		setClassDefinition(def.classDefinition);
	}

	private void checkSuperDefinition()
	{
		// This is used to get over the case where an inherited definition
		// is a ValueDefinition that has since been replaced with a new
		// LocalDefinition. It would be better to somehow list the
		// inherited definitions that refer to a LocalDefinition and update
		// them...

		if (superdef instanceof UntypedDefinition)
		{
			if (classDefinition != null)
			{
				superdef = classDefinition.findName(superdef.name, nameScope);
			}
		}
	}

	@Override
	public Type getType()
	{
		checkSuperDefinition();
		return superdef.getType();
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") + superdef.toString(); //getVariableNames() + ":" + getType();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		superdef.typeCheck(base, scope);
	}

	@Override
    public void markUsed()
	{
		used = true;
		superdef.markUsed();
	}

	@Override
    protected boolean isUsed()
	{
		return superdef.isUsed();
	}

	@Override
	public DefinitionList getDefinitions()
	{
		return superdef.getDefinitions();
	}

	@Override
	public LexNameList getVariableNames()
	{
		LexNameList names = new LexNameList();
		checkSuperDefinition();

		for (LexNameToken vn: superdef.getVariableNames())
		{
			names.add(vn.getModifiedName(name.module));
		}

		return names;
	}

	@Override
	public Definition findName(LexNameToken sought, NameScope scope)
	{
		// The problem is, when the InheritedDefinition is created, we
		// don't know its fully qualified name.
		
		if (superdef instanceof InheritedDefinition)
		{
			superdef.findName(sought, scope);
		}

		name.setTypeQualifier(superdef.name.typeQualifier);

		if (name.equals(sought))
		{
			return this;
		}
		else if (scope.matches(NameScope.OLDSTATE) && oldname.equals(sought))
		{
			return this;
		}

		return null;
	}

	@Override
	public Definition findType(LexNameToken sought, String fromModule)
	{
		if (superdef instanceof TypeDefinition && sought.equals(name))
		{
			return this;
		}

		return null;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList renamed = new NameValuePairList();

		if (superdef instanceof UntypedDefinition)
		{
			if (classDefinition != null)
			{
				superdef = classDefinition.findName(superdef.name, nameScope);
			}
		}

		for (NameValuePair nv: superdef.getNamedValues(ctxt))
		{
			renamed.add(new NameValuePair(
				nv.name.getModifiedName(name.module), nv.value));
		}

		return renamed;
	}

	@Override
	public String kind()
	{
		return superdef.kind();
	}

	@Override
	public boolean isFunction()
	{
		return superdef.isFunction();
	}

	@Override
	public boolean isOperation()
	{
		return superdef.isOperation();
	}

	@Override
	public boolean isCallableOperation()
	{
		return superdef.isCallableOperation();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return superdef.isInstanceVariable();
	}

	@Override
	public boolean isTypeDefinition()
	{
		return superdef.isTypeDefinition();
	}

	@Override
	public boolean isValueDefinition()
	{
		return superdef.isValueDefinition();
	}

	@Override
	public boolean isRuntime()
	{
		return superdef.isRuntime();
	}

	@Override
	public boolean isUpdatable()
	{
		return superdef.isUpdatable();
	}

	@Override
	public Definition deref()
	{
		return superdef.deref();
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return superdef.isSubclassResponsibility();
	}
}
