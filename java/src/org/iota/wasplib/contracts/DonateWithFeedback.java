// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package org.iota.wasplib.contracts;

import org.iota.wasplib.client.bytes.BytesDecoder;
import org.iota.wasplib.client.bytes.BytesEncoder;
import org.iota.wasplib.client.context.ScAccount;
import org.iota.wasplib.client.context.ScCallContext;
import org.iota.wasplib.client.context.ScLog;
import org.iota.wasplib.client.context.ScRequest;
import org.iota.wasplib.client.exports.ScExports;
import org.iota.wasplib.client.hashtypes.ScAgent;
import org.iota.wasplib.client.hashtypes.ScColor;
import org.iota.wasplib.client.hashtypes.ScRequestId;
import org.iota.wasplib.client.mutable.ScMutableInt;
import org.iota.wasplib.client.mutable.ScMutableMap;

public class DonateWithFeedback {
	//export onLoad
	public static void onLoad() {
		ScExports exports = new ScExports();
		exports.AddCall("donate", DonateWithFeedback::donate);
		exports.AddCall("withdraw", DonateWithFeedback::withdraw);
	}

	public static void donate(ScCallContext sc) {
		ScLog tlog = sc.TimestampedLog("l");
		ScRequest request = sc.Request();
		DonationInfo donation = new DonationInfo();
		donation.seq = tlog.Length();
		donation.id = request.Id();
		donation.amount = request.Balance(ScColor.IOTA);
		donation.sender = request.Sender();
		donation.error = "";
		donation.feedback = request.Params().GetString("f").Value();
		if (donation.amount == 0 || donation.feedback.length() == 0) {
			donation.error = "error: empty feedback or donated amount = 0. The donated amount has been returned (if any)";
			if (donation.amount > 0) {
				sc.Transfer(donation.sender, ScColor.IOTA, donation.amount);
				donation.amount = 0;
			}
		}
		byte[] bytes = encodeDonationInfo(donation);
		tlog.Append(request.Timestamp(), bytes);

		ScMutableMap state = sc.State();
		ScMutableInt largestDonation = state.GetInt("maxd");
		ScMutableInt totalDonated = state.GetInt("total");
		if (donation.amount > largestDonation.Value()) {
			largestDonation.SetValue(donation.amount);
		}
		totalDonated.SetValue(totalDonated.Value() + donation.amount);
	}

	public static void withdraw(ScCallContext sc) {
		ScAgent scOwner = sc.Contract().Owner();
		ScRequest request = sc.Request();
		if (!request.From(scOwner)) {
			sc.Log("Cancel spoofed request");
			return;
		}

		ScAccount account = sc.Account();
		long amount = account.Balance(ScColor.IOTA);
		long withdrawAmount = request.Params().GetInt("s").Value();
		if (withdrawAmount == 0 || withdrawAmount > amount) {
			withdrawAmount = amount;
		}
		if (withdrawAmount == 0) {
			sc.Log("DonateWithFeedback: withdraw. nothing to withdraw");
			return;
		}

		sc.Transfer(scOwner, ScColor.IOTA, withdrawAmount);
	}

	public static void transferOwnership(ScCallContext sc) {
	}

	public static DonationInfo decodeDonationInfo(byte[] bytes) {
		BytesDecoder decoder = new BytesDecoder(bytes);
		DonationInfo bet = new DonationInfo();
		bet.seq = decoder.Int();
		bet.id = decoder.RequestId();
		bet.amount = decoder.Int();
		bet.sender = decoder.Agent();
		bet.error = decoder.String();
		bet.feedback = decoder.String();
		return bet;
	}

	public static byte[] encodeDonationInfo(DonationInfo donation) {
		return new BytesEncoder().
				Int(donation.seq).
				RequestId(donation.id).
				Int(donation.amount).
				Agent(donation.sender).
				String(donation.error).
				String(donation.feedback).
				Data();
	}

	public static class DonationInfo {
		long seq;
		ScRequestId id;
		long amount;
		ScAgent sender;
		String error;
		String feedback;
	}
}
