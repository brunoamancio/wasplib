package org.iota.wasplib.client.immutable;

import org.iota.wasplib.client.Host;

public class ScImmutableString {
	int objId;
	int keyId;

	public ScImmutableString(int objId, int keyId) {
		this.objId = objId;
		this.keyId = keyId;
	}

	public String Value() {
		return Host.GetString(objId, keyId);
	}
}
