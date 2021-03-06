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

package org.overturetool.vdmj.statements;

import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.pog.SeqApplyObligation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.FunctionType;
import org.overturetool.vdmj.types.MapType;
import org.overturetool.vdmj.types.OperationType;
import org.overturetool.vdmj.types.SeqType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.MapValue;
import org.overturetool.vdmj.values.SeqValue;
import org.overturetool.vdmj.values.UpdatableValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueMap;

public class MapSeqDesignator extends StateDesignator
{
	private static final long serialVersionUID = 1L;
	public final StateDesignator mapseq;
	public final Expression exp;

	private MapType mapType;
	private SeqType seqType;

	public MapSeqDesignator(StateDesignator mapseq, Expression exp)
	{
		super(mapseq.location);
		this.mapseq = mapseq;
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return mapseq + "(" + exp + ")";
	}

	@Override
	public Type typeCheck(Environment env)
	{
		Type etype = exp.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
		Type rtype = mapseq.typeCheck(env);
		TypeSet result = new TypeSet();

		if (rtype.isMap(location))
		{
			mapType = rtype.getMap();

			if (!TypeComparator.compatible(mapType.from, etype))
			{
				report(3242, "Map element assignment of wrong type");
				detail2("Expect", mapType.from, "Actual", etype);
			}
			else
			{
				result.add(mapType.to);
			}
		}

		if (rtype.isSeq(location))
		{
			seqType = rtype.getSeq();

			if (!etype.isNumeric(location))
			{
				report(3243, "Seq index is not numeric");
				detail("Actual", etype);
			}
			else
			{
				result.add(seqType.seqof);
			}
		}
		
		if (rtype.isFunction(location))
		{
			// Error case, but improves errors if we work out the return type
			FunctionType ftype = rtype.getFunction();
			result.add(ftype.result);
		}

		if (rtype.isOperation(location))
		{
			// Error case, but improves errors if we work out the return type
			OperationType otype = rtype.getOperation();
			result.add(otype.result);
		}

		if (result.isEmpty())
		{
			report(3244, "Expecting a map or a sequence");
			return new UnknownType(location);
		}

		return result.getType(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		Value result = null;

		try
		{
			Value root = mapseq.eval(ctxt);
			Value index = exp.eval(ctxt);

			if (root.isType(MapValue.class))
			{
				index = index.convertTo(mapType.from, ctxt);
				ValueMap map = root.mapValue(ctxt);
				result = map.get(index);

				if (result == null && root instanceof UpdatableValue)
				{
					// Assignment to a non-existent map key creates the value
					// in order to have it updated.

					UpdatableValue ur = (UpdatableValue)root;
					result = UpdatableValue.factory(ur.listeners, mapType.to);
					map.put(index, result);
				}
			}
			else if (root.isType(SeqValue.class))
			{
				ValueList seq = root.seqValue(ctxt);
				int i = (int)index.intValue(ctxt)-1;

				if (!seq.inbounds(i))
				{
					if (i == seq.size())
					{
						// Assignment to an index one greater than the length
						// creates the value in order to have it updated.

						UpdatableValue ur = (UpdatableValue)root;
						seq.add(UpdatableValue.factory(ur.listeners, seqType.seqof));
					}
					else
					{
						exp.abort(4019, "Sequence cannot extend to key: " + index, ctxt);
					}
				}

				result = seq.get(i);
			}
			else
			{
				abort(4020, "State value is neither a sequence nor a map", ctxt);
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}

		return result;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = new ProofObligationList();

		if (seqType != null)
		{
			list.add(new SeqApplyObligation(mapseq, exp, ctxt));
		}
		
		// Maps are OK, as you can create new map domain entries

		return list;
	}
}
