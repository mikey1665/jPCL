package com.PenguinClientLibrary.JPCL;

/*
	PenguinClientLibrary - JPCL - Cucumber.java - Base Connector
	Copyright (C) 2012 PenguinClientLibrary.com 
	View license in LICENSE file.

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import anto.dev.Bot1;

public class Cucumber2 implements Runnable {

	private boolean debug_enabled, notifications_enabled, store_players, is_connected = false;
	private String last_sent = "", recv_bufferLine = "";
	private int myPlayerID;
	
	private ArrayList<String> recv_buffer = new ArrayList<String>();
	public HashMap<Integer, Player> players = new HashMap<Integer, Player>();
	
	
	// Socket and read/write stream variables
	private Thread t;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private String hostIp = "server.cprewritten.net";
	private int packetNumber = 99999;
	
	//For Following players
	private String followPlayer;
	private boolean follow;
	private int xOffset;
	private int yOffset;

	public Cucumber2() {
		this.debug_enabled = false;
		this.notifications_enabled = true;
		this.store_players = true;
	}

	public Cucumber2(boolean notifications, boolean debug, boolean players) {
		this.notifications_enabled = notifications;
		this.debug_enabled = debug;
		this.store_players = players;
	}

	//// General Methods ////

	private String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).subSequence(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}

	public void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String stribet(String input, String left, String right) {
		int pos_left = input.indexOf(left) + left.length();
		int pos_right = input.indexOf(right);
		return input.substring(pos_left, pos_right);
	}

	//// Output Methods ////

	public void debug(String message) {
		if (this.debug_enabled) {
			System.out.println(this.now() + message);
		}
	}

	public void notification(String message) {
		if (this.notifications_enabled) {
			System.out.println(this.now() + message);
		}
	}

	public String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
		return "[" + sdf.format(cal.getTime()) + "] ";
	}

	//// Data Stream Methods ////

	public void run() {
		while (is_connected) {
			if (socket.isClosed() == false) {
				recvPacket();
			}
		}
	}
	
	public void sendPacket(String data) {
		if(data.equals(last_sent)){
			sleep(2);
		}
		
		out.print(data + "\0");
		out.flush();
		
		last_sent = data;
		notification("Sent: " + data);
	}

	public String recvPacket() {
		try {
			int dec = this.in.read();
			String data = "";

			while (dec != 0) {
				data += (char) dec;
				dec = in.read();
			}
			handlePacket(data);
			recv_buffer.add(data);
			if(recv_buffer.size() > 10){
				recv_buffer.remove(0);
			}
			
			recv_bufferLine = data;
			notification("Recv: " + data);
			return data;
		} catch (IOException e) {
		}
		return null;
	}

	public String getLastSent() {
		return this.last_sent;
	}

	//// Server Methods ////

	private String swappedMD5(String password) {
		String hash = MD5(password);
		hash = hash.substring(16, 32) + hash.substring(0, 16);
		return hash;
	}

	public boolean isConnected() {
		return this.is_connected;
	}

	// Connection handling

	public boolean connect(String username, String password, int server) {
		String loginKey, randKey, data = null;

		try {		
			socket = new Socket(hostIp, 6112);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			is_connected = true;
			t = new Thread(this);
			t.start();
			sleep(3);
			
			notification("Connecting to login server at " + socket.getInetAddress().getHostName() + ":" + socket.getPort());

			// Version Check
			notification("Sending version check...");
			sendPacket("<msg t='sys'><body action='verChk' r='0'><ver v='153' /></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("apiOK")) {
				notification("Version check... PASS");
			} else {
				notification("Version check... FAILED");
			}
			
			//rndk
			notification("Sending rndK check...");
			sendPacket("<msg t='sys'><body action='rndK' r='-1'></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("rndK")) {
				notification("rndK check... PASS");
				data = recv_bufferLine;
			} else {
				notification("rndK check... FAILED");
			}
			randKey = stribet(data, "<k>", "</k>");
			
			//Password and MD5
			notification("Sending login packet...");
			String magic = "Y(02.>'H}t\":E1";
			String passwordHash = swappedMD5(swappedMD5(password).toUpperCase() + randKey + magic);
			sendPacket("<msg t='sys'><body action='login' r='0'><login z='w1'><nick><![CDATA[" + username + "]]></nick><pword><![CDATA[" + passwordHash + "]]></pword></login></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("%l%-1%")) {
				notification("Login packet... PASS");
				notification("Sending login packet...");
				data = recv_bufferLine;
			} else {
				notification("Login packet... FAILED");
			}

			socket.close();
			in.close();
			out.close();
			
			if(data.indexOf("%e%-1%") != -1){
				is_connected = false;
				return false;
			}
			System.out.println("Successfully logged into " + username);
			
			String[] packet = data.split("%");
			myPlayerID = Integer.parseInt(packet[4]);
			loginKey = packet[5];
			Player p = new Player(myPlayerID, username);
			players.put(myPlayerID, p);
			
			try{
				socket = new Socket(hostIp, Bot1.serverRoom);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			notification("Connecting to server at " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
			
			// Version Check
			notification("Sending version check...");
			sendPacket("<msg t='sys'><body action='verChk' r='0'><ver v='153' /></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("apiOK")) {
				notification("Version check... PASS");
			} else {
				notification("Version check... FAILED");
			}
			
			//rndk
			notification("Sending rndK check...");
			sendPacket("<msg t='sys'><body action='rndK' r='-1'></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("rndK")) {
				notification("rndK check... PASS");
				data = recv_bufferLine;
			} else {
				notification("rndK check... FAILED");
			}
			randKey = stribet(data, "<k>", "</k>");

			//Password and MD5
			notification("Sending login packet...");
			String passwordLoginHash = swappedMD5(loginKey + randKey) + loginKey;
			sendPacket("<msg t='sys'><body action='login' r='0'><login z='w1'><nick><![CDATA[" + username + "]]></nick><pword><![CDATA[" + passwordLoginHash + "]]></pword></login></body></msg>");
			sleep(1);
			if(recv_bufferLine.contains("%l%-1%")) {
				notification("Join server packet... PASS");
				data = recv_bufferLine;
			} else {
				notification("join server packet... FAILED");
			}
			
			int currentPacket = (packetNumber + 769567) ^ 942215; //205881
			packetNumber += 1;
			
			sendPacket("%xt%s%j#js%" + currentPacket + "%-1%"+ myPlayerID +"%" + loginKey + "%en%");
			sleep(1);
			notification("Successfully connected to " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	//// Player Methods ////

	public int myPlayerID() {
		return this.myPlayerID;
	}

	public void purgePlayers() {
		Player p = this.players.get(this.myPlayerID);
		this.players.clear();
		players.put(this.myPlayerID, p);
	}

	public Player getPlayerByID(int user_id) {
		if (this.players.containsKey(user_id)) {
			return this.players.get(user_id);
		}
		return null;
	}

	public Player getPlayerByName(String name) throws InterruptedException {
		String response;
		for (Player p : this.players.values()) {
			if (p.getNickname().equals(name)) {
				return p;
			}
		}
		sendPacket("%xt%s%u#pbn%-1%" + name + "%");
		response = recvPacket();
		String[] data = response.split("%");
		return new Player(Integer.parseInt(data[5]), data[6]);
	}

	//Working 2020
	public void joinRoom(int room_id, int x, int y) {
		if (room_id != this.players.get(this.myPlayerID).getExtRoomID() && room_id < 999) {
			sendPacket("%xt%s%j#jr%" + myPlayerID + "%" + players.get(myPlayerID).getIntRoomID() + "%" + room_id + "%" + x + "%" + y + "%");
		}
	}
	
	public void joinRoom(int room_id) {
		if (room_id != players.get(myPlayerID).getExtRoomID() && room_id < 999) {
			sendPacket("%xt%s%j#jr%" + myPlayerID + "%" + players.get(myPlayerID).getIntRoomID() + "%" + room_id + "%0%0%");
		}
	}

	public void joinIgloo(int penguin_id) {
		if (this.players.get(this.myPlayerID).getExtRoomID() != penguin_id + 2000) {
			this.sendPacket("%xt%s%g#gm%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + penguin_id + "%");
			this.sendPacket("%xt%s%p#pg%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + penguin_id + "%");
			this.sendPacket("%xt%s%j#jp%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + (penguin_id + 1000) + "%");
		}
	}

	public void sendMessage(String message) {
		int currentPacket = (packetNumber + 769567) ^ 942215;
		packetNumber += 1;
		sendPacket("%xt%s%m#sm%" + currentPacket + "%" + players.get(myPlayerID).getIntRoomID() + "%" + myPlayerID + "%" + message + "%");
	}

	public void sendPhraseChat(String message_id) {
		this.sendPacket("%xt%s%m#sc%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + this.myPlayerID + "%" + message_id + "%");
	}

	public void sendPosition(int x, int y) {
		if (x != 0 && y != 0) {
			int currentPacket = (packetNumber + 769567) ^ 942215;
			packetNumber += 1;
			this.sendPacket("%xt%s%u#sp%" + currentPacket + "%" + players.get(myPlayerID).getIntRoomID() + "%" + x + "%" + y + "%");
		}
	}

	public void sendSafe(int message_id) {
		this.sendPacket("%xt%s%u#ss%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + message_id + "%");
	}

	public void sendLine(int message_id) {
		this.sendPacket("%xt%s%u#sl%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + message_id + "%");
	}

	public void sendQuick(int message_id) {
		this.sendPacket("%xt%s%u#sq%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + message_id + "%");
	}

	public void sendGuide(int room_id) {
		this.sendPacket("%xt%s%u#sg%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + room_id + "%");
	}

	public void sendJoke(int message_id) {
		int currentPacket = (packetNumber + 769567) ^ 942215;
		packetNumber += 1;
		sendPacket("%xt%s%u#sj%" + currentPacket + "%" + myPlayerID + "%" + message_id + "%");
	}

	public void sendEmote(int emote_id) {
		int currentPacket = (packetNumber + 769567) ^ 942215;
		packetNumber += 1;
		sendPacket("%xt%s%u#se%" + currentPacket + "%" + players.get(myPlayerID).getIntRoomID() + "%" + emote_id + "%");
	}

	public void snowball(int x, int y) {
		this.sendPacket("%xt%s%u#sb%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + x + "%" + y + "%");
	}

	public void sendAction(int id) {
		this.sendPacket("%xt%s%u#sa%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void sendFrame(int id) {
		int currentPacket = (packetNumber + 769567) ^ 942215;
		packetNumber += 1;
		this.sendPacket("%xt%s%u#sf%" + currentPacket + "%" + players.get(myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void dance() {
		this.sendFrame(26);
	}

	public void wave() {
		this.sendAction(25);
	}

	public void addCoins(int amount) {
		this.sendPacket("%xt%z%zo%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + amount + "%");
	}

	public void sendMail(int penguinID, int cardID) {
		this.sendPacket("%xt%s%l#ms%" + penguinID + "%" + cardID + "%");
	}

	public void addIgnore(int id) {
		this.sendPacket("%xt%s%n#an%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void removeIgnore(int id) {
		this.sendPacket("%xt%s%n#rn%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void addItem(int id) {
		this.sendPacket("%xt%s%i#ai%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void updatePlayer(String type, int item_id) {
		if (this.players.get(this.myPlayerID).hasItem(item_id) || item_id == 0) {
			type = type.toLowerCase();
			this.sendPacket("%xt%s%s#up" + type + "%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + item_id + "%");
		} else {
			// Implement buying the item if it's not bait and coin requirement met --
			// requires item parser
		}
	}

	public void openNewspaper() {
		this.sendPacket("%xt%s%t#at%" + this.players.get(this.myPlayerID).getIntRoomID() + "%1%1%");
	}

	public void closeNewspaper() {
		this.sendPacket("%xt%s%t#rt%" + this.players.get(this.myPlayerID).getIntRoomID() + "%1%");
	}

	public void getPlayer(int id) {
		this.sendPacket("%xt%s%u#gp%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%");
	}

	public void buyPuffle(int id, String name) {
		this.sendPacket("%xt%s%p#pn%" + this.players.get(this.myPlayerID).getIntRoomID() + "%" + id + "%" + name + "%");
	}
	
	//For 2020
	
	public void followPlayer(String player, boolean state, int x, int y) {
		if (state == false) {
			this.follow = false;
			return;
		}
		
		this.follow = true;
		this.followPlayer = player;
		this.xOffset = x;
		this.yOffset = y;
	}

	public void handlePacket(String packet) {
		int error = 0;

		if (!packet.contains("%"))
			return;
		String[] packetArray = packet.split("%");

		if (packetArray[1].equals("xt")) {
			String handler = packetArray[2];

			// Error handler
			if (handler.equals("e")) {
				error = Integer.parseInt(packetArray[4]);
				System.out.println(this.now() + "Error: " + error);
			}

			// Load Player handler
			else if (handler.equals("lp")) {
				this.players.get(this.myPlayerID).parsePlayerObject(packetArray[4]);
				this.players.get(this.myPlayerID).setCoins(Integer.parseInt(packetArray[5]));
				this.players.get(this.myPlayerID).setIsSafe(Boolean.parseBoolean(packetArray[6]));
				this.players.get(this.myPlayerID).setEggTimerRemaining(Integer.parseInt(packetArray[7]));
				this.players.get(this.myPlayerID).setServerJoinTime(Long.parseLong(packetArray[8]));
				this.players.get(this.myPlayerID).setAge(Integer.parseInt(packetArray[9]));
				this.players.get(this.myPlayerID).setBannedAge(Integer.parseInt(packetArray[10]));
				this.players.get(this.myPlayerID).setMinutesPlayed(Integer.parseInt(packetArray[11]));
				if (this.players.get(this.myPlayerID).getIsMember()) {
					this.players.get(this.myPlayerID).setMembershipDaysRemaining(Integer.parseInt(packetArray[12]));
				} else {
					this.players.get(this.myPlayerID).setMembershipDaysRemaining(-1);
				}
				this.players.get(this.myPlayerID).setTimezoneOffset(0 - Integer.parseInt(packetArray[13]));
			}

			// Join Room handler
			else if (handler.equals("jr")) {
				this.purgePlayers();
				this.players.get(this.myPlayerID).setRoom(Integer.parseInt(packetArray[3]), Integer.parseInt(packetArray[4]));
				int user_id;

				// Update players array with new player information from the room
				for (int i = 5; i <= packetArray.length - 1; i++) {
					user_id = Integer.parseInt(packetArray[i].substring(0, packetArray[i].indexOf("|")));
					if (user_id == this.myPlayerID) {
						this.players.get(this.myPlayerID).parsePlayerObject(packetArray[i]);
						if (this.store_players == false) {
							break;
						}
					} else if (this.store_players == true) {
						Player p = new Player();
						p.parsePlayerObject(packetArray[i]);
						this.players.put(user_id, p);
					}
				}
				this.notification(this.players.get(this.myPlayerID).getNickname() + " joined room: " + this.players.get(this.myPlayerID).getExtRoomID());
			}

			// Join Game handler
			else if (handler.equals("jg")) {
				this.purgePlayers();
				this.players.get(this.myPlayerID).setRoom(Integer.parseInt(packetArray[3]), Integer.parseInt(packetArray[4]));
				this.notification(this.players.get(this.myPlayerID).getNickname() + " joined game: " + this.players.get(this.myPlayerID).getExtRoomID());
			}

			// Add Player to room handler
			else if (handler.equals("ap")) {
				Player p = new Player();
				p.parsePlayerObject(packetArray[4]);
				if (p.getID() != this.myPlayerID && this.store_players == true) {
					this.players.put(p.getID(), p);
					this.notification(players.get(p.getID()).getNickname() + " has joined the room");
				}
			}

			// Remove Player from room handler
			else if (handler.equals("rp")) {
				int user_id = Integer.parseInt(packetArray[4]);
				if (user_id != this.myPlayerID && this.store_players == true) {
					this.notification(players.get(user_id).getNickname() + " has left the room");
					this.players.remove(user_id);
				}
			}

			// Get Items packet
			else if (handler.equals("gi")) {
				int item_id;
				for (int i = 4; i < packetArray.length; i++) {
					item_id = Integer.parseInt(packetArray[i]);
					this.players.get(this.myPlayerID).addItem(item_id);
				}
			}

			// Send Position handler
			else if (handler.equals("sp")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int x = Integer.parseInt(packetArray[5]);
				int y = Integer.parseInt(packetArray[6]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setPosition(x, y);
					this.notification(this.players.get(user_id).getNickname() + " moved to (" + x + ", " + y + ")");
					
					if(follow) {
						if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
							sendPosition(x + xOffset, y + yOffset);
						}
					}
				}
			}

			// Update Color packet
			else if (handler.equals("upc")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setColor(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Head Item packet
			else if (handler.equals("uph")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setHead(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Face Item packet
			else if (handler.equals("upf")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setFace(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Neck Item packet
			else if (handler.equals("upn")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setNeck(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Body Item packet
			else if (handler.equals("upb")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setBody(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Hand Item packet
			else if (handler.equals("upa")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setHand(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Feet Item packet
			else if (handler.equals("upe")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setFeet(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Flag/Pin Item packet
			else if (handler.equals("upl")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setFlag(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Photo/Background Item packet
			else if (handler.equals("upp")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int item_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setPhoto(item_id);
					this.notification(this.players.get(user_id).getNickname() + " is now wearing " + item_id);
				}
			}

			// Update Remove Item packet
			else if (handler.equals("upr")) {
				// ~ CP has this packet in their listings, but I've never seen it used
				// ~ If needed, incorporate with items array by getting item type and setting
				// that type to 0
			}

			// Add Item packet
			else if (handler.equals("ai")) {
				int item_id = Integer.parseInt(packetArray[4]);
				int coins = Integer.parseInt(packetArray[5]);
				this.players.get(this.myPlayerID).addItem(item_id);
				this.players.get(this.myPlayerID).setCoins(coins);
				this.notification(this.players.get(this.myPlayerID).getNickname() + " now has the item " + item_id);
			}

			// Send Emote handler
			else if (handler.equals("se")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int emote_id = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					if (emote_id == 1)
						this.notification(players.get(user_id).getNickname() + " emoted :D");
					else if (emote_id == 2)
						this.notification(players.get(user_id).getNickname() + " emoted :)");
					else if (emote_id == 3)
						this.notification(players.get(user_id).getNickname() + " emoted :|");
					else if (emote_id == 4)
						this.notification(players.get(user_id).getNickname() + " emoted :(");
					else if (emote_id == 5)
						this.notification(players.get(user_id).getNickname() + " emoted :o");
					else if (emote_id == 6)
						this.notification(players.get(user_id).getNickname() + " emoted :P");
					else if (emote_id == 7)
						this.notification(players.get(user_id).getNickname() + " emoted ;)");
					else if (emote_id == 8)
						this.notification(players.get(user_id).getNickname() + " emoted :sick:");
					else if (emote_id == 9)
						this.notification(players.get(user_id).getNickname() + " emoted >:(");
					else if (emote_id == 10)
						this.notification(players.get(user_id).getNickname() + " emoted :'(");
					else if (emote_id == 11)
						this.notification(players.get(user_id).getNickname() + " emoted :\\");
					else if (emote_id == 12)
						this.notification(players.get(user_id).getNickname() + " emoted :lightbulb:");
					else if (emote_id == 13)
						this.notification(players.get(user_id).getNickname() + " emoted :coffee:");
					else if (emote_id == 16)
						this.notification(players.get(user_id).getNickname() + " emoted :flower:");
					else if (emote_id == 17)
						this.notification(players.get(user_id).getNickname() + " emoted :clover:");
					else if (emote_id == 18)
						this.notification(players.get(user_id).getNickname() + " emoted :game:");
					else if (emote_id == 19)
						this.notification(players.get(user_id).getNickname() + " emoted :fart:");
					else if (emote_id == 20)
						this.notification(players.get(user_id).getNickname() + " emoted :coin:");
					else if (emote_id == 21)
						this.notification(players.get(user_id).getNickname() + " emoted :puffle:");
					else if (emote_id == 22)
						this.notification(players.get(user_id).getNickname() + " emoted :sun:");
					else if (emote_id == 23)
						this.notification(players.get(user_id).getNickname() + " emoted :moon:");
					else if (emote_id == 24)
						this.notification(players.get(user_id).getNickname() + " emoted :pizza:");
					else if (emote_id == 25)
						this.notification(players.get(user_id).getNickname() + " emoted :igloo:");
					else if (emote_id == 26)
						this.notification(players.get(user_id).getNickname() + " emoted :pink ice cream:");
					else if (emote_id == 27)
						this.notification(players.get(user_id).getNickname() + " emoted :brown ice cream:");
					else if (emote_id == 28)
						this.notification(players.get(user_id).getNickname() + " emoted :cake:");
					else if (emote_id == 29)
						this.notification(players.get(user_id).getNickname() + " emoted emoted :popcorn:");
					else if (emote_id == 30)
						this.notification(players.get(user_id).getNickname() + " emoted  <3");
					
					if(follow) {
						if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
							sendEmote(emote_id);
						}
					}
				}
			}

			// Snowball handler
			else if (handler.equals("sb")) {
				int user_id = Integer.parseInt(packetArray[4]);
				String x = packetArray[5];
				String y = packetArray[6];
				if (this.players.containsKey(user_id)) {
					this.notification(players.get(user_id).getNickname() + " threw a snowball to (" + x + ", " + y + ")");
					
					if(follow) {
						if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
							snowball(Integer.parseInt(x), Integer.parseInt(y));
						}
					}
				}
			}

			// Send Message handler
			else if (handler.equals("sm")) {
				int user_id = Integer.parseInt(packetArray[4]);
				String message = packetArray[5];
				if (this.players.containsKey(user_id)) {
					this.notification(players.get(user_id).getNickname() + " said " + message);
					
					if(follow) {
						if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
							sendMessage(message);
						}
					}
				}
			}

			// Phrase Chat handler
			/*else if (handler.equals("sc")) {
				int user_id = Integer.parseInt(packetArray[4]);
				String encMsg = packetArray[5];
				// ~ parseChatMessage is buggy. Use at own risk.
				// ~ if(this.players.containsKey(user_id)){
				// ~ notification(players.get(user_id).getNickname() + "said " +
				// parseChatMessage(encMsg));
				// ~ }
			}*/

			// Send Safe Message handler
			else if (handler.equals("ss")) {
				int user_id = Integer.parseInt(packetArray[4]);
				String message_id = packetArray[5];
				if (this.players.containsKey(user_id)) {
					this.notification(players.get(user_id).getNickname() + " safe-messaged " + message_id);
				}
			}

			// Send Joke handler
			else if (handler.equals("sj")) {
				int user_id = Integer.parseInt(packetArray[4]);
				String joke_id = packetArray[5];
				if (this.players.containsKey(user_id))
					this.notification(players.get(user_id).getNickname() + " joked " + joke_id);
				
				if(follow) {
					if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
						sendJoke(Integer.parseInt(joke_id));
					}
				}
			}

			// Send Frame handler
			else if (handler.equals("sf")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int frame = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setFrame(frame);
					if (frame == 17)
						this.notification(players.get(user_id).getNickname() + " is sitting facing S");
					else if (frame == 18)
						this.notification(players.get(user_id).getNickname() + " is sitting facing SW");
					else if (frame == 19)
						this.notification(players.get(user_id).getNickname() + " is sitting facing W");
					else if (frame == 20)
						this.notification(players.get(user_id).getNickname() + " is sitting facing NW");
					else if (frame == 21)
						this.notification(players.get(user_id).getNickname() + " is sitting facing N");
					else if (frame == 22)
						this.notification(players.get(user_id).getNickname() + " is sitting facing NE");
					else if (frame == 23)
						this.notification(players.get(user_id).getNickname() + " is sitting facing E");
					else if (frame == 24)
						this.notification(players.get(user_id).getNickname() + " is sitting facing SE");
					else if (frame == 26)
						this.notification(players.get(user_id).getNickname() + " is dancing");
					else
						this.notification(players.get(user_id).getNickname() + " sent an unknown frame(" + frame + " )");
					
					if(follow) {
						if (players.get(user_id).getNickname().equalsIgnoreCase(followPlayer)) {
							sendFrame(frame);
						}
					}
				}
			}

			// Open Newspaper handler
			else if (handler.equals("at")) {
				int user_id = Integer.parseInt(packetArray[4]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setNewspaper();
				}
				this.notification(players.get(user_id).getNickname() + " is reading the newspaper");
			}

			// Close Newspaper handler
			else if (handler.equals("rt")) {
				int user_id = Integer.parseInt(packetArray[4]);
				if (this.players.containsKey(user_id)) {
					this.players.get(user_id).setNewspaper();
				}
				this.notification(players.get(user_id).getNickname() + " has stopped reading the newspaper");
			}

			// Send Action handler
			else if (handler.equals("sa")) {
				int user_id = Integer.parseInt(packetArray[4]);
				int action = Integer.parseInt(packetArray[5]);
				if (this.players.containsKey(user_id)) {
					if (action == 25)
						this.notification(this.players.get(user_id).getNickname() + " waved");
					else
						this.notification(this.players.get(user_id).getNickname() + " sent unknown action " + action);
				}
			}
		}
	}

	public void disconnect() {
		try {
			this.socket.close();
			this.in.close();
			this.out.close();
			this.is_connected = false;
			System.out.println(this.now() + "You are now disconnected.");
		} catch (IOException e) {
		}
	}
}
