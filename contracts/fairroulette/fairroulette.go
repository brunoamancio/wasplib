// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package fairroulette

import "github.com/iotaledger/wasplib/client"

const keyBets = client.Key("bets")
const keyColor = client.Key("color")
const keyLastWinningColor = client.Key("last_winning_color")
const keyLockedBets = client.Key("locked_bets")
const keyPlayPeriod = client.Key("play_period")

const numColors = 5
const defaultPlayPeriod = 120

func OnLoad() {
	exports := client.NewScExports()
	exports.AddCall("place_bet", placeBet)
	exports.AddCall("lock_bets", lockBets)
	exports.AddCall("pay_winners", payWinners)
	exports.AddCall("play_period", playPeriod)
	exports.AddCall("nothing", client.Nothing)
}

func placeBet(sc *client.ScCallContext) {
	amount := sc.Incoming().Balance(client.IOTA)
	if amount == 0 {
		sc.Panic("Empty bet...")
	}
	color := sc.Params().GetInt(keyColor).Value()
	if color == 0 {
		sc.Panic("No color...")
	}
	if color < 1 || color > numColors {
		sc.Panic("Invalid color...")
	}

	bet := &BetInfo{
		better: sc.Caller(),
		amount: amount,
		color:  color,
	}

	state := sc.State()
	bets := state.GetBytesArray(keyBets)
	betNr := bets.Length()
	bets.GetBytes(betNr).SetValue(encodeBetInfo(bet))
	if betNr == 0 {
		playPeriod := state.GetInt(keyPlayPeriod).Value()
		if playPeriod < 10 {
			playPeriod = defaultPlayPeriod
		}
		sc.Post("lock_bets").Post(playPeriod)
	}
}

func lockBets(sc *client.ScCallContext) {
	// can only be sent by SC itself
	if !sc.From(sc.Contract().Id()) {
		sc.Panic("Cancel spoofed request")
	}

	// move all current bets to the locked_bets array
	state := sc.State()
	bets := state.GetBytesArray(keyBets)
	lockedBets := state.GetBytesArray(keyLockedBets)
	nrBets := bets.Length()
	for i := int32(0); i < nrBets; i++ {
		bytes := bets.GetBytes(i).Value()
		lockedBets.GetBytes(i).SetValue(bytes)
	}
	bets.Clear()

	sc.Post("pay_winners").Post(0)
}

func payWinners(sc *client.ScCallContext) {
	// can only be sent by SC itself
	scId := sc.Contract().Id()
	if !sc.From(scId) {
		sc.Panic("Cancel spoofed request")
	}

	winningColor := sc.Utility().Random(5) + 1
	state := sc.State()
	state.GetInt(keyLastWinningColor).SetValue(winningColor)

	// gather all winners and calculate some totals
	totalBetAmount := int64(0)
	totalWinAmount := int64(0)
	lockedBets := state.GetBytesArray(keyLockedBets)
	winners := make([]*BetInfo, 0)
	nrBets := lockedBets.Length()
	for i := int32(0); i < nrBets; i++ {
		bet := decodeBetInfo(lockedBets.GetBytes(i).Value())
		totalBetAmount += bet.amount
		if bet.color == winningColor {
			totalWinAmount += bet.amount
			winners = append(winners, bet)
		}
	}
	lockedBets.Clear()

	if len(winners) == 0 {
		sc.Log("Nobody wins!")
		// compact separate UTXOs into a single one
		sc.Transfer(scId, client.IOTA, totalBetAmount)
		return
	}

	// pay out the winners proportionally to their bet amount
	totalPayout := int64(0)
	size := len(winners)
	for i := 0; i < size; i++ {
		bet := winners[i]
		payout := totalBetAmount * bet.amount / totalWinAmount
		if payout != 0 {
			totalPayout += payout
			sc.Transfer(bet.better, client.IOTA, payout)
		}
		text := "Pay " + sc.Utility().String(payout) + " to " + bet.better.String()
		sc.Log(text)
	}

	// any truncation left-overs are fair picking for the smart contract
	if totalPayout != totalBetAmount {
		remainder := totalBetAmount - totalPayout
		text := "Remainder is " + sc.Utility().String(remainder)
		sc.Log(text)
		sc.Transfer(scId, client.IOTA, remainder)
	}
}

func playPeriod(sc *client.ScCallContext) {
	// can only be sent by SC creator
	if !sc.From(sc.Contract().Creator()) {
		sc.Panic("Cancel spoofed request")
	}

	playPeriod := sc.Params().GetInt(keyPlayPeriod).Value()
	if playPeriod < 10 {
		sc.Panic("Invalid play period...")
	}

	sc.State().GetInt(keyPlayPeriod).SetValue(playPeriod)
}
