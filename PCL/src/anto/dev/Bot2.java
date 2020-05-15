package anto.dev;

public class Bot2 {

	public static void main(String args[]) throws InterruptedException {

		// Follow our player
		String follow = "cheeseguy01";

		// Our accounts

		// , "ninjabro03", "ninjabro04", "ninjabro05", "ninjabro06", "ninjabro07", "ninjabro08", "ninjabro09"
		
		String botsUsername[] = { };
		String botsPasswords[] = { };

		// Circle
		int xOffets[] = { -0, 0, 60, -60, -40, 40, -40, 40 };
		int yOffets[] = { -60, 60, 0, -0, 40, 40, -40, -40 };

		System.out.println("Initializing....");

		// Create an instance of Pickle
		Pickle[] p = new Pickle[botsUsername.length];
		for (int i = 0; i < p.length; i++) {
			p[i] = new Pickle();
			p[i].connect(botsUsername[i], botsPasswords[i], 6119);
			p[i].joinRoom(100);

			p[i].followPlayer(follow, true, xOffets[i], yOffets[i]);
			System.out.println(botsUsername[i] + " sent offsets: " + xOffets[i] + ", " + yOffets[i]);
		}

		/*while (true) {
			for (int i = 0; i < p.length; i++) {
				Random x = new Random();
				Random y = new Random();

				p[i].sendPosition(x.nextInt(900), y.nextInt(900));

				p[i].sendEmote(19);

				Thread.sleep(500);
			}
		}*/
	}
}
