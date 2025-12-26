enum PlaybackState { notStarted, inProgress, completed }

enum DownloadState { notDownloaded, downloading, downloaded, failed }

class Episode {
  final int? id;
  final int podcastId;
  final String title;
  final String? description;
  final String audioUrl;
  final String? imageUrl;
  final Duration? duration;
  final DateTime? publishedAt;
  final String? guid;

  // Playback state
  final Duration position;
  final PlaybackState playbackState;

  // Download state
  final DownloadState downloadState;
  final String? localPath;
  final double downloadProgress;

  Episode({
    this.id,
    required this.podcastId,
    required this.title,
    this.description,
    required this.audioUrl,
    this.imageUrl,
    this.duration,
    this.publishedAt,
    this.guid,
    this.position = Duration.zero,
    this.playbackState = PlaybackState.notStarted,
    this.downloadState = DownloadState.notDownloaded,
    this.localPath,
    this.downloadProgress = 0.0,
  });

  Episode copyWith({
    int? id,
    int? podcastId,
    String? title,
    String? description,
    String? audioUrl,
    String? imageUrl,
    Duration? duration,
    DateTime? publishedAt,
    String? guid,
    Duration? position,
    PlaybackState? playbackState,
    DownloadState? downloadState,
    String? localPath,
    double? downloadProgress,
  }) {
    return Episode(
      id: id ?? this.id,
      podcastId: podcastId ?? this.podcastId,
      title: title ?? this.title,
      description: description ?? this.description,
      audioUrl: audioUrl ?? this.audioUrl,
      imageUrl: imageUrl ?? this.imageUrl,
      duration: duration ?? this.duration,
      publishedAt: publishedAt ?? this.publishedAt,
      guid: guid ?? this.guid,
      position: position ?? this.position,
      playbackState: playbackState ?? this.playbackState,
      downloadState: downloadState ?? this.downloadState,
      localPath: localPath ?? this.localPath,
      downloadProgress: downloadProgress ?? this.downloadProgress,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'podcastId': podcastId,
      'title': title,
      'description': description,
      'audioUrl': audioUrl,
      'imageUrl': imageUrl,
      'duration': duration?.inMilliseconds,
      'publishedAt': publishedAt?.toIso8601String(),
      'guid': guid,
      'position': position.inMilliseconds,
      'playbackState': playbackState.index,
      'downloadState': downloadState.index,
      'localPath': localPath,
      'downloadProgress': downloadProgress,
    };
  }

  factory Episode.fromMap(Map<String, dynamic> map) {
    return Episode(
      id: map['id'] as int?,
      podcastId: map['podcastId'] as int,
      title: map['title'] as String,
      description: map['description'] as String?,
      audioUrl: map['audioUrl'] as String,
      imageUrl: map['imageUrl'] as String?,
      duration: map['duration'] != null
          ? Duration(milliseconds: map['duration'] as int)
          : null,
      publishedAt: map['publishedAt'] != null
          ? DateTime.parse(map['publishedAt'] as String)
          : null,
      guid: map['guid'] as String?,
      position: Duration(milliseconds: (map['position'] as int?) ?? 0),
      playbackState: PlaybackState.values[(map['playbackState'] as int?) ?? 0],
      downloadState: DownloadState.values[(map['downloadState'] as int?) ?? 0],
      localPath: map['localPath'] as String?,
      downloadProgress: (map['downloadProgress'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
