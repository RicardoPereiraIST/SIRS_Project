import getpass

# Dictionary containing all users
# - Key: Username
# - Value: Password
users = {"test": "12345"}

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


if __name__ == "__main__":
	login()