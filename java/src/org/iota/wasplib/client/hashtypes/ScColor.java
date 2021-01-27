// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package org.iota.wasplib.client.hashtypes;

import org.iota.wasplib.client.context.ScUtility;
import org.iota.wasplib.client.host.Host;
import org.iota.wasplib.client.keys.MapKey;

import java.util.Arrays;

public class ScColor implements MapKey {
	public static final ScColor IOTA = new ScColor(new byte[32]);
	public static final ScColor MINT = new ScColor(new byte[32]);

	final byte[] id = new byte[32];

	public ScColor(byte[] bytes) {
		if (bytes == null || bytes.length != id.length) {
			throw new RuntimeException("invalid color id length");
		}
		System.arraycopy(bytes, 0, id, 0, id.length);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ScColor other = (ScColor) o;
		return Arrays.equals(id, other.id);
	}

	@Override
	public int GetId() {
		return Host.GetKeyIdFromBytes(id);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(id);
	}

	public byte[] toBytes() {
		return id;
	}

	@Override

	public String toString() {
		return ScUtility.Base58String(id);
	}

	static {
		Arrays.fill(IOTA.id, (byte) 0x00);
		Arrays.fill(MINT.id, (byte) 0xff);
	}
}
