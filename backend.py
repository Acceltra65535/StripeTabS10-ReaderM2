import http.server
import socketserver
import urllib.request
import urllib.parse
import json
import base64
import os

PORT = 3000
# REPLACE THIS WITH YOUR ACTUAL STRIPE SECRET KEY (sk_test_...)
STRIPE_SECRET_KEY = os.environ.get('STRIPE_SECRET_KEY', 'sk_test_YOUR_STRIPE_SECRET_KEY_HERE')

class ConnectionTokenHandler(http.server.SimpleHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/connection_token':
            try:
                # Stripe API URL for connection tokens
                url = 'https://api.stripe.com/v1/terminal/connection_tokens'
                
                # Setup Basic Auth with the Secret Key
                auth_str = STRIPE_SECRET_KEY + ':'
                auth_b64 = base64.b64encode(auth_str.encode('utf-8')).decode('utf-8')
                
                req = urllib.request.Request(url, data=b'')
                req.add_header('Authorization', f'Basic {auth_b64}')
                
                # Execute HTTP POST to Stripe
                with urllib.request.urlopen(req) as response:
                    res_body = response.read()
                    
                    # Return success response to Android app
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(res_body)
            except urllib.error.URLError as e:
                self.send_response(500)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                error_msg = {"error": str(e)}
                self.wfile.write(json.dumps(error_msg).encode('utf-8'))
        elif self.path == '/create_payment_intent':
            try:
                # Stripe API URL for payment intents
                url = 'https://api.stripe.com/v1/payment_intents'
                
                # Setup Basic Auth with the Secret Key
                auth_str = STRIPE_SECRET_KEY + ':'
                auth_b64 = base64.b64encode(auth_str.encode('utf-8')).decode('utf-8')
                
                # PaymentIntent parameters
                data = urllib.parse.urlencode({
                    'amount': 100,
                    'currency': 'sgd',
                    'payment_method_types[]': 'card_present'
                }).encode('utf-8')

                req = urllib.request.Request(url, data=data)
                req.add_header('Authorization', f'Basic {auth_b64}')
                
                # Execute HTTP POST to Stripe
                with urllib.request.urlopen(req) as response:
                    res_body = response.read()
                    
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(res_body)
            except urllib.error.URLError as e:
                self.send_response(500)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                error_msg = {"error": str(e)}
                self.wfile.write(json.dumps(error_msg).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

class MyTCPServer(socketserver.TCPServer):
    allow_reuse_address = True

with MyTCPServer(("", PORT), ConnectionTokenHandler) as httpd:
    print(f"Backend Server running on port {PORT}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server.")
        httpd.server_close()
