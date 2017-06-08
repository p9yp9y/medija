import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import pgy.HtsClient;

public class Test {

	private HtsClient htsClient;

	@org.junit.Test
	public void testPrintChannels() throws IOException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		
		htsClient = new HtsClient();
		htsClient.open("pgy.no-ip.hu", 80, "andris", "pender123", () -> {
			printChannels(htsClient);
			latch.countDown();
		});
		
		latch.await();
	}

	@org.junit.Test
	public void testGetTicket() throws IOException, InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		
		testPrintChannels();
		htsClient.getTicket(htsClient.getChannels().get(0), (url) -> {
			System.out.println("!!!" + url);
			latch.countDown();
		});
		
		latch.await();
	}

	private void printChannels(HtsClient htsClient) {
		htsClient.getChannels().forEach(ch -> System.out.println(ch));
	}
}
