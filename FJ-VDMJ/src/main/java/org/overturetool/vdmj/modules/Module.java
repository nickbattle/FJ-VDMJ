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

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.definitions.RenamedDefinition;
import org.overturetool.vdmj.definitions.StateDefinition;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexIdentifierToken;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ContextException;
import org.overturetool.vdmj.runtime.StateContext;
import org.overturetool.vdmj.statements.Statement;
import org.overturetool.vdmj.typechecker.ModuleEnvironment;
import org.overturetool.vdmj.typechecker.TypeChecker;
import org.overturetool.vdmj.util.Delegate;
import org.overturetool.vdmj.values.Value;

/**
 * A class holding all the details for one module.
 */

public class Module implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The module name. */
	public final LexIdentifierToken name;
	/** A list of import declarations. */
	public final ModuleImports imports;
	/** A list of export declarations. */
	public final ModuleExports exports;
	/** A list of definitions created in the module. */
	public final DefinitionList defs;
	/** A list of source file names for the module. */
	public final List<File> files;

	/** Those definitions which are exported. */
	public DefinitionList exportdefs;
	/** Definitions of imported objects from other modules. */
	public DefinitionList importdefs;
	/** True if the module was loaded from an object file. */
	public boolean typechecked = false;
	/** True if the "module" is actually a flat definition file. */
	public boolean isFlat = false;

	/** A delegate Java class, if one exists. */
	private Delegate delegate = null;
	/** A delegate Java object, if one exists. */
	private Object delegateObject = null;

	/**
	 * Create a module from the given name and definitions.
	 */

	public Module(LexIdentifierToken name,
		ModuleImports imports,
		ModuleExports exports,
		DefinitionList defs)
	{
		this.name = name;
		this.imports = imports;
		this.exports = exports;
		this.defs = defs;
		this.files = new Vector<File>();

		files.add(name.location.file);

		exportdefs = new DefinitionList();	// By default, export nothing
		importdefs = new DefinitionList();	// and import nothing

		this.delegate = new Delegate(name.name, defs);
	}

	/**
	 * Create a module with a default name from the given definitions.
	 */

	public Module(File file, DefinitionList defs)
	{
		if (defs.isEmpty())
		{
			this.name =	defaultName(new LexLocation());
		}
		else
		{
    		this.name = defaultName(defs.get(0).location);
 		}

		this.imports = null;
		this.exports = null;
		this.defs = defs;
		this.files = new Vector<File>();

		if (file != null)
		{
			files.add(file);
		}

		exportdefs = new DefinitionList();	// Export nothing
		importdefs = new DefinitionList();	// and import nothing

		this.delegate = new Delegate(name.name, defs);

		isFlat = true;
	}

	/**
	 * Create a module called DEFAULT with no file and no definitions.
	 */

	public Module()
	{
		this(null, new DefinitionList());
	}

	/**
	 * Generate the default module name.
	 *
	 * @param location	The textual location of the name
	 * @return	The default module name.
	 */

	public static LexIdentifierToken defaultName(LexLocation location)
	{
		return new LexIdentifierToken("DEFAULT", false, location);
	}

	/**
	 * Generate the exportdefs list of definitions. The exports list of
	 * export declarations is processed by searching the defs list of
	 * locally defined objects. The exportdefs field is populated with
	 * the result.
	 */

	public void processExports()
	{
		if (exports != null)
		{
			exportdefs.addAll(exports.getDefinitions(defs));
		}
	}

	/**
	 * Generate the importdefs list of definitions. The imports list of
	 * import declarations is processed by searching the module list passed
	 * in. The importdefs field is populated with the result.
	 */

	public void processImports(ModuleList allModules)
	{
		if (imports != null)
		{
			DefinitionList updated = imports.getDefinitions(allModules);

			D: for (Definition u: updated)
			{
				for (Definition tc: importdefs)
				{
					if (tc.name != null && u.name != null && tc.name.matches(u.name))
					{
						u.used = tc.used;	// Copy usage from TC phase
						continue D;
					}
				}
			}

			importdefs.clear();
			importdefs.addAll(updated);
		}
	}

	/**
	 * Type check the imports, compared to their export definitions.
	 */

	public void typeCheckImports()
	{
		if (imports != null)
		{
			imports.typeCheck(new ModuleEnvironment(this));
		}
	}

	/**
	 * Type check the exports, compared to their local definitions.
	 */

	public void typeCheckExports()
	{
		if (exports != null)
		{
			exports.typeCheck(new ModuleEnvironment(this), defs);
		}
	}

	/**
	 * Return the module's state context, if any. Modules which define
	 * state produce a {@link Context} object that contains the state field
	 * values. This is independent of the initial context.
	 *
	 * @return	The state context, or null.
	 */

	public Context getStateContext()
	{
		StateDefinition sdef = defs.findStateDefinition();

		if (sdef != null)
		{
			return sdef.getStateContext();
		}

		return null;
	}

	/**
	 * Initialize the system for execution from this module. The initial
	 * {@link Context} is created, and populated with name/value pairs from the
	 * local definitions and the imported definitions. If state is defined
	 * by the module, this is also initialized, creating the state Context.
	 *
	 * @return True if initialized OK.
	 */

	public Set<ContextException> initialize(StateContext initialContext)
	{
		Set<ContextException> trouble = new HashSet<ContextException>();

		for (Definition d: importdefs)
		{
			if (d instanceof RenamedDefinition)
			{
				try
				{
					initialContext.putList(d.getNamedValues(initialContext));
				}
				catch (ContextException e)
				{
					trouble.add(e);		// Carry on...
				}
			}
		}

		for (Definition d: defs)
		{
			try
			{
				initialContext.putList(d.getNamedValues(initialContext));
			}
			catch (ContextException e)
			{
				trouble.add(e);		// Carry on...
			}
		}

		try
		{
			StateDefinition sdef = defs.findStateDefinition();

			if (sdef != null)
			{
				sdef.initState(initialContext);
			}
		}
		catch (ContextException e)
		{
			trouble.add(e);		// Carry on...
		}

		return trouble;
	}

	/**
	 * Find the first {@link Statement} in the module that starts on a given line.
	 *
	 * @param file The file to search for.
	 * @param lineno The line number to search for.
	 * @return	The first {@link Statement} on that line, or null.
	 */

	public Statement findStatement(File file, int lineno)
	{
		// The DEFAULT module can include definitions from many files,
		// so we have to consider each definition's file before searching
		// within that for the statement.

		for (Definition d: defs)
		{
			if (d.location.file.equals(file))
			{
				Statement stmt = d.findStatement(lineno);

				if (stmt != null)
				{
					return stmt;
				}
			}
		}

		return null;
	}

	/**
	 * Find the first {@link Expression} in the module that starts on a given line.
	 *
	 * @param file The file to search for.
	 * @param lineno The line number to search for.
	 * @return	The first {@link Expression} on that line, or null.
	 */

	public Expression findExpression(File file, int lineno)
	{
		// The DEFAULT module can include definitions from many files,
		// so we have to consider each definition's file before searching
		// within that for the expression.

		for (Definition d: defs)
		{
			if (d.location.file.equals(file))
			{
				Expression exp = d.findExpression(lineno);

				if (exp != null)
				{
					return exp;
				}
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("module " + name.name + "\n");

		if (imports != null)
		{
			sb.append("\nimports\n\n");
			sb.append(imports.toString());
		}

		if (exports != null)
		{
			sb.append("\nexports\n\n");
			sb.append(exports.toString());
		}
		else
		{
			sb.append("\nexports all\n\n");
		}

		if (defs != null)
		{
			sb.append("\ndefinitions\n\n");

			for (Definition def: defs)
			{
				sb.append(def.toString() + "\n");
			}
		}

		sb.append("\nend " + name.name + "\n");

		return sb.toString();
	}

	public ProofObligationList getProofObligations()
	{
		return defs.getProofObligations(new POContextStack());
	}

	public boolean hasDelegate()
	{
		if (delegate.hasDelegate())
		{
			if (delegateObject == null)
			{
				delegateObject = delegate.newInstance();
			}

			return true;
		}

		return false;
	}

	public Value invokeDelegate(Context ctxt)
	{
		return delegate.invokeDelegate(delegateObject, ctxt);
	}

	public void checkOver()
	{
		List<String> done = new Vector<String>();

		DefinitionList singles = defs.singleDefinitions();

		for (Definition def1: singles)
		{
			for (Definition def2: singles)
			{
				if (def1 != def2 &&
					def1.name != null && def2.name != null &&
					def1.name.name.equals(def2.name.name) &&
					!done.contains(def1.name.name))
				{
					if ((def1.isFunction() && !def2.isFunction()) ||
						(def1.isOperation() && !def2.isOperation()))
					{
						def1.report(3017, "Duplicate definitions for " + def1.name.name);
						TypeChecker.detail2(def1.name.name, def1.location, def2.name.name, def2.location);
						done.add(def1.name.name);
					}
				}
			}
		}
	}
}
