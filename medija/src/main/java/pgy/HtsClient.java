package pgy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.tvheadend.tvhclient.htsp.HTSConnection;
import org.tvheadend.tvhclient.htsp.HTSConnectionListener;
import org.tvheadend.tvhclient.htsp.HTSMessage;
import org.tvheadend.tvhclient.htsp.HTSResponseHandler;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.ChannelTag;

public class HtsClient {
	private List<Channel> channels = new ArrayList<>();
	private HTSConnection connection;
	private Executor execService;
	private boolean initialSyncCompleted;
	private Map<Object, ChannelTag> tags = new HashMap<>();

	public interface InitialSyncCompletedListener {
		public void execute();
	}

	public interface OnGetTicketListener {
		public void execute(String url);
	}

	public HTSConnection open(String hostname, int port, String username, String password, InitialSyncCompletedListener initialSyncCompletedListener) {
		connection = new HTSConnection(new HTSConnectionListener() {

			@Override
			public void onMessage(HTSMessage response) {
				String method = response.getMethod();
				System.out.println("onMessage " + method);
				if (method.equals("channelAdd")) {
					onChannelAdd(response);
				} else if (method.equals("tagAdd")) {
					onTagAdd(response);
				} else if (method.equals("tagUpdate")) {
					onTagUpdate(response);
				} else if (method.equals("initialSyncCompleted")) {
					onInitialSyncCompleted(initialSyncCompletedListener);
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

	private void onTagAdd(HTSMessage msg) {
		ChannelTag tag = new ChannelTag();
		tag.id = msg.getLong("tagId");
		tag.name = msg.getString("tagName", null);
		tag.icon = msg.getString("tagIcon", null);
		tags.put(tag.id, tag);
	}

	private void onTagUpdate(HTSMessage msg) {
		ChannelTag tag = tags.get(msg.getLong("tagId"));
		if (tag == null) {
			return;
		}

		tag.name = msg.getString("tagName", tag.name);
		String icon = msg.getString("tagIcon", tag.icon);
		if (icon == null) {
			tag.icon = null;
			tag.iconBitmap = null;
		} else if (!icon.equals(tag.icon)) {
			tag.icon = icon;
		}
	}

	public void getTicket(Channel ch, OnGetTicketListener onGetTicketListener) {
		HTSMessage request = new HTSMessage();
		request.setMethod("getTicket");
		request.putField("channelId", ch.id);
		connection.sendMessage(request, new HTSResponseHandler() {

			public void handleResponse(HTSMessage response) {
				String path = response.getString("path", null);
				String ticket = response.getString("ticket", null);
				String webroot = connection.getWebRoot();
				String url = webroot + path + "?ticket=" + ticket;
				if (onGetTicketListener != null) {
					onGetTicketListener.execute(url);
				}
			}
		});
	}

	private void onInitialSyncCompleted(InitialSyncCompletedListener initialSyncCompletedListener) {
		initialSyncCompleted = true;
		if (initialSyncCompletedListener != null) {
			initialSyncCompletedListener.execute();
		}
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