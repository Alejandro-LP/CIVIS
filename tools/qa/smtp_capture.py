"""Local SMTP fixture for delivery verification. Stores messages in an ignored directory."""
import argparse
import socketserver
from pathlib import Path
from uuid import uuid4


class SmtpHandler(socketserver.StreamRequestHandler):
    def send(self, text):
        self.wfile.write((text + "\r\n").encode("ascii"))
        self.wfile.flush()

    def handle(self):
        self.send("220 civis-test ESMTP")
        message = []
        in_data = False
        while line := self.rfile.readline(65536):
            if in_data:
                if line.rstrip(b"\r\n") == b".":
                    (self.server.output / f"{uuid4().hex}.eml").write_bytes(b"".join(message))
                    in_data = False
                    message = []
                    self.send("250 Message stored")
                else:
                    message.append(line[1:] if line.startswith(b"..") else line)
                continue
            command = line.decode("ascii", errors="replace").strip().upper()
            if command.startswith(("EHLO", "HELO")):
                self.send("250 civis-test")
            elif command == "DATA":
                in_data = True
                self.send("354 End data with a dot")
            elif command == "QUIT":
                self.send("221 Bye")
                return
            elif command.startswith(("MAIL FROM:", "RCPT TO:", "RSET", "NOOP")):
                self.send("250 OK")
            else:
                self.send("502 Command unsupported")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--port", type=int, default=11025)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    with socketserver.ThreadingTCPServer(("127.0.0.1", args.port), SmtpHandler) as server:
        server.output = args.output
        server.serve_forever()
