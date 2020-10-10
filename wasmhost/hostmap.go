package wasmhost

type HostMap struct {
	host      *SimpleWasmHost
	fields    map[int32]interface{}
	immutable bool
	types     map[int32]int32
}

func NewHostMap(host *SimpleWasmHost) *HostMap {
	return &HostMap{
		host:   host,
		fields: make(map[int32]interface{}),
		types:  make(map[int32]int32),
	}
}

func (m *HostMap) GetBytes(keyId int32) []byte {
	if !m.valid(keyId, OBJTYPE_BYTES) {
		return []byte(nil)
	}
	value, ok := m.fields[keyId]
	if !ok {
		return []byte(nil)
	}
	return value.([]byte)
}

func (m *HostMap) GetInt(keyId int32) int64 {
	switch keyId {
	case KeyLength:
		return int64(len(m.fields))
	}

	if !m.valid(keyId, OBJTYPE_INT) {
		return 0
	}

	value, ok := m.fields[keyId]
	if !ok {
		return 0
	}
	return value.(int64)
}

func (m *HostMap) GetLength() int32 {
	return int32(len(m.fields))
}

func (m *HostMap) GetObjectId(keyId int32, typeId int32) int32 {
	if !m.valid(keyId, typeId) {
		return 0
	}
	value, ok := m.fields[keyId]
	if ok {
		return value.(int32)
	}

	var o HostObject
	switch typeId {
	case OBJTYPE_BYTES_ARRAY:
		o = NewHostArray(m.host, OBJTYPE_BYTES)
	case OBJTYPE_INT_ARRAY:
		o = NewHostArray(m.host, OBJTYPE_INT)
	case OBJTYPE_MAP:
		o = NewHostMap(m.host)
	case OBJTYPE_MAP_ARRAY:
		o = NewHostArray(m.host, OBJTYPE_MAP)
	case OBJTYPE_STRING_ARRAY:
		if keyId == m.host.ExportsId {
			o = NewHostExports(m.host)
			break
		}
		o = NewHostArray(m.host, OBJTYPE_STRING)
	default:
		m.host.SetError("Map.GetObjectId: Invalid type id")
		return 0
	}
	objId := m.host.TrackObject(o)
	m.fields[keyId] = objId
	return objId
}

func (m *HostMap) GetString(keyId int32) string {
	if !m.valid(keyId, OBJTYPE_STRING) {
		return ""
	}
	value, ok := m.fields[keyId]
	if !ok {
		return ""
	}
	return value.(string)
}

func (m *HostMap) SetBytes(keyId int32, value []byte) {
	if EnableImmutableChecks && m.immutable {
		m.host.SetError("Map.SetBytes: Immutable")
		return
	}
	if !m.valid(keyId, OBJTYPE_BYTES) {
		return
	}
	m.fields[keyId] = value
}

func (m *HostMap) SetInt(keyId int32, value int64) {
	if EnableImmutableChecks && m.immutable {
		m.host.SetError("Map.SetInt: Immutable")
		return
	}
	if keyId == KeyLength {
		for k, v := range m.types {
			switch v {
			case OBJTYPE_MAP,
				OBJTYPE_BYTES_ARRAY,
				OBJTYPE_INT_ARRAY,
				OBJTYPE_STRING_ARRAY:
				// tell object to clear itself
				m.host.SetInt(m.fields[k].(int32), keyId, 0)
				//TODO move to pool for reuse of transfers
			}
		}
		m.fields = make(map[int32]interface{})
		return
	}
	if !m.valid(keyId, OBJTYPE_INT) {
		return
	}
	m.fields[keyId] = value
}

func (m *HostMap) SetString(keyId int32, value string) {
	if EnableImmutableChecks && m.immutable {
		m.host.SetError("Map.SetString: Immutable")
		return
	}
	if !m.valid(keyId, OBJTYPE_STRING) {
		return
	}
	m.fields[keyId] = value
}

func (m *HostMap) valid(keyId int32, typeId int32) bool {
	fieldType, ok := m.types[keyId]
	if !ok {
		if EnableImmutableChecks && m.immutable {
			m.host.SetError("Map.valid: Immutable")
			return false
		}
		m.types[keyId] = typeId
		return true
	}
	if fieldType != typeId {
		m.host.SetError("Map.valid: Invalid typeId")
		return false
	}
	return true
}

func (m *HostMap) CopyDataTo(other HostObject) {
	for k, v := range m.fields {
		switch m.types[k] {
		case OBJTYPE_BYTES:
			other.SetBytes(k, v.([]byte))
		case OBJTYPE_INT:
			other.SetInt(k, v.(int64))
		case OBJTYPE_STRING:
			other.SetString(k, v.(string))
		default:
			panic("Implement types")
		}
	}
}
