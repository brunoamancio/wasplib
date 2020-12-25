// Copyright 2020 IOTA Stiftung
// SPDX-License-Identifier: Apache-2.0

package wasmlocalhost

import (
	"fmt"
	"github.com/iotaledger/wasp/packages/vm/wasmhost"
)

type HostExports struct {
	HostArray
}

func NewHostExports(host *SimpleWasmHost, keyId int32) *HostExports {
	return &HostExports{HostArray: *NewHostArray(host, keyId, wasmhost.OBJTYPE_STRING)}
}

func (a *HostExports) SetString(keyId int32, value string) {
	fmt.Printf("%s = %d\n", value, keyId)
	a.host.SetExport(keyId, value)
}