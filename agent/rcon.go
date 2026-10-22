package main

import (
	"encoding/binary"
	"errors"
	"io"
	"net"
	"time"
)

// Minimal Source RCON client (what Minecraft speaks on port 25575).
type rcon struct{ c net.Conn }

func rconDial(addr, pass string) (*rcon, error) {
	c, err := net.DialTimeout("tcp", addr, 2*time.Second)
	if err != nil {
		return nil, err
	}
	r := &rcon{c}
	if err := r.send(1, 3, pass); err != nil {
		c.Close()
		return nil, err
	}
	id, _, err := r.read()
	if err != nil {
		c.Close()
		return nil, err
	}
	if id == -1 {
		c.Close()
		return nil, errors.New("rcon: wrong password")
	}
	return r, nil
}

func (r *rcon) send(id, typ int32, body string) error {
	n := int32(4 + 4 + len(body) + 2)
	b := make([]byte, 4+n)
	binary.LittleEndian.PutUint32(b[0:], uint32(n))
	binary.LittleEndian.PutUint32(b[4:], uint32(id))
	binary.LittleEndian.PutUint32(b[8:], uint32(typ))
	copy(b[12:], body)
	r.c.SetDeadline(time.Now().Add(5 * time.Second))
	_, err := r.c.Write(b)
	return err
}

func (r *rcon) read() (int32, string, error) {
	var h [4]byte
	r.c.SetDeadline(time.Now().Add(5 * time.Second))
	if _, err := io.ReadFull(r.c, h[:]); err != nil {
		return 0, "", err
	}
	n := binary.LittleEndian.Uint32(h[:])
	if n < 10 || n > 1<<16 {
		return 0, "", errors.New("rcon: bad packet")
	}
	b := make([]byte, n)
	if _, err := io.ReadFull(r.c, b); err != nil {
		return 0, "", err
	}
	id := int32(binary.LittleEndian.Uint32(b[0:]))
	return id, string(b[8 : n-2]), nil
}

func (r *rcon) cmd(c string) (string, error) {
	if err := r.send(2, 2, c); err != nil {
		return "", err
	}
	_, s, err := r.read()
	return s, err
}

func (r *rcon) Close() { r.c.Close() }
