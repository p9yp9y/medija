import java.io.IOException;

import pgy.HtsClient;

public class Test {

	@org.junit.Test
	public void testPrintChannels() throws IOException, InterruptedException {
		HtsClient htsClient = new HtsClient();
		htsClient.open("pgy.no-ip.hu", 9982, "andris", "pender123", () -> printChannels(htsClient));
		waitForFinish(htsClient);
	}

	private void waitForFinish(HtsClient htsClient) throws InterruptedException {
		while (!htsClient.isInitialSyncCompleted()) {
			Thread.sleep(100);
		}
	}

	private void printChannels(HtsClient htsClient) {
		htsClient.getChannels().forEach(ch -> System.out.println(ch));
	}
}