#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independent wire-only calculator for the proposed body-hash profile.

Uses Python's hashlib, not an SDK encoder or SDK hash accessor. Fixture bytes
are inputs; this script never rewrites them or their expected digests. It checks
the fixture envelope/identifier shapes, not every protobuf schema or encoding.
"""
import hashlib
import json
from pathlib import Path


def varint(data, pos, max_bytes=10):
    value = 0
    for shift in range(0, 7 * max_bytes, 7):
        if pos >= len(data):
            raise ValueError('truncated varint')
        byte = data[pos]
        pos += 1
        if shift == 63 and byte > 1:
            raise ValueError('overflow varint')
        value |= (byte & 127) << shift
        if byte < 128:
            return value, pos
    raise ValueError('overflow varint')


def fields(data):
    result, pos = [], 0
    while pos < len(data):
        start = pos
        tag, pos = varint(data, pos, 5)
        number, wire = tag >> 3, tag & 7
        if not 0 < number < (1 << 29):
            raise ValueError('invalid field number')
        if wire == 0:
            payload, pos = varint(data, pos)
        elif wire in (1, 5):
            end = pos + (8 if wire == 1 else 4)
            payload, pos = data[pos:end], end
        elif wire == 2:
            size, pos = varint(data, pos, 5)
            end = pos + size
            payload, pos = data[pos:end], end
        else:
            raise ValueError('unsupported wire type')
        if pos > len(data):
            raise ValueError('truncated field')
        result.append((number, wire, payload, data[start:pos]))
    return result


def one(records, number, wire=2):
    found = [r for r in records if r[0] == number]
    if len(found) != 1 or found[0][1] != wire:
        raise ValueError('missing, repeated or mistyped field')
    return found[0][2]


def numeric_id(data):
    decoded = {n: (w, p) for n, w, p, _ in fields(data)}
    return tuple(decoded.get(n, (0, 0)) for n in (1, 2, 3, 4))


def transaction_id(data):
    records = fields(data)
    timestamp = numeric_id(one(records, 1))
    account = numeric_id(one(records, 2))
    rest = {n: (w, p) for n, w, p, _ in records if n in (3, 4)}
    return timestamp, account, rest.get(3, (0, 0)), rest.get(4, (0, 0))


def sha(data):
    return hashlib.sha384(data).digest()


def entry_hash(body):
    records = fields(body)
    if not body:
        raise ValueError('empty body')
    batches = [r for r in records if r[0] == 74]
    if not batches:
        return sha(body)
    batch = one(records, 74)
    # Known operation fields exercised in these fixtures. SDKs validate all
    # current schema operation tags using their normal transaction decoder.
    if any(n in (14, 16) for n, _, _, _ in records):
        raise ValueError('conflicting transaction operations')
    outer = b''.join(raw for n, _, _, raw in records if n != 74)
    parts = [sha(outer)]
    children = fields(batch)
    if not children or any(n != 1 or w != 2 for n, w, _, _ in children):
        raise ValueError('empty or unsupported batch wrapper')
    for _, _, signed, _ in children:
        child = one(fields(signed), 1)
        child_fields = fields(child)
        if not child or any(n == 74 for n, _, _, _ in child_fields):
            raise ValueError('empty or nested child body')
        parts.append(sha(child))
    return sha(b''.join(parts))


def calculate(serialized):
    transactions = fields(serialized)
    if not transactions:
        raise ValueError('empty transaction list')
    chunks, first_nodes = {}, []
    for n, w, transaction, _ in transactions:
        if n != 1 or w != 2:
            raise ValueError('invalid transaction list')
        envelope = fields(transaction)
        signed = one(envelope, 5)
        body = one(fields(signed), 1)
        records = fields(body)
        txid = transaction_id(one(records, 1))
        node = numeric_id(one(records, 2))
        if txid not in chunks:
            chunks[txid] = {}
        chunk = chunks[txid]
        if node in chunk:
            raise ValueError('duplicate entry')
        if txid == next(iter(chunks)):
            first_nodes.append(node)
        chunk[node] = body
    parts = []
    for chunk in chunks.values():
        if set(chunk) != set(first_nodes):
            raise ValueError('inconsistent node set')
        parts.extend(entry_hash(chunk[node]) for node in first_nodes)
    return sha(b''.join(parts)).hex()


def main():
    successes, rejections = 0, 0
    for name in ('transaction-body-hash-vectors.json', 'transaction-body-hash-identifier-vectors.json'):
        data = json.loads(Path(__file__).with_name(name).read_text())
        for vector in data['vectors']:
            try:
                actual = calculate(bytes.fromhex(vector['transactionListHex']))
            except ValueError:
                assert 'expectedError' in vector, vector['name']
                rejections += 1
            else:
                assert 'expectedHashHex' in vector, vector['name']
                assert actual == vector['expectedHashHex'], (vector['name'], actual)
                successes += 1
    print(f'{successes} expected SHA-384 digests and {rejections} rejection vectors verified independently')


if __name__ == '__main__':
    main()
