import getpass
import socket
import json
import time

# Dictionary containing all users
# - Key: Username
# - Value: Password
users = {"test": "12345"}

# Login code for a first step authentication
def login():
	logged = False
	while(logged == False):
		username = input("Username: ")
		password = getpass.getpass("Password: ")

		if username in users:
			if password == users[username]:
				print("Logged in")
				logged = True
			else:
				print("Wrong credentials")
		else:
			print("Wrong credentials")

# Contacts the "phone" sending a json string containing 
# the timestamp and a placeholder for a token
def contactPhone():
	s = socket.socket()
	host = "192.168.1.127"
	port = 6000

	s.connect((host, port))
	s.send(createMessage().encode())
	s.close()

# Creates the json data to send
def createMessage():
	data_to_send = {}
	data_to_send["token"] = "token-placeholder"
	data_to_send["timestamp"] = time.strftime("%H:%M:%S")

	json_data = json.dumps(data_to_send)

	return json_data

if __name__ == "__main__":
	login()
	contactPhone()
