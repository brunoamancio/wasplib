// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package org.iota.wasplib.client.context;

import org.iota.wasplib.client.hashtypes.ScHash;
import org.iota.wasplib.client.keys.Key;
import org.iota.wasplib.client.mutable.ScMutableMap;
import org.iota.wasplib.client.mutable.ScMutableMapArray;

public class ScDeployInfo {
	protected static final ScMutableMap root = new ScMutableMap(1);

	ScMutableMap deploy;

	protected ScDeployInfo(String name, String description) {
		ScMutableMapArray deploys = root.GetMapArray(Key.Deploys);
		deploy = deploys.GetMap(deploys.Length());
		deploy.GetString(Key.Name).SetValue(name);
		deploy.GetString(Key.Description).SetValue(description);
	}

	protected void Deploy(ScHash programHash) {
		deploy.GetHash(Key.Hash).SetValue(programHash);
	}

	public ScMutableMap Params() {
		return deploy.GetMap(Key.Params);
	}
}
