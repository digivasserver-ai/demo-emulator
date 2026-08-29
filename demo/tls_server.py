#!/usr/bin/env python3
"""Demo-only TLS wrapper around the PR's multi-step Remote DroidGuard server.

The upstream server module (droidguard_multistep_server.py) is imported and
left untouched; this wrapper only adds an SSL listener so the clean Android
network-security model (no cleartext to arbitrary IPs) is respected.
"""
import argparse
import logging
import ssl

from droidguard_multistep_server import (
    RemoteDroidGuardServer,
    build_backend,
    make_server,
)


def main():
    parser = argparse.ArgumentParser(description="TLS wrapper for Remote DroidGuard server")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8443)
    parser.add_argument("--cert", default="server.crt")
    parser.add_argument("--key", default="server.key")
    parser.add_argument("--session-timeout", type=float, default=60.0)
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )

    backend = build_backend(args)
    dg = RemoteDroidGuardServer(backend, token=None, timeout_s=args.session_timeout)
    server = make_server(dg, args.host, args.port)

    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(args.cert, args.key)
    server.socket = ctx.wrap_socket(server.socket, server_side=True)

    logging.getLogger("droidguard").info(
        "TLS Remote DroidGuard server listening on %s:%s (backend=%s)",
        args.host, args.port, backend.name,
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()


if __name__ == "__main__":
    main()