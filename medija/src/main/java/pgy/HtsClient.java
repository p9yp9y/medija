package pgy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.tvheadend.tvhclient.htsp.HTSConnection;
import org.tvheadend.tvhclient.htsp.HTSConnectionListener;
import org.tvheadend.tvhclient.htsp.HTSMessage;
import org.tvheadend.tvhclient.model.Channel;

public class HtsClient {
	private List<Channel> channels = new ArrayList<>();
	private HTSConnection connection;
	private Executor execService;
	private boolean initialSyncCompleted;

	public interface InitialSyncCompletedListener {
		public void execute();
	}

	public HTSConnection open(String hostname, int port, String username, String password,
			InitialSyncCompletedListener initialSyncCompletedListener) {
		connection = new HTSConnection(new HTSConnectionListener() {

			@Override
			public void onMessage(HTSMessage response) {
				String method = response.getMethod();
				System.out.println("onMessage " + method);
				if (method.equals("channelAdd")) {
					onChannelAdd(response);
				}
				if (method.equals("initialSyncCompleted")) {
					initialSyncCompleted = true;
					if (initialSyncCompletedListener != null) {
						initialSyncCompletedListener.execute();
					}
				}
			}

			@Override
			public void onError(int errorCode) {
				System.out.println("onError");
			}

			@Override
			public void onError(Exception ex) {
				System.out.println("onError");
			}
		}, "Andris HTS", "1.0.0");

		if (execService == null) {
			execService = Executors.newScheduledThreadPool(5);
		}

		execService.execute(() -> {
			connection.open(hostname, port);
			connection.authenticate(username, password);
		});

		return connection;
	}

	private void onChannelAdd(HTSMessage msg) {
		final Channel ch = new Channel();
		ch.id = msg.getLong("channelId");
		ch.name = msg.getString("channelName", null);
		ch.number = msg.getInt("channelNumber", 0);
		ch.icon = msg.getString("channelIcon", null);
		ch.tags = msg.getIntList("tags", ch.tags);

		if (ch.number == 0) {
			ch.number = (int) (ch.id + 25000);
		}
		channels.add(ch);
	}

	public void close() {
		connection.close();
	}

	public boolean isConnected() {
		return connection.isConnected();
	}

	public HTSConnection getConnection() {
		return connection;
	}

	public List<Channel> getChannels() {
		return channels;
	}

	public boolean isInitialSyncCompleted() {
		return initialSyncCompleted;
	}

}