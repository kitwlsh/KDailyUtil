
import base64

def decode_url_safe(data):
    padding = '=' * (4 - len(data) % 4)
    return base64.urlsafe_b64decode(data + padding)

# The ID extracted from the binary data
id_part = "AU_yqLPcPHALXKt54mk5z32_0NX-ypMZW6t-jhOLtmPW1pQOd1K2E5jzcAF7ueV68P4tCn0EHUe8LZycVwyCrIuDERLt_sYZPPrWByLVuuHuegk1QD8Ky-T16D6FuymYn4TS3iTrC4Lajp8E85VP8YrZ1fw"

try:
    decoded = decode_url_safe(id_part)
    print(f"Decoded ID bytes: {decoded.hex()}")
    print(f"Decoded ID string: {decoded.decode('latin-1', errors='ignore')}")
except Exception as e:
    print(f"Error decoding ID: {e}")
