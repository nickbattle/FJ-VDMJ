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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.pog.FuncComposeObligation;
import com.fujitsu.vdmj.pog.MapComposeObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCType;

public class POCompExpression extends POBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public POCompExpression(POExpression left, LexToken op, POExpression right, TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (ltype.isFunction(location))
		{
			String pref1 = left.getPreName();
			String pref2 = right.getPreName();

			if (pref1 == null || !pref1.equals(""))
			{
				obligations.add(new FuncComposeObligation(
					this, pref1, pref2, ctxt));
			}
		}

		if (ltype.isMap(location))
		{
			obligations.add(new MapComposeObligation(this, ctxt));
		}

		return obligations;
	}
}
