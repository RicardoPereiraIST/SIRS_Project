# Demonstration of the interaction with the mobile device
# - Acts like a server listening to incoming connections
# - Receives the json string from the server and prints it

import socket
import json

def listenToConnection():
	s = socket.socket()
	host = socket.gethostname()
	port = 500
	s.bind((host, port))

	s.listen(1)
	while(True):
		print ("Listening to connections...")
		client, addr = s.accept()
		print ("Connected"), addr
		print (client.recv(1024).decode())
		client.close()

if __name__ == "__main__":
	listenToConnection()