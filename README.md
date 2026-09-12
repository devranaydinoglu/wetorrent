# weTorrent

Torrenting works by sharing pieces of a file directly between users (P2P) instead of downloading it from a single central server.

- [Terminology](#terminology)
- [How torrenting works](#how-torrenting-works)
- [Components](#components)

## Terminology

**Trackers** - central servers that coordinate between users/peers who are downloading and uploading files. They're contacted by a user who wants to find the people who are sharing a file and it's how you tell the swarm that you have pieces to share with others.

**Swarm** - connected group of all users/peers downloading and sharing the same file.

**Seeder** - user/peer that has the full file and continues sharing it with others. Uploads only, no downloading.

**Leecher** - users/peers that are currently downloading pieces and sharing what they already have.

**Infohash** - a 20-byte unique identifier of the torrent. It’s what peers use to find each other, what trackers use to group peers, and what the DHT uses as the lookup key. Changing even one byte of the file contents changes the infohash, making counterfeiting or tampering detectable. It is computed by hashing the b-encoded info dictionary using SHA-1. Although SHA-1 considered cryptographically weak for collision resistance (2^61 operations). So SHA-256 preferred.


## How torrenting works

1. User creates a torrent in the torrent client, specifying the downloadable file on your system. This creates a torrent file (or magnet URI).
2. To participate in torrenting a file, you need to be in possession of a torrent file holding metadata of the file to download.
3. When you "run" the torrent file in the torrent client, it establishes TCP connection between participating peers using the tracker/announce and infohash.
4. BitTorrent handshake: to connect with peers, header is sent to all peers. 68-byte header. | "19" (1B) | "BitTorrent protocol" (19B) | protocol extensions, empty (8B) | info hash (20B) | peer id (20B) |
5. Peer that receives the header validates it (length, infohash, etc.) and returns their infohash.
6. Returned infohash validated on peer. If everything valid, handshake successful. If infohash send by both sides doesn't match, connection is severed.
7. First message that is shared is bitfield (how is bitfield constructed? how does it know which pieces the peer has?).
8. Pieces are selected based on two strategies (rarest-first strategy. very first downloaded pieces use random-first strategy because the peer has nothing to upload yet and so can't contribute to swarm)
9. Use choking algorithm to determine who gets bandwidth.
10. 

When a torrent is created, the source file is split into fixed-size pieces (256KB - 4MB).
For a 5 GB file with 1 MB pieces, there are 5,120 pieces. Each piece has a 20-byte SHA-1 hash stored in the .torrent file’s info dictionary. The total hash data for this torrent would be:
5,120 pieces × 20 bytes = 102,400 bytes ≈ 100 KB of hash data.

Each peer maintains a bitfield, a binary array where bit N represents whether the peer has piece N. A 5,120-piece torrent has a 5,120-bit (640-byte) bitfield. When two peers connect, they exchange their bitfields so each knows what the other has.


## Components

### Torrent file

Torrent file contains all the metadata needed to torrent:
- announce (tracker url or DHT)
- info dictionary:
	- **'name'** key maps to a UTF-8 encoded string which is the suggested name to save the file (or directory) as. In the single file case, it is the name of a file, in the muliple file case, it's the name of a directory.
	- **'piece length'** maps to the size of a piece in bytes. Most commonly 256K.
	- **'pieces'** maps to a string whose length is a multiple of 20. It is to be subdivided into strings of length 20, each of which is the SHA1 hash of the piece at the corresponding index.
	- There is also a key **'length'** or a key **'files'**, but not both or neither. If length is present then the download represents a single file, otherwise it represents a set of files which go in a directory structure. In the single file case, length maps to the length of the file in bytes. For the purposes of the other keys, the multi-file case is treated as only having a single file by concatenating the files in the order they appear in the files list. The files list is the value files maps to, and is a list of dictionaries containing the following keys: length - The length of the file, in bytes. path - A list of UTF-8 encoded strings corresponding to subdirectory names, the last of which is the actual file name (a zero length list is an error case).
(file name, lengths, piece length, concatenated SHA-1 hashes of all pieces of the file, creation date)

### Trackers

Trackers are central servers that coordinate between users/peers who are downloading and uploading files. They're contacted by a user who wants to find the people who are sharing a file and it's how you tell the swarm that you have pieces to share with others.

Tracker `GET` requests have the following keys:
- **info_hash**: 20-byte sha1 hash of the bencoded form of the info dictionary from the torrent file.
- **peer_id**: string of length 20. Each peer generates its own id at random at the start of a new download.
- **ip**: optional parameter giving the IP (or dns name) which this peer is at. Generally used for the origin if it's on the same machine as the tracker.
- **port**: port number this peer is listening on. Common behavior is for a downloader to try to listen on port 6881 and if that port is taken try 6882, then 6883, etc. and give up after 6889.
- **uploaded**: total amount uploaded so far, encoded in base ten ascii.
- **downloaded**: total amount downloaded so far, encoded in base ten ascii.
- **left**: number of bytes this peer still has to download, encoded in base ten ascii.
- **event**: optional key which maps to started, completed, or stopped (or empty, which is the same as not being present). If not present, this is one of the announcements done at regular intervals. An announcement using started is sent when a download first begins, and one using completed is sent when the download is complete. No completed is sent if the file was complete when started. Downloaders send an announcement using stopped when they cease downloading.

Tracker responses are bencoded dictionaries. If a tracker response has a key `failure reason`, then that maps to a human readable string which explains why the query failed, and no other keys are required. Otherwise, it must have two keys: `interval`, which maps to the number of seconds the downloader should wait between regular rerequests, and `peers`. Peers maps to a list of dictionaries corresponding to peers, each of which contains the keys peer id, ip, and port, which map to the peer's self-selected ID, IP address or dns name as a string, and port number, respectively.
More commonly is that trackers return a compact representation of the peer list. To reduce the size of tracker responses and to reduce memory and computational requirements in trackers, trackers may return peers as a packed string rather than as a bencoded list. It is suggested now to use a compact format where each peer is represented using only 6 bytes. The first 4 bytes contain the 32-bit ipv4 address. The remaining two bytes contain the port number. Both address and port use network-byte order.

Multi-tracker torrent can be used for redundancy.

### Peer protocol

BitTorrent's peer protocol operates over TCP or uTP. Peer connections are symmetrical. Messages sent in both directions look the same, and data can flow in either direction.

The peer protocol refers to pieces of the file by index as described in the metainfo file, starting at zero.

Connections start out choked and not interested.

When data is being transferred, downloaders should keep several piece requests queued up at once in order to get good TCP performance (this is called 'pipelining'.) On the other side, requests which can't be written out to the TCP buffer immediately should be queued up in memory rather than kept in an application-level network buffer, so they can all be thrown out when a choke happens.

Messages sent/received consist of | length (4B) | message type (1B) | payload (variable length) |
Message types:
- 0 - choke - stop sending requests to this peer
- 1 - unchoke - resume accepting requests
- 2 - interested - signal willigness to download
- 3 - not interested - no needed pieces from this peer
- 4 - have - announce completion of piece
- 5 - bitfield - send bitfield
- 6 - request - request a block (piece index, offset, length)
- 7 - piece - deliver a block (piece index, offset, block data)
- 8 - cancel - cancel a previously sent request

Before requesting/sending pieces, they're further divided into blocks.

### Choking algorithm

To prevent peers from abusing the protocol, downloading files without contributing anything to the swarm, the choke algorithm is used.
Suppose peer B is the one abusing the protocol by downloading from peer A without uploading anything. In this case, peer A chokes peer B, blocking peer B from downloading anything from peer A but peer A still has the power to download from peer B. Doing this, the network traffic relaxes, and it makes the game more fair for every participant of the network.
