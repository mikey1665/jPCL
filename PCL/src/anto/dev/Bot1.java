package anto.dev;

import java.util.Scanner;

public class Bot1 {
		
	public static int serverRoom = 6119;

	public static void main(String args[]) throws InterruptedException {
		System.out.println("Initializing....");
		System.out.println("Welcome to the Club Pengiun Bot Script!");
		System.out.println("This has been updated in 2020 by mike1665");
		System.out.println("----------------------------------------------------------");
		
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		System.out.println("Username:");
		String username = in.nextLine();
		System.out.println("Password:");
		String password = in.nextLine();
		
		System.out.println("Server:");
		String room = in.nextLine();
		switch(room) {
		case "blizzard":
			serverRoom = 6113;
			break;
		case "snow avalanche":
			serverRoom = 6114;
			break;
		case "sleet":
			serverRoom = 6119;
			break;
		case "marshmallow":
			serverRoom = 7000;
			break;
		case "zipline":
			serverRoom = 7001;
			break;
		case "deep freeze":
			serverRoom = 7008;
			break;
		case "antarctic":
			serverRoom = 7003;
			break;
		case "abominable":
			serverRoom = 7004;
			break;
		case "ascent":
			serverRoom = 7008;
			break;
		case "beanie":
			serverRoom = 7011;
			break;
		}
		
		System.out.println("Follow:");
		String follow = in.nextLine();
	
		Pickle p = new Pickle();
		p.connect(username, password, serverRoom);
		
		p.joinRoom(805);
		p.followPlayer(follow, true, 0, -50);
		
		while (true) {
			
		}

	}
}
