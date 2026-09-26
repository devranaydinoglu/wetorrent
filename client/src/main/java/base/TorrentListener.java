package base;

import torrent.TorrentHandle;

public interface TorrentListener {

    void onTorrentAdded(TorrentHandle handle);

    void onTorrentRemoved(TorrentHandle handle);

}
