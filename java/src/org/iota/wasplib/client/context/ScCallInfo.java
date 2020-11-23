// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package org.iota.wasplib.client.context;

import org.iota.wasplib.client.hashtypes.ScColor;
import org.iota.wasplib.client.immutable.ScImmutableMap;
import org.iota.wasplib.client.mutable.ScMutableMap;
import org.iota.wasplib.client.mutable.ScMutableMapArray;

public class ScCallInfo {
	ScMutableMap call;

	ScCallInfo(ScMutableMap call) {
		this.call = call;
	}

	void Call() {
		call.GetInt("delay").SetValue(-1);
	}

	public ScMutableMap Params() {
		return call.GetMap("params");
	}

	public ScImmutableMap Results() {
		return call.GetMap("results").Immutable();
	}

	public void Transfer(ScColor color, long amount) {
		ScMutableMapArray transfers = call.GetMapArray("transfers");
		ScMutableMap transfer = transfers.GetMap(transfers.Length());
		transfer.GetColor("color").SetValue(color);
		transfer.GetInt("amount").SetValue(amount);
	}
}
