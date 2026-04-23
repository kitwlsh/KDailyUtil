
import base64

def decode_google_news_url(encoded_url):
    try:
        # Base64 padding
        padding = len(encoded_url) % 4
        if padding == 2:
            encoded_url += "=="
        elif padding == 3:
            encoded_url += "="
        
        # URL safe decode
        decoded_bytes = base64.urlsafe_b64decode(encoded_url)
        print(f"Decoded bytes (hex): {decoded_bytes.hex()}")
        
        # Protobuf Field 1 is usually 0x0a [len] [URL]
        if len(decoded_bytes) > 2 and decoded_bytes[0] == 0x0a:
            length = decoded_bytes[1]
            url = decoded_bytes[2:2+length].decode('utf-8', errors='ignore')
            print(f"Protobuf URL: {url}")
            return url
            
        # Fallback search
        import re
        match = re.search(rb'https?://[^\x00-\x1f\x7f-\xff]+', decoded_bytes)
        if match:
            url = match.group(0).decode('utf-8')
            print(f"Regex URL: {url}")
            return url
            
        return None
    except Exception as e:
        print(f"Error: {e}")
        return None

# From the user's log
encoded = "CBMingFBVV95cUxQY1BDSEFMWEt0NTRtazV6MzJfME5YLXlwTVpXNnQtamhPTHRtUFcxcFFPZDFLMkU1anpjQUY3dWVWNjhQNHRDbjBFSFVlOExaeWN2d3lDckl1REVSTHRfc1laUFBydFdCeHlMVnV1SHVlZ2sxUUQ4S3ktVDE2RDZGdXltWW40VFMzaVRyQzRMYWpwOEU4NVZQOFlyWjFmdw"
decode_google_news_url(encoded)
