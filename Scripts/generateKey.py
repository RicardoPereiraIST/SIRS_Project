import Crypto
from Crypto.PublicKey import RSA
from Crypto import Random
import ast

def generateKey():
	random_generator = Random.new().read
	key = RSA.generate(1024, random_generator)
	publickey = key.publickey()
	f = open ('mobileprivate.key', 'w')
	f.write(str(key.exportKey()))
	f.close()

	f = open ('mobilepublic.key','w')
	f.write(str(publickey.exportKey()))
	f.close()

if __name__ == "__main__":
	generateKey()