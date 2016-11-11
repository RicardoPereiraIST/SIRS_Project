import getpass
import socket
import json
import time
import Crypto
from Crypto.PublicKey import RSA
import ast
import sys

# Dictionary containing all users
# - Key: Username
# - Value: Password
users = {"test": "12345"}
keys = []

# Login code for a first step authentication
def login():
	logged = False
	while(logged == False):
		if sys.version_info[0] < 3:
			username = raw_input("Username: ")
		else:
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
	host = "192.168.1.134"
	port = 6000

	s.connect((host, port))
	s.send(createMessage()[0])
	s.close()

# Creates the json data to send
def createMessage():
	data_to_send = {}
	data_to_send["token"] = "token-placeholder"
	data_to_send["timestamp"] = time.strftime("%H:%M:%S")

	json_data = json.dumps(data_to_send)

	return rsaEncrypt(json_data)

def importKeys():
	f = open('private.key', 'r')
	s = f.read()
	keys.append(RSA.importKey(s))
	f.close()
	
	f = open ('public.key','r')
	s = f.read()
	keys.append(RSA.importKey(s))
	f.close()

def rsaEncrypt(message):
	encrypted = keys[1].encrypt(message, 32)
	return encrypted

def rsaDecrypt(message):
	decrypted = keys[0].decrypt(ast.literal_eval(str(message)))
	return decrypted

if __name__ == "__main__":
	importKeys()
	login()
	contactPhone()
