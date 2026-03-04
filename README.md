This is a network aplication for android that will mask all network traffic from an android phone and then all of this traffic will be constructed back once it reaches a server. 
 # MaskedVPN - Android Traffic Obfuscation Tool
​
 **MaskedVPN** is a research-oriented Android application designed to bypass network filters and Deep Packet Inspection (DPI) that block standard VPN handshakes. It uses the Android `VpnService` API to intercept all device traffic and "mask" it using custom bit manipulation before transmission.
​
 ## 🚀 Purpose
 Standard VPN protocols (OpenVPN, WireGuard) have distinct handshakes that are easily detected and blocked by restrictive networks. This application demonstrates how to:
 1. Intercept raw IP packets at the system level.
 2. Apply a custom obfuscation layer (XOR masking).
 3. Tunnel the masked data via UDP to a remote endpoint, making it appear as "random" noise to network monitors.
​
 ## ✨ Features
 *   **Full System Tunneling**: Captures all outgoing traffic from the device.
 *   **DPI Evasion**: Simple XOR-based bit-flipping to hide protocol signatures.
 *   **Clean UI**: Simple Start/Stop controls.
 *   **Efficient Handling**: Uses a dedicated background thread for packet processing.
 *   **Socket Protection**: Uses `VpnService.protect()` to prevent routing loops.
​
 ## 🛠 How it Works
 1.  **TUN Interface**: The app creates a virtual network interface (10.0.0.2).
 2.  **Packet Capture**: Raw IPv4 packets are read from a `ParcelFileDescriptor`.
 3.  **Masking**: Each byte of the packet is XORed with a secret key (`0x42`).
 4.  **Transport**: The masked packet is encapsulated in a UDP datagram and sent to a pre-configured server IP.
 5.  **De-masking**: A server-side script reverses the XOR operation to recover the original IP packet.
​
 ## 📦 Getting Started
​
 ### Prerequisites
 *   Android Studio Jellyfish or newer.
 *   A server or PC with a public IP address (for testing).
 *   Python 3.x installed on the testing server.
​
 ### Configuration
 1.  Open `app/src/main/java/com/example/networkaplication_android/MyVpnService.java`.
 2.  Replace `YOUR_PUBLIC_IP_HERE` with your server's IP address.
 3.  Build and run the app on your Android device.
​
 ### Server-Side Testing
 Run the following Python script on your server to verify that packets are being received and successfully unmasked:
​
 ```python
 import socket
​
 UDP_IP = "0.0.0.0"
 UDP_PORT = 9999
 MASK_KEY = 0x42
​
 sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
 sock.bind((UDP_IP, UDP_PORT))
​
 print(f"Listening for masked VPN packets on port {UDP_PORT}...")
​
 while True:
     data, addr = sock.recvfrom(32767)
     # UNMASK the data
     unmasked = bytearray([b ^ MASK_KEY for b in data])
     
     # Check for IPv4 header (0x45)
     if len(unmasked) > 0 and unmasked[0] == 0x45:
         print(f"SUCCESS: Decoded IP packet from {addr}")
     else:
         print(f"Received data, but could not decode IP header.")
 ```
​
 ## ⚠️ Disclaimer
 This project is for **educational and research purposes only**. 
 *   This is a Proof of Concept (PoC) and does not provide production-grade encryption.
 *   Simple XOR masking can still be detected by advanced statistical analysis.
 *   Bypassing network restrictions may violate local laws or terms of service. Use responsibly.
​
 ## 📄 License
 Appache 2.0 
