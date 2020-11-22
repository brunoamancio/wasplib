// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package org.iota.wasplib.contracts;

import org.iota.wasplib.client.bytes.BytesDecoder;
import org.iota.wasplib.client.bytes.BytesEncoder;
import org.iota.wasplib.client.context.ScCallContext;
import org.iota.wasplib.client.context.ScRequest;
import org.iota.wasplib.client.exports.ScExports;
import org.iota.wasplib.client.hashtypes.ScAgent;
import org.iota.wasplib.client.hashtypes.ScColor;
import org.iota.wasplib.client.immutable.ScImmutableMap;
import org.iota.wasplib.client.mutable.ScMutableBytes;
import org.iota.wasplib.client.mutable.ScMutableColorArray;
import org.iota.wasplib.client.mutable.ScMutableMap;

public class TokenRegistry {
	//export onLoad
	public static void onLoad() {
		ScExports exports = new ScExports();
		exports.AddCall("mintSupply", TokenRegistry::mintSupply);
		exports.AddCall("updateMetadata", TokenRegistry::updateMetadata);
		exports.AddCall("transferOwnership", TokenRegistry::transferOwnership);
	}

	public static void mintSupply(ScCallContext sc) {
		ScRequest request = sc.Request();
		ScColor color = request.MintedColor();
		ScMutableMap state = sc.State();
		ScMutableBytes registry = state.GetKeyMap("registry").GetBytes(color.toBytes());
		if (registry.Exists()) {
			sc.Log("TokenRegistry: Color already exists");
			return;
		}
		ScImmutableMap params = request.Params();
		TokenInfo token = new TokenInfo();
		token.supply = request.Balance(color);
		token.mintedBy = request.Sender();
		token.owner = request.Sender();
		token.created = request.Timestamp();
		token.updated = request.Timestamp();
		token.description = params.GetString("dscr").Value();
		token.userDefined = params.GetString("ud").Value();
		if (token.supply <= 0) {
			sc.Log("TokenRegistry: Insufficient supply");
			return;
		}
		if (token.description.isEmpty()) {
			token.description += "no dscr";
		}
		byte[] bytes = encodeTokenInfo(token);
		registry.SetValue(bytes);
		ScMutableColorArray colors = state.GetColorArray("lc");
		colors.GetColor(colors.Length()).SetValue(color);
	}

	public static void updateMetadata(ScCallContext sc) {
		//TODO
	}

	public static void transferOwnership(ScCallContext sc) {
		//TODO
	}

	public static TokenInfo decodeTokenInfo(byte[] bytes) {
		BytesDecoder decoder = new BytesDecoder(bytes);
		TokenInfo token = new TokenInfo();
		token.supply = decoder.Int();
		token.mintedBy = decoder.Agent();
		token.owner = decoder.Agent();
		token.created = decoder.Int();
		token.updated = decoder.Int();
		token.description = decoder.String();
		token.userDefined = decoder.String();
		return token;
	}

	public static byte[] encodeTokenInfo(TokenInfo token) {
		return new BytesEncoder().
				Int(token.supply).
				Agent(token.mintedBy).
				Agent(token.owner).
				Int(token.created).
				Int(token.updated).
				String(token.description).
				String(token.userDefined).
				Data();
	}

	public static class TokenInfo {
		long supply;
		ScAgent mintedBy;
		ScAgent owner;
		long created;
		long updated;
		String description;
		String userDefined;
	}
}
